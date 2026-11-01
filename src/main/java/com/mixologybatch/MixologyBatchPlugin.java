package com.mixologybatch;

import com.google.inject.Provides;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Mixology AIO",
	description = "Plans your 28-potion batches for Mastering Mixology, with reward tracking and station timing cues.",
	tags = {"mastering", "mixology", "herblore", "potions", "batch", "rewards", "skilling", "varlamore"}
)
public class MixologyBatchPlugin extends Plugin
{
	private static final int LAB_REGION = 5521;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MixologyBatchConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BatchPanelOverlay panelOverlay;

	@Inject
	private BatchSceneOverlay sceneOverlay;

	@Inject
	private BatchInventoryOverlay inventoryOverlay;

	@Inject
	private BatchQueueOverlay queueOverlay;

	@Inject private net.runelite.client.ui.ClientToolbar clientToolbar;
    @Inject private ConfigManager configManager;
    private MixologyPanel sidebar;
    private net.runelite.client.ui.NavigationButton navigation;
    private java.util.concurrent.ExecutorService plannerWorker;
    private java.util.concurrent.Future<FullInventoryOptimizer.Result> pendingPlan;
    private FullInventoryOptimizer.Result estimate;
    private final FullInventoryOptimizer optimizer = new FullInventoryOptimizer();
    private boolean refillPrepared;
    private String pendingKey = "";
    private String planningStatus = "Calculating the next full inventory…";
    private String appliedSettings = "";
    private OptimizationGoal activeGoal = OptimizationGoal.BALANCED;
    private int observedLevel;
    private int[] resinBalances = new int[3], hopperPaste = new int[3];
    private long[] targetCosts = new long[3];

    @Inject private StationTimingOverlay timingOverlay;
    private final StationTiming stationTiming = new StationTiming();
    private final AlembicCountdown alembicCountdown = new AlembicCountdown();
    private static final int TIMING_CHIME = 2655; // found_gem, also used by the game.

    private final BatchStateResolver resolver = new BatchStateResolver();
	private BatchPlan plan = BatchPlan.defaultPlan();
	private CyclePlan cyclePlan;
	private Guidance guidance = Guidance.outside();
	private EnumMap<Potion, Integer> currentPotionCounts = new EnumMap<>(Potion.class);
	private final Deque<Potion> previousQueuePotions = new ArrayDeque<>();
	private List<Potion> upcomingQueuePotions = Collections.emptyList();
	private List<Potion> plannedPotionQueue = Collections.emptyList();
	private Potion trackedPotion;
	private int trackedStep = 1;
	private MixPrediction mixPrediction;
	private Station partialProcessingStation;
	private int gameTickCounter;
	private BatchMode batchMode = BatchMode.REFILLING;
	private boolean inLab;

	@Override
	protected void startUp()
	{
		plannerWorker = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "mixology-inventory-planner"); thread.setDaemon(true); return thread;
        });
        sidebar = new MixologyPanel(configManager, config);
        java.awt.image.BufferedImage icon = net.runelite.client.util.ImageUtil.resizeImage(
            net.runelite.client.util.ImageUtil.loadImageResource(MixologyBatchPlugin.class, "/mixology-aio.png"), 16, 16);
        navigation = net.runelite.client.ui.NavigationButton.builder().tooltip("Mixology AIO").icon(icon).priority(6).panel(sidebar).build();
        clientToolbar.addNavigation(navigation);
        overlayManager.add(panelOverlay);
		overlayManager.add(sceneOverlay);
		overlayManager.add(inventoryOverlay);
		overlayManager.add(queueOverlay);
        overlayManager.add(timingOverlay);
		rebuildPlan();
		clientThread.invokeLater(this::updateState);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navigation);
        plannerWorker.shutdownNow();
        sidebar = null;
        overlayManager.remove(panelOverlay);
		overlayManager.remove(sceneOverlay);
		overlayManager.remove(inventoryOverlay);
		overlayManager.remove(queueOverlay);
        overlayManager.remove(timingOverlay);
		deactivateLab();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
        gameTickCounter++;
        alembicCountdown.tick(StationTiming.now());
        updateState();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (inLab && event.getContainerId() == InventoryID.INV)
		{
			clientThread.invokeLater(this::updateState);
		}
	}

    @Subscribe
    public void onVarbitChanged(net.runelite.api.events.VarbitChanged event) {
        if (!inLab) { return; }
        int id = event.getVarbitId();
        for (Station station : Station.values()) {
            if (id == station.getPotionVarbit()) { stationTiming.setPotion(station, event.getValue()); }
        }
        if (id == Station.CRYSTALLISE.getPotionVarbit()) {
            if (event.getValue() == 0) { alembicCountdown.stop(); }
            else {
                alembicCountdown.newPotion();
                if (isPumpingAlembic()) { alembicCountdown.start(gameTickCounter, StationTiming.now()); }
            }
        }
        if (id == VarbitID.MM_ALEMBIC_PROGRESS) {
            stationTiming.progress(Station.CRYSTALLISE, event.getValue()); alembicCountdown.progress(event.getValue());
        }
        if (id == VarbitID.MM_AGITATOR_PROGRESS) { stationTiming.progress(Station.HOMOGENISE, event.getValue()); }
        if (id == VarbitID.MM_LAB_HIT_SKILLSHOT_ALEMBIC && event.getValue() > 0) { stationTiming.success(Station.CRYSTALLISE); alembicCountdown.stop(); }
        if (id == VarbitID.MM_LAB_HIT_SKILLSHOT_AGITATOR && event.getValue() > 0) { stationTiming.success(Station.HOMOGENISE); }
        if (id == VarbitID.MM_LAB_MIXER_SLOT_0 || id == VarbitID.MM_LAB_MIXER_SLOT_1
            || id == VarbitID.MM_LAB_MIXER_SLOT_2 || id == VarbitID.MM_LAB_VESSEL_READY) {
            clientThread.invokeLater(this::updateState);
        }
    }

    @Subscribe
    public void onGraphicsObjectCreated(net.runelite.api.events.GraphicsObjectCreated event) {
        if (!inLab || !config.stationTimingCues()) { return; }
        int effect = event.getGraphicsObject().getId();
        Station station = StationTiming.stationForEffect(effect);
        if (station == null) { return; }
        stationTiming.setPotion(station, client.getVarbitValue(station.getPotionVarbit()));
        if (stationTiming.effect(effect, StationTiming.now())) {
            if (station == Station.CRYSTALLISE) { alembicCountdown.effect(gameTickCounter); }
            if (config.timingSound()) { client.playSoundEffect(TIMING_CHIME); }
        }
    }

    @Subscribe
    public void onSoundEffectPlayed(net.runelite.api.events.SoundEffectPlayed event) {
        if (inLab && config.stationTimingCues() && config.timingSound() && event.getSoundId() == TIMING_CHIME
            && event.getDelay() > 0 && client.getVarbitValue(Station.CRYSTALLISE.getPotionVarbit()) != 0) {
            event.consume();
        }
    }

    @Subscribe
    public void onAnimationChanged(net.runelite.api.events.AnimationChanged event) {
        if (!inLab || event.getActor() != client.getLocalPlayer()) { return; }
        if (isPumpingAlembic() && client.getVarbitValue(Station.CRYSTALLISE.getPotionVarbit()) != 0) {
            alembicCountdown.start(gameTickCounter, StationTiming.now());
        } else { alembicCountdown.stop(); }
    }
    private boolean isPumpingAlembic() {
        Player player = client.getLocalPlayer();
        return player != null && player.getAnimation() == net.runelite.api.gameval.AnimationID.HUMAN_MACHINERY_ALCHEMY01_ALEMBIC01_INTERACT01;
    }
    double getAlembicCountdown(long now) { return alembicCountdown.fraction(gameTickCounter, now); }

    StationTiming getStationTiming() { return stationTiming; }
    Station getTimingStation() {
        for (Station station : Station.values()) {
            if (stationTiming.isUrgent(station, StationTiming.now())) { return station; }
        }
        for (Station station : Station.values()) {
            if (stationTiming.message(station, StationTiming.now()) != null) { return station; }
        }
        return null;
    }

	@Subscribe
	public void onPostMenuSort(PostMenuSort event)
	{
		if (!inLab || !config.guardWrongStations() || client.isMenuOpen())
		{
			return;
		}

		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();
		StationMenuGuard.MenuScan scan = new StationMenuGuard.MenuScan();

		for (int i = 0; i < entries.length; i++)
		{
			MenuEntry entry = entries[i];
			scan.accept(
				i,
				entry.getIdentifier(),
				entry.getParam0(),
				entry.getParam1(),
				entry.getType(),
				entry.getOption());
		}

		StationMenuGuard.MenuSwap swap = scan.select();
		if (swap == null || canUseStation(swap.getStation()))
		{
			return;
		}

		MenuEntry processingEntry = entries[swap.getProcessingIndex()];
		entries[swap.getProcessingIndex()] = entries[swap.getCheckIndex()];
		entries[swap.getCheckIndex()] = processingEntry;
		menu.setMenuEntries(entries);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!inLab || "Examine".equalsIgnoreCase(event.getMenuOption()))
		{
			return;
		}

		Station clickedStation = StationMenuGuard.stationForObjectId(event.getId());
		if (batchMode.isRefilling()
			&& clickedStation != null
			&& clickedStation == partialProcessingStation
			&& event.getMenuAction() == MenuAction.GAME_OBJECT_FIRST_OPTION
			&& canUseStation(clickedStation))
		{
			// Treat choosing the available processing station before the inventory
			// is full as an explicit request to finish and deposit this partial batch.
			batchMode = BatchMode.FINISHING_PARTIAL;
			resetActionQueue();
			updateState();
			return;
		}

		Component lever = componentForLeverObject(event.getId());
		if (lever != null)
		{
			handleLeverClick(lever);
			return;
		}
		if (event.getId() == LabObject.MIXING_VESSEL.getObjectId()
			&& "Mix".equalsIgnoreCase(event.getMenuOption()))
		{
			handleMixClick();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGIN_SCREEN:
			case HOPPING:
				deactivateLab();
				break;
			default:
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.MM_OVERLAY)
		{
			clientThread.invokeLater(this::updateState);
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.MM_OVERLAY)
		{
			deactivateLab();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!MixologyBatchConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
        // Settings and targets are queued for the next refill; never replace an active recipe queue.
        clientThread.invokeLater(this::updateState);
    }

    private void rebuildPlan()
	{
		EnumMap<Potion, Integer> counts = new EnumMap<>(Potion.class);
		counts.put(Potion.MMM, config.mmmCount());
		counts.put(Potion.MMA, config.mmaCount());
		counts.put(Potion.MML, config.mmlCount());
		counts.put(Potion.AAA, config.aaaCount());
		counts.put(Potion.AAM, config.aamCount());
		counts.put(Potion.ALA, config.alaCount());
		counts.put(Potion.LLL, config.lllCount());
		counts.put(Potion.MLL, config.mllCount());
		counts.put(Potion.ALL, config.allCount());
		counts.put(Potion.MAL, config.malCount());
		plan = BatchPlan.create(counts, config.stationOrder());
		cyclePlan = null;
		resetActionQueue();
	}

	private void updateState()
	{
		boolean wasInLab = inLab;
		inLab = isPlayerInMixologyRoom();
		if (!inLab)
		{
			deactivateLab();
			return;
		}

        List<InventorySlot> inventory = readInventory();
		Potion vesselPotion = Potion.fromVarbit(client.getVarbitValue(VarbitID.MM_LAB_VESSEL_READY));
		int[] mixerSlots = {
			client.getVarbitValue(VarbitID.MM_LAB_MIXER_SLOT_0),
			client.getVarbitValue(VarbitID.MM_LAB_MIXER_SLOT_1),
			client.getVarbitValue(VarbitID.MM_LAB_MIXER_SLOT_2)
		};
		Map<Station, Potion> activeStations = new EnumMap<>(Station.class);
		for (Station station : Station.values())
		{
			Potion potion = Potion.fromVarbit(client.getVarbitValue(station.getPotionVarbit()));
			if (potion != null)
			{
				activeStations.put(station, potion);
			}
		}
        for (Station station : Station.values()) {
            stationTiming.setPotion(station, client.getVarbitValue(station.getPotionVarbit()));
        }
        readPlanningResources();
        if (refillPrepared && plan.size() == 0 && !planningKey(inventory).equals(pendingKey)) { refillPrepared = false; }
        boolean mixingNow = vesselPotion != null || BatchStateResolver.hasMixerContents(mixerSlots);
        boolean boundary = !refillPrepared && activeStations.isEmpty()
            && (cyclePlan == null || (!mixingNow && (countInventoryPotions(inventory) == 0
                || (batchMode.allowsRollingRefill() && BatchStateResolver.shouldStartRollingRefill(inventory)))));
        if (boundary && !prepareRefill(inventory)) {
            guidance = Guidance.invalid(planningStatus);
            publishSidebar(); return;
        }
        reconcileMixPrediction(inventory);

		List<InventorySlot> planningInventory = inventory;
		Potion planningVesselPotion = vesselPotion;
		int[] planningMixerSlots = mixerSlots;
		if (mixPrediction != null
			&& inventory.get(mixPrediction.getInventorySlot()).isEmpty())
		{
			planningInventory = new ArrayList<>(inventory);
			planningInventory.set(
				mixPrediction.getInventorySlot(),
				InventorySlot.fromItemId(mixPrediction.getPotion().getUnfinishedItemId()));
			planningVesselPotion = null;
			planningMixerSlots = new int[]{0, 0, 0};
		}

		Guidance previousGuidance = guidance;
		int inventoryPotionCount = countInventoryPotions(planningInventory);
		boolean mixingActivity = vesselPotion != null || BatchStateResolver.hasMixerContents(mixerSlots);
		if (!wasInLab || cyclePlan == null || !cyclePlan.belongsTo(plan))
		{
			cyclePlan = CyclePlan.create(plan, planningInventory);
			batchMode = cyclePlan.isValid()
				? BatchMode.initialize(
					inventoryPotionCount,
					activeStations.size(),
					cyclePlan.getPotionCapacity(),
					mixingActivity,
					BatchStateResolver.firstUnfinishedEntry(cyclePlan, inventory) != null)
				: BatchMode.PROCESSING;
		}

		if (batchMode == BatchMode.PROCESSING && mixingActivity && activeStations.isEmpty())
		{
			cyclePlan = CyclePlan.create(plan, planningInventory);
			batchMode = BatchMode.REFILLING;
		}
		else if (batchMode.isRefilling()
			&& cyclePlan != null
			&& !cyclePlan.containsPotionSlots(planningInventory))
		{
			cyclePlan = CyclePlan.create(plan, planningInventory);
		}
		if (cyclePlan != null && cyclePlan.isValid())
		{
			cyclePlan.observeInventory(planningInventory);
		}

		if (batchMode.isRefilling()
			&& cyclePlan != null
			&& cyclePlan.isValid()
			&& cyclePlan.getPotionCapacity() > 0
			&& inventoryPotionCount >= cyclePlan.getPotionCapacity()
			&& !mixingActivity)
		{
			batchMode = BatchMode.PROCESSING;
		}
		else if (batchMode.allowsRollingRefill()
			&& activeStations.isEmpty()
			&& !mixingActivity
			&& BatchStateResolver.shouldStartRollingRefill(planningInventory))
		{
			cyclePlan = CyclePlan.create(plan, planningInventory);
			batchMode = BatchMode.REFILLING;
			resetActionQueue();
		}
		else if (!batchMode.isRefilling()
			&& inventoryPotionCount == 0
			&& activeStations.isEmpty())
		{
			cyclePlan = CyclePlan.create(plan, planningInventory);
			batchMode = BatchMode.REFILLING;
		}

		BatchEntry partialEntry = activeStations.isEmpty()
			? BatchStateResolver.firstUnfinishedEntry(cyclePlan, inventory)
			: null;
		partialProcessingStation = partialEntry == null ? null : partialEntry.getStation();

		currentPotionCounts = BatchStateResolver.potionCounts(planningInventory);
		if (planningVesselPotion != null)
		{
			currentPotionCounts.merge(planningVesselPotion, 1, Integer::sum);
		}
		for (Potion activePotion : activeStations.values())
		{
			currentPotionCounts.merge(activePotion, 1, Integer::sum);
		}
		plannedPotionQueue = buildPlannedPotionQueue(planningInventory, 4);

		guidance = resolver.resolve(
			plan,
			cyclePlan,
			batchMode.isRefilling(),
			planningInventory,
			planningVesselPotion,
			planningMixerSlots,
			activeStations,
			previousGuidance);
        if (!batchMode.isRefilling()) { refillPrepared = false; }
        // Only recover mid-batch when the next recommended potion has become impossible.
        if (activeGoal != OptimizationGoal.MANUAL && guidance.getPhase() == Guidance.Phase.MIXING && guidance.getEntry() != null) {
            Potion next = guidance.getEntry().getPotion();
            if (next.level() > observedLevel || next.paste(0) > hopperPaste[0]
                || next.paste(1) > hopperPaste[1] || next.paste(2) > hopperPaste[2]) {
                refillPrepared = false; cyclePlan = null;
                guidance = Guidance.invalid("Level or paste changed. Recovering the remaining inventory…");
            }
        }
        if (activeGoal != OptimizationGoal.MANUAL && plan.size() == 0) {
            guidance = Guidance.invalid(observedLevel < 60 ? "60 Herblore is required." : "Load paste into the hopper to prepare an inventory.");
        }
        if (activeGoal != OptimizationGoal.MANUAL && countInventoryPotions(inventory) > plan.size()) {
            refillPrepared = false; cyclePlan = null;
        }
        synchronizeActionQueue();
        publishSidebar();
    }

    private String settingsKey() {
        return config.goal() + ":" + config.rewardTargets() + ":" + config.stationOrder() + ":" + config.processingSeconds()
            + ":" + config.mmmCount() + ":" + config.mmaCount() + ":" + config.mmlCount() + ":" + config.aaaCount()
            + ":" + config.aamCount() + ":" + config.alaCount() + ":" + config.lllCount() + ":" + config.mllCount()
            + ":" + config.allCount() + ":" + config.malCount();
    }
    private void readPlanningResources() {
        observedLevel = client.getBoostedSkillLevel(net.runelite.api.Skill.HERBLORE);
        resinBalances = new int[]{client.getVarpValue(net.runelite.api.gameval.VarPlayerID.MIXOLOGY_MOX_POINTS),
            client.getVarpValue(net.runelite.api.gameval.VarPlayerID.MIXOLOGY_AGA_POINTS), client.getVarpValue(net.runelite.api.gameval.VarPlayerID.MIXOLOGY_LYE_POINTS)};
        hopperPaste = new int[]{client.getVarbitValue(VarbitID.MM_AVAILABLE_MOX), client.getVarbitValue(VarbitID.MM_AVAILABLE_AGA), client.getVarbitValue(VarbitID.MM_AVAILABLE_LYE)};
        targetCosts = RewardTargets.costs(RewardTargets.parse(config.rewardTargets()));
    }
    private String planningKey(List<InventorySlot> inventory) {
        StringBuilder items = new StringBuilder();
        for (InventorySlot slot : inventory) { items.append(slot.getItemId()).append(','); }
        return settingsKey() + observedLevel + java.util.Arrays.toString(resinBalances)
            + java.util.Arrays.toString(hopperPaste) + items;
    }
    private boolean prepareRefill(List<InventorySlot> inventory) {
        if (config.goal() == OptimizationGoal.MANUAL) {
            rebuildPlan(); activeGoal = config.goal(); appliedSettings = settingsKey(); refillPrepared = true;
            if (pendingPlan != null) { pendingPlan.cancel(true); pendingPlan = null; }
            estimate = null; pendingKey = planningKey(inventory); return true;
        }
        if (config.goal() == OptimizationGoal.REWARDS && java.util.Arrays.stream(RewardTargets.deficits(targetCosts, resinBalances)).sum() == 0) {
            planningStatus = java.util.Arrays.stream(targetCosts).sum() == 0 ? "Select rewards in the sidebar." : "Reward target funded. Visit the reward shop.";
            return false;
        }
        planningStatus = "Calculating the next full inventory…";
        int capacity = 0;
        for (InventorySlot slot : inventory) { if (slot.isEmpty() || slot.isPotion()) { capacity++; } }
        String key = planningKey(inventory);
        if (pendingPlan == null && estimate != null && key.equals(pendingKey)) {
            refillPrepared = true; return true;
        }
        if (pendingPlan == null || !key.equals(pendingKey)) {
            if (pendingPlan != null) { pendingPlan.cancel(true); }
            FullInventoryOptimizer.Input input = new FullInventoryOptimizer.Input(observedLevel, capacity, hopperPaste,
                RewardTargets.deficits(targetCosts, resinBalances), config.goal(), config.stationOrder(), config.processingSeconds(), inventory);
            pendingPlan = plannerWorker.submit(() -> optimizer.optimize(input)); pendingKey = key;
            return false;
        }
        if (!pendingPlan.isDone()) { return false; }
        try {
            estimate = pendingPlan.get(); pendingPlan = null;
            plan = estimate.plan; cyclePlan = null; resetActionQueue();
            activeGoal = config.goal(); appliedSettings = settingsKey(); refillPrepared = true;
            return true;
        } catch (java.util.concurrent.ExecutionException ex) {
            pendingPlan = null;
            planningStatus = "Planning failed. Select manual batch mode to continue.";
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); return false;
        }
    }
    private void publishSidebar() {
        if (sidebar != null) { sidebar.update(observedLevel, resinBalances, hopperPaste, targetCosts, estimate, plan, guidance,
            !settingsKey().equals(appliedSettings), activeGoal); }
    }

    private void handleLeverClick(Component clicked)
	{
        if (!batchMode.isRefilling())
        {
            refillPrepared = false;
            mixPrediction = null;
			cyclePlan = null;
			batchMode = BatchMode.REFILLING;
			trackedPotion = null;
			trackedStep = 1;
			updateState();
		}
		if (trackedPotion == null || trackedStep < 1 || trackedStep > 3)
		{
			return;
		}

		MixQueueAction expected = MixQueueAction.forStep(trackedPotion, trackedStep);
		if (expected.getComponent() != clicked)
		{
			// Do not let a mistaken click advance or replace the intended sequence.
			refreshPotionQueue();
			return;
		}
		trackedStep++;
		refreshPotionQueue();
	}

	private void handleMixClick()
	{
		if (trackedPotion == null
			|| mixPrediction != null
			|| cyclePlan == null
			|| !cyclePlan.isValid())
		{
			return;
		}

		List<InventorySlot> inventory = readInventory();
		int inventorySlot = cyclePlan.firstEmptySlot(inventory);
		if (inventorySlot < 0)
		{
			return;
		}

		Potion predictedPotion = trackedPotion;
		mixPrediction = new MixPrediction(
			predictedPotion,
			inventorySlot,
			BatchStateResolver.potionCounts(inventory),
			gameTickCounter + 8);
		trackedPotion = null;
		trackedStep = 1;
		updateState();
	}

	private void reconcileMixPrediction(List<InventorySlot> inventory)
	{
		if (mixPrediction == null)
		{
			return;
		}

		Potion actualPotion = mixPrediction.findActualPotion(inventory);

		if (actualPotion != null)
		{
			addPreviousPotion(actualPotion);
			mixPrediction = null;
			trackedPotion = null;
			trackedStep = 1;
		}
		else if (mixPrediction.isExpired(gameTickCounter))
		{
			mixPrediction = null;
			trackedPotion = null;
			trackedStep = 1;
		}
	}

	private void synchronizeActionQueue()
	{
		if (guidance.getPhase() != Guidance.Phase.MIXING || guidance.getEntry() == null)
		{
			trackedPotion = null;
			trackedStep = 1;
			upcomingQueuePotions = Collections.emptyList();
			return;
		}

		Potion guidedPotion = guidance.getEntry().getPotion();
		if (trackedPotion != guidedPotion)
		{
			trackedPotion = guidedPotion;
			trackedStep = guidance.getStepNumber() >= 1 && guidance.getStepNumber() <= 4
				? guidance.getStepNumber() : 1;
		}
		else if (guidance.getStepNumber() > trackedStep && guidance.getStepNumber() <= 4)
		{
			trackedStep = guidance.getStepNumber();
		}
		refreshPotionQueue();
	}

	private void refreshPotionQueue()
	{
		if (trackedPotion == null || trackedStep < 1 || trackedStep > 4)
		{
			upcomingQueuePotions = Collections.emptyList();
			return;
		}

		List<Potion> potions = new ArrayList<>(4);
		potions.add(trackedPotion);
		int potionIndex = !plannedPotionQueue.isEmpty() && plannedPotionQueue.get(0) == trackedPotion ? 1 : 0;
		while (potions.size() < 4 && potionIndex < plannedPotionQueue.size())
		{
			potions.add(plannedPotionQueue.get(potionIndex++));
		}
		upcomingQueuePotions = Collections.unmodifiableList(potions);
	}

	private List<Potion> buildPlannedPotionQueue(List<InventorySlot> inventory, int maximum)
	{
		if (!batchMode.isRefilling() || cyclePlan == null || !cyclePlan.isValid())
		{
			return Collections.emptyList();
		}

		List<InventorySlot> projectedInventory = new ArrayList<>(inventory);
		List<Potion> potions = new ArrayList<>(maximum);
		while (potions.size() < maximum)
		{
			int nextSlot = cyclePlan.firstEmptySlot(projectedInventory);
			if (nextSlot < 0)
			{
				break;
			}
			Potion potion = BatchStateResolver.nextNeededPotion(
				plan, cyclePlan, projectedInventory, nextSlot);
			if (potion == null)
			{
				break;
			}
			potions.add(potion);
			projectedInventory.set(nextSlot, InventorySlot.fromItemId(potion.getUnfinishedItemId()));
		}
		return Collections.unmodifiableList(potions);
	}

	private void addPreviousPotion(Potion potion)
	{
		previousQueuePotions.addLast(potion);
		while (previousQueuePotions.size() > 2)
		{
			previousQueuePotions.removeFirst();
		}
	}

	private void resetActionQueue()
	{
		previousQueuePotions.clear();
		upcomingQueuePotions = Collections.emptyList();
		plannedPotionQueue = Collections.emptyList();
		trackedPotion = null;
		trackedStep = 1;
		mixPrediction = null;
	}

	private static Component componentForLeverObject(int objectId)
	{
		for (Component component : Component.values())
		{
			if (component.getLever().getObjectId() == objectId)
			{
				return component;
			}
		}
		return null;
	}

	private boolean canUseStation(Station station)
	{
		boolean stationContainsPotion = Potion.fromVarbit(
			client.getVarbitValue(station.getPotionVarbit())) != null;
		return StationMenuGuard.canUseStation(
			guidance, station, stationContainsPotion, partialProcessingStation);
	}

	private static int countInventoryPotions(List<InventorySlot> inventory)
	{
		int count = 0;
		for (InventorySlot slot : inventory)
		{
			if (slot.isPotion())
			{
				count++;
			}
		}
		return count;
	}

	private List<InventorySlot> readInventory()
	{
		List<InventorySlot> result = new ArrayList<>(BatchPlan.INVENTORY_SIZE);
		ItemContainer container = client.getItemContainer(InventoryID.INV);
		Item[] items = container == null ? null : container.getItems();
		for (int slot = 0; slot < BatchPlan.INVENTORY_SIZE; slot++)
		{
			if (items == null || slot >= items.length || items[slot] == null)
			{
				result.add(InventorySlot.empty());
			}
			else
			{
				result.add(InventorySlot.fromItemId(items[slot].getId()));
			}
		}
		return result;
	}

	private boolean isPlayerInMixologyRoom()
	{
		Player player = client.getLocalPlayer();
		Widget ordersOverlay = client.getWidget(InterfaceID.MM_OVERLAY, 0);
		return player != null
			&& player.getWorldLocation().getRegionID() == LAB_REGION
			&& player.getWorldLocation().getPlane() == 0
			&& ordersOverlay != null
			&& !ordersOverlay.isSelfHidden();
	}

	private void deactivateLab()
	{
        inLab = false;
        stationTiming.reset(); alembicCountdown.reset();
        if (pendingPlan != null) { pendingPlan.cancel(true); pendingPlan = null; }
        refillPrepared = false; estimate = null;
        if (sidebar != null) { sidebar.showOutside(); }
        cyclePlan = null;
		partialProcessingStation = null;
		currentPotionCounts.clear();
		resetActionQueue();
		batchMode = BatchMode.REFILLING;
		guidance = Guidance.outside();
	}

	boolean isInLab()
	{
		return inLab;
	}

	Guidance getGuidance()
	{
		return guidance;
	}

	BatchPlan getPlan()
	{
		return plan;
	}

	BatchEntry getCycleEntry(int inventorySlot, Potion potion)
	{
		return cyclePlan == null || !cyclePlan.isValid()
			? null
			: cyclePlan.entryForSlot(inventorySlot, potion);
	}

	int getCurrentPotionCount(Potion potion)
	{
		return currentPotionCounts.getOrDefault(potion, 0);
	}

	int getCyclePotionCapacity()
	{
		return cyclePlan == null || !cyclePlan.isValid()
			? plan.size()
			: cyclePlan.getPotionCapacity();
	}

	List<Potion> getPreviousQueuePotions()
	{
		return Collections.unmodifiableList(new ArrayList<>(previousQueuePotions));
	}

	List<Potion> getUpcomingQueuePotions()
	{
		return upcomingQueuePotions;
	}

	Potion getNextQueuedPotion()
	{
		return upcomingQueuePotions.size() > 1 ? upcomingQueuePotions.get(1) : null;
	}

	@Provides
	MixologyBatchConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MixologyBatchConfig.class);
	}
}

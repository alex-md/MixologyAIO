package com.mixologybatch;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BatchStateResolverTest
{
	private BatchPlan plan;
	private BatchStateResolver resolver;

	@Before
	public void setUp()
	{
		plan = BatchPlan.defaultPlan();
		resolver = new BatchStateResolver();
	}

	@Test
	public void emptyInventoryStartsAtFirstLever()
	{
		List<InventorySlot> inventory = emptyInventory();
		Guidance guidance = resolve(inventory, true, null, new int[]{0, 0, 0}, noActive(), Guidance.outside());

		assertEquals(Guidance.Phase.MIXING, guidance.getPhase());
		assertEquals(Guidance.Action.PULL_LEVER, guidance.getAction());
		assertEquals(0, guidance.getEntry().getInventorySlot());
		assertEquals(Component.MOX, guidance.getComponent());
	}

	@Test
	public void fullInventoryStartsFirstStationBatch()
	{
		List<InventorySlot> inventory = inventoryForPlan(false);
		Guidance guidance = resolve(inventory, false, null, new int[]{0, 0, 0}, noActive(), Guidance.outside());

		assertEquals(Guidance.Phase.PROCESSING, guidance.getPhase());
		assertEquals(Guidance.Action.USE_STATION, guidance.getAction());
		assertEquals(Station.CRYSTALLISE, guidance.getEntry().getStation());
		assertEquals(0, guidance.getEntry().getStationPosition());
	}

	@Test
	public void finishedFirstBatchMovesToSecondStation()
	{
		List<InventorySlot> inventory = inventoryForPlan(false);
		for (int slot = 0; slot < 10; slot++)
		{
			inventory.set(slot, InventorySlot.fromItemId(plan.get(slot).getPotion().getFinishedItemId()));
		}
		Guidance guidance = resolve(inventory, false, null, new int[]{0, 0, 0}, noActive(), Guidance.outside());

		assertEquals(Station.HOMOGENISE, guidance.getEntry().getStation());
		assertEquals(10, guidance.getEntry().getInventorySlot());
	}

	@Test
	public void activeCorrectStationWaitsWithoutLosingSlot()
	{
		List<InventorySlot> inventory = inventoryForPlan(false);
		inventory.set(0, InventorySlot.empty());
		Map<Station, Potion> active = new EnumMap<>(Station.class);
		active.put(Station.CRYSTALLISE, plan.get(0).getPotion());
		Guidance previous = Guidance.processing(plan.get(0), false);

		Guidance guidance = resolve(inventory, false, null, new int[]{0, 0, 0}, active, previous);

		assertEquals(Guidance.Action.WAIT_STATION, guidance.getAction());
		assertEquals(0, guidance.getEntry().getInventorySlot());
	}

	@Test
	public void activeWrongStationPausesWithCorrection()
	{
		List<InventorySlot> inventory = inventoryForPlan(false);
		inventory.set(0, InventorySlot.empty());
		Map<Station, Potion> active = new EnumMap<>(Station.class);
		active.put(Station.CONCENTRATE, plan.get(0).getPotion());
		Guidance previous = Guidance.processing(plan.get(0), false);

		Guidance guidance = resolve(inventory, false, null, new int[]{0, 0, 0}, active, previous);

		assertEquals(Guidance.Phase.INVALID, guidance.getPhase());
		assertTrue(guidance.getMessage().contains("Alembic"));
	}

	@Test
	public void fullyFinishedBatchPointsToConveyor()
	{
		List<InventorySlot> inventory = inventoryForPlan(true);
		Guidance guidance = resolve(inventory, false, null, new int[]{0, 0, 0}, noActive(), Guidance.outside());

		assertEquals(Guidance.Phase.COMPLETE, guidance.getPhase());
		assertEquals(Guidance.Action.DEPOSIT, guidance.getAction());
	}

	@Test
	public void leftoverGapsCanStartARefillCycle()
	{
		List<InventorySlot> inventory = emptyInventory();
		inventory.set(18, InventorySlot.fromItemId(Potion.MAL.getUnfinishedItemId()));
		inventory.set(19, InventorySlot.fromItemId(Potion.MMA.getUnfinishedItemId()));

		Guidance guidance = resolve(inventory, true, null, new int[]{1, 0, 0}, noActive(), Guidance.outside());

		assertEquals(Guidance.Phase.MIXING, guidance.getPhase());
		assertEquals(0, guidance.getEntry().getInventorySlot());
	}

	@Test
	public void manuallyMixedPotionIsAcceptedOutOfOrder()
	{
		List<InventorySlot> inventory = emptyInventory();
		inventory.set(0, InventorySlot.fromItemId(Potion.MAL.getUnfinishedItemId()));

		Guidance guidance = resolve(inventory, true, null, new int[]{0, 0, 0}, noActive(), Guidance.outside());

		assertEquals(Guidance.Phase.MIXING, guidance.getPhase());
		assertEquals(1, guidance.getEntry().getInventorySlot());
		assertEquals(Integer.valueOf(1), BatchStateResolver.potionCounts(inventory).get(Potion.MAL));
	}

	@Test
	public void wrongMixerContentsKeepTheIntendedRecipeVisible()
	{
		List<InventorySlot> inventory = emptyInventory();

		Guidance guidance = resolve(inventory, true, null, new int[]{3, 3, 3}, noActive(), Guidance.outside());

		assertEquals(Guidance.Phase.MIXING, guidance.getPhase());
		assertEquals(Potion.MMA, guidance.getEntry().getPotion());
		assertEquals(Guidance.Action.PULL_LEVER, guidance.getAction());
	}

	@Test
	public void processingSkipsDepositedInventoryGaps()
	{
		List<InventorySlot> inventory = emptyInventory();
		inventory.set(18, InventorySlot.fromItemId(Potion.MAL.getUnfinishedItemId()));
		inventory.set(19, InventorySlot.fromItemId(Potion.MMA.getUnfinishedItemId()));

		Guidance guidance = resolve(inventory, false, null, new int[]{0, 0, 0}, noActive(), Guidance.outside());

		assertEquals(Guidance.Phase.PROCESSING, guidance.getPhase());
		assertEquals(18, guidance.getEntry().getInventorySlot());
	}

	@Test
	public void partialInventorySelectsItsEarliestUnfinishedStation()
	{
		List<InventorySlot> inventory = emptyInventory();
		inventory.set(0, InventorySlot.fromItemId(Potion.MMA.getFinishedItemId()));
		inventory.set(10, InventorySlot.fromItemId(Potion.ALA.getUnfinishedItemId()));
		CyclePlan cycle = CyclePlan.create(plan, inventory);

		BatchEntry unfinished = BatchStateResolver.firstUnfinishedEntry(cycle, inventory);

		assertEquals(10, unfinished.getInventorySlot());
		assertEquals(Station.HOMOGENISE, unfinished.getStation());
	}

	@Test
	public void digweedReservesOneSlotWithoutPausingTheBatch()
	{
		List<InventorySlot> inventory = emptyInventory();
		inventory.set(4, InventorySlot.fromItemId(ItemID.MM_LAB_SPECIAL_HERB));
		CyclePlan cycle = CyclePlan.create(plan, inventory);

		assertTrue(cycle.isValid());
		assertEquals(27, cycle.getPotionCapacity());
		Guidance guidance = resolver.resolve(
			plan, cycle, true, inventory, null, new int[]{0, 0, 0}, noActive(), Guidance.outside());
		assertEquals(Guidance.Phase.MIXING, guidance.getPhase());

		while (cycle.firstEmptySlot(inventory) >= 0)
		{
			int nextSlot = cycle.firstEmptySlot(inventory);
			Potion potion = BatchStateResolver.nextNeededPotion(plan, cycle, inventory, nextSlot);
			inventory.set(nextSlot, InventorySlot.fromItemId(potion.getUnfinishedItemId()));
		}
		guidance = resolver.resolve(
			plan, cycle, true, inventory, null, new int[]{0, 0, 0}, noActive(), Guidance.outside());
		assertEquals(Guidance.Phase.PROCESSING, guidance.getPhase());
	}

	@Test
	public void pickingUpDigweedDuringMixingReducesOnlyTheCurrentCycleCapacity()
	{
		List<InventorySlot> inventory = emptyInventory();
		CyclePlan cycle = CyclePlan.create(plan, inventory);

		inventory.set(4, InventorySlot.fromItemId(ItemID.MM_LAB_SPECIAL_HERB));
		cycle.observeInventory(inventory);
		assertEquals(27, cycle.getPotionCapacity());

		inventory.set(4, InventorySlot.empty());
		cycle.observeInventory(inventory);
		assertEquals(27, cycle.getPotionCapacity());

		inventory.set(4, InventorySlot.fromItemId(Potion.MAL.getUnfinishedItemId()));
		cycle.observeInventory(inventory);
		assertEquals(28, cycle.getPotionCapacity());
	}

	@Test
	public void rollingRefillIgnoresAnyNumberOfFinishedMixalots()
	{
		List<InventorySlot> inventory = emptyInventory();
		for (int slot = 0; slot < 6; slot++)
		{
			inventory.set(slot, InventorySlot.fromItemId(Potion.MAL.getFinishedItemId()));
		}

		assertTrue(BatchStateResolver.shouldStartRollingRefill(inventory));
	}

	@Test
	public void rollingRefillStartsAtTwoOtherFinishedPotions()
	{
		List<InventorySlot> inventory = emptyInventory();
		inventory.set(0, InventorySlot.fromItemId(Potion.MAL.getFinishedItemId()));
		inventory.set(1, InventorySlot.fromItemId(Potion.MMA.getFinishedItemId()));
		inventory.set(2, InventorySlot.fromItemId(Potion.ALL.getFinishedItemId()));

		assertTrue(BatchStateResolver.shouldStartRollingRefill(inventory));
	}

	@Test
	public void rollingRefillWaitsWithThreeOtherPotionsOrAnyUnfinishedPotion()
	{
		List<InventorySlot> inventory = emptyInventory();
		inventory.set(0, InventorySlot.fromItemId(Potion.MMA.getFinishedItemId()));
		inventory.set(1, InventorySlot.fromItemId(Potion.ALL.getFinishedItemId()));
		inventory.set(2, InventorySlot.fromItemId(Potion.MML.getFinishedItemId()));
		assertTrue(!BatchStateResolver.shouldStartRollingRefill(inventory));

		inventory.set(2, InventorySlot.empty());
		inventory.set(3, InventorySlot.fromItemId(Potion.MAL.getUnfinishedItemId()));
		assertTrue(!BatchStateResolver.shouldStartRollingRefill(inventory));
	}

	private Guidance resolve(
		List<InventorySlot> inventory,
		boolean refilling,
		Potion vessel,
		int[] mixer,
		Map<Station, Potion> active,
		Guidance previous)
	{
		return resolver.resolve(
			plan,
			CyclePlan.create(plan, inventory),
			refilling,
			inventory,
			vessel,
			mixer,
			active,
			previous);
	}

	private List<InventorySlot> inventoryForPlan(boolean finished)
	{
		List<InventorySlot> inventory = emptyInventory();
		for (BatchEntry entry : plan.getEntries())
		{
			int itemId = finished
				? entry.getPotion().getFinishedItemId()
				: entry.getPotion().getUnfinishedItemId();
			inventory.set(entry.getInventorySlot(), InventorySlot.fromItemId(itemId));
		}
		return inventory;
	}

	private static List<InventorySlot> emptyInventory()
	{
		List<InventorySlot> inventory = new ArrayList<>(BatchPlan.INVENTORY_SIZE);
		for (int slot = 0; slot < BatchPlan.INVENTORY_SIZE; slot++)
		{
			inventory.add(InventorySlot.empty());
		}
		return inventory;
	}

	private static Map<Station, Potion> noActive()
	{
		return new EnumMap<>(Station.class);
	}
}

package com.mixologybatch;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

final class BatchQueueOverlay extends OverlayPanel
{
	private static final Color PREVIOUS = new Color(145, 145, 145);
	private static final Color CURRENT = new Color(255, 215, 70);
	private static final Color NEXT = Color.WHITE;

	private final MixologyBatchPlugin plugin;
	private final MixologyBatchConfig config;

	@Inject
	private BatchQueueOverlay(MixologyBatchPlugin plugin, MixologyBatchConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		panelComponent.setPreferredSize(new Dimension(190, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInLab() || !config.showActionQueue())
		{
			return null;
		}
		if (plugin.getGuidance().getPhase() != Guidance.Phase.MIXING)
		{
			return null;
		}

		List<Potion> previous = plugin.getPreviousQueuePotions();
		List<Potion> upcoming = plugin.getUpcomingQueuePotions();
		if (previous.isEmpty() && upcoming.isEmpty())
		{
			return null;
		}

		panelComponent.getChildren().add(TitleComponent.builder().text("Potion queue").build());
		int previousOffset = -previous.size();
		for (int index = 0; index < previous.size(); index++)
		{
			addPotion(Integer.toString(previousOffset + index), previous.get(index), PREVIOUS);
		}
		for (int index = 0; index < upcoming.size(); index++)
		{
			addPotion(index == 0 ? "NOW" : "+" + index, upcoming.get(index), index == 0 ? CURRENT : NEXT);
		}
		return super.render(graphics);
	}

	private void addPotion(String position, Potion potion, Color color)
	{
		panelComponent.getChildren().add(LineComponent.builder()
			.left(position)
			.leftColor(color)
			.right(potion.name() + "  " + potion.getRecipeSequence())
			.rightColor(color)
			.build());
	}
}

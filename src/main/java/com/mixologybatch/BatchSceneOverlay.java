package com.mixologybatch;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

final class BatchSceneOverlay extends Overlay
{
	private static final Color VESSEL_COLOR = Color.WHITE;
	private static final Color COMPLETE_COLOR = new Color(0, 255, 90);

	private final Client client;
	private final MixologyBatchPlugin plugin;
	private final MixologyBatchConfig config;
	private final LabObjectResolver objects;
	private final ModelOutlineRenderer outliner;

	@Inject
	private BatchSceneOverlay(
		Client client,
		MixologyBatchPlugin plugin,
		MixologyBatchConfig config,
		LabObjectResolver objects,
		ModelOutlineRenderer outliner)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.objects = objects;
		this.outliner = outliner;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInLab() || !config.showSceneGuidance())
		{
			return null;
		}

		Guidance guidance = plugin.getGuidance();
		if (guidance.getPhase() == Guidance.Phase.MIXING)
		{
            LabObject next = mixingTarget(guidance);
            if (next != null) {
                Color color = guidance.getComponent() == null ? VESSEL_COLOR : guidance.getComponent().getColor();
                drawTarget(graphics, next, guidance.getComponent() == null ? "MIX" : guidance.getComponent().name(), color);
            }
            return null;
        }

		LabObject target;
		Color color;
		String label;
		switch (guidance.getAction())
		{
			case USE_STATION:
				target = guidance.getEntry().getStation().getLabObject();
				color = config.stationColor();
				label = stationLabel(guidance.getEntry(), false);
				break;
			case WAIT_STATION:
				target = guidance.getEntry().getStation().getLabObject();
				color = config.stationColor();
				label = stationLabel(guidance.getEntry(), true);
				break;
			case DEPOSIT:
				target = LabObject.CONVEYOR;
				color = COMPLETE_COLOR;
				label = "BATCH READY";
				break;
			default:
				return null;
		}

        if (guidance.getAction() == Guidance.Action.WAIT_STATION && config.stationTimingCues()) {
            Station station = guidance.getEntry().getStation();
            long now = StationTiming.now();
            boolean ready = plugin.getStationTiming().isUrgent(station, now);
            if (station == Station.CRYSTALLISE && config.alembicBorderTimer()) {
                double progress = plugin.getAlembicCountdown(now);
                if (ready || progress >= 0) {
                    TileObject object = objects.find(target);
                    if (object != null) {
                        java.awt.Shape hull = object instanceof net.runelite.api.GameObject
                            ? ((net.runelite.api.GameObject)object).getConvexHull() : object.getClickbox();
                        if (hull != null) { StationBorderTimer.draw(graphics, hull, progress, ready); return null; }
                    }
                }
            }
            if (ready) {
                TileObject object = objects.find(target);
                if (object != null) {
                    outliner.drawOutline(object, config.outlineWidth(), StationBorderTimer.READY, config.outlineFeather());
                }
                return null;
            }
            // Quiet processing outline; no persistent countdown text or extra panel.
            TileObject object = objects.find(target);
            if (object != null) { outliner.drawOutline(object, config.outlineWidth(), color, config.outlineFeather()); }
            return null;
        }
        drawTarget(graphics, target, label, color);
		return null;
	}

    static LabObject mixingTarget(Guidance guidance) {
        if (guidance.getAction() == Guidance.Action.PULL_LEVER && guidance.getComponent() != null) {
            return guidance.getComponent().getLever();
        }
        return guidance.getAction() == Guidance.Action.MIX_VESSEL ? LabObject.MIXING_VESSEL : null;
    }


	private void drawTarget(Graphics2D graphics, LabObject target, String label, Color color)
	{
		TileObject object = objects.find(target);
		if (object == null)
		{
			return;
		}
		outliner.drawOutline(object, config.outlineWidth(), color, config.outlineFeather());
		drawLabel(graphics, object, label, color, 0);
	}

	private static String stationLabel(BatchEntry entry, boolean active)
	{
		return (entry.getStationPosition() + 1) + "/" + entry.getStationTotal()
			+ " " + (active ? "PROCESS" : entry.getStation().getActionName().toUpperCase());
	}

	private void drawLabel(
		Graphics2D graphics,
		TileObject object,
		String text,
		Color color,
		int verticalOffset)
	{
		graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 16f));
		Point location = Perspective.getCanvasTextLocation(client, graphics, object.getLocalLocation(), text, 120);
		if (location == null)
		{
			return;
		}
		int y = location.getY() + verticalOffset;
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, location.getX() + 1, y + 1);
		graphics.setColor(color);
		graphics.drawString(text, location.getX(), y);
	}
}

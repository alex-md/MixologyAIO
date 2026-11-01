package com.mixologybatch;

import java.awt.*;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.components.LineComponent;

/** Small, high-contrast cue visible even when the station model is obscured. */
final class StationTimingOverlay extends OverlayPanel
{
    private final MixologyBatchPlugin plugin;
    private final MixologyBatchConfig config;
    @Inject StationTimingOverlay(MixologyBatchPlugin plugin, MixologyBatchConfig config) {
        this.plugin = plugin; this.config = config;
        setPosition(OverlayPosition.TOP_CENTER); setLayer(OverlayLayer.ABOVE_WIDGETS);
        panelComponent.setPreferredSize(new Dimension(220, 0));
    }
    @Override public Dimension render(Graphics2D graphics) {
        if (!plugin.isInLab() || !config.stationTimingCues() || !config.timingBanner()) { return null; }
        Station station = plugin.getTimingStation();
        if (station == null) { return null; }
        long now = StationTiming.now();
        String message = plugin.getStationTiming().message(station, now);
        if (message == null) { return null; }
        boolean urgent = plugin.getStationTiming().isUrgent(station, now);
        panelComponent.setBackgroundColor(urgent ? new Color(25, 70, 35, 235) : new Color(30, 30, 30, 190));
        panelComponent.getChildren().add(TitleComponent.builder().text(message)
            .color(urgent ? new Color(130, 255, 150) : Color.LIGHT_GRAY).build());
        panelComponent.getChildren().add(LineComponent.builder().left(station.getObjectName())
            .right(station == Station.CONCENTRATE ? "Each game tick" : urgent ? "Speedup window" : "Processing").build());
        return super.render(graphics);
    }
}

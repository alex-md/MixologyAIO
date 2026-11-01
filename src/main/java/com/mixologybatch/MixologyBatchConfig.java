package com.mixologybatch;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(MixologyBatchConfig.GROUP)
public interface MixologyBatchConfig extends Config
{
	String GROUP = "mixologyaio";

    @ConfigItem(keyName = "goal", name = "Goal", description = "Automatically plan full inventories, or override recipe quantities", position = -3)
    default OptimizationGoal goal() { return OptimizationGoal.BALANCED; }

    @ConfigItem(keyName = "rewardTargets", name = "Reward targets", description = "Managed in the Mixology AIO sidebar", hidden = true)
    default String rewardTargets() { return ""; }

    @Range(min = 4, max = 60)
    @ConfigItem(keyName = "processingSeconds", name = "Seconds per station", description = "Planning estimate per potion; adjust for your attention level. Timing is approximate.", position = 1, section = DISPLAY)
    default int processingSeconds() { return 18; }


	@ConfigSection(
		name = "Manual batch fallback",
		description = "Number of copies to distribute evenly across the three processing stations",
		position = 10,
        closedByDefault = true
	)
	String POTIONS = "potions";

	@ConfigSection(
		name = "Display",
		description = "Overlay and highlight settings",
		position = 20,
        closedByDefault = true
	)
	String DISPLAY = "display";

	@ConfigItem(
		keyName = "stationOrder",
		name = "Station order",
		description = "Order of the three contiguous station batches. Concentrate last is safest for spam-clicking.",
		position = 0
	)
	default StationOrder stationOrder()
	{
		return StationOrder.CRYSTALLISE_HOMOGENISE_CONCENTRATE;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "mmmCount", name = "MMM", description = "Mammoth-might mix copies", position = 0)
	default int mmmCount()
	{
		return 0;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "mmaCount", name = "MMA", description = "Mystic mana amalgam copies", position = 1)
	default int mmaCount()
	{
		return 3;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "mmlCount", name = "MML", description = "Marley's moonlight copies", position = 2)
	default int mmlCount()
	{
		return 4;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "aaaCount", name = "AAA", description = "Alco-augmentator copies", position = 3)
	default int aaaCount()
	{
		return 0;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "aamCount", name = "AAM", description = "Azure aura mix copies", position = 4)
	default int aamCount()
	{
		return 3;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "alaCount", name = "ALA", description = "Aqualux amalgam copies", position = 5)
	default int alaCount()
	{
		return 3;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "lllCount", name = "LLL", description = "Liplack liquor copies", position = 6)
	default int lllCount()
	{
		return 3;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "mllCount", name = "MLL", description = "Megalite liquid copies", position = 7)
	default int mllCount()
	{
		return 3;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "allCount", name = "ALL", description = "Anti-leech lotion copies", position = 8)
	default int allCount()
	{
		return 3;
	}

	@Range(min = 0, max = 28)
	@ConfigItem(section = POTIONS, keyName = "malCount", name = "MAL", description = "Mixalot copies", position = 9)
	default int malCount()
	{
		return 6;
	}

	@ConfigItem(
		section = DISPLAY,
		keyName = "showPanel",
		name = "Show instruction panel",
		description = "Show the current action and live per-recipe inventory totals",
		position = 0
	)
	default boolean showPanel()
	{
		return true;
	}

	@ConfigItem(
		section = DISPLAY,
		keyName = "showSceneGuidance",
		name = "Show scene guidance",
		description = "Highlight only the next lever or vessel, then the current station or conveyor",
		position = 1
	)
	default boolean showSceneGuidance()
	{
		return true;
	}

	@ConfigItem(
		section = DISPLAY,
		keyName = "showInventoryBatches",
		name = "Number inventory batches",
		description = "Mark each planned potion slot with its station batch number",
		position = 2
	)
	default boolean showInventoryBatches()
	{
		return true;
	}

	@ConfigItem(
		section = DISPLAY,
		keyName = "showActionQueue",
		name = "Show potion queue",
		description = "Show the previous two, current, and next three potions with their full recipes",
		position = 3
	)
	default boolean showActionQueue()
	{
		return true;
	}

	@ConfigItem(
		section = DISPLAY,
		keyName = "guardWrongStations",
		name = "Guard wrong stations",
		description = "Swap the station's existing Check option into left-click position while it is unavailable",
		position = 4
	)
	default boolean guardWrongStations()
	{
		return true;
	}

    @ConfigItem(section = DISPLAY, keyName = "stationTimingCues", name = "Station timing cues",
        description = "Show a bright station highlight and compact CLICK NOW banner for actual speedup windows", position = 8)
    default boolean stationTimingCues() { return true; }

    @ConfigItem(section = DISPLAY, keyName = "alembicBorderTimer", name = "Alembic border timer",
        description = "A quiet countdown around the station, calibrated after its first observed speedup cue", position = 10)
    default boolean alembicBorderTimer() { return true; }

    @ConfigItem(section = DISPLAY, keyName = "timingBanner", name = "Separate timing banner",
        description = "Optional additional timing panel; off by default to keep cues on the station", position = 11)
    default boolean timingBanner() { return false; }

    @ConfigItem(section = DISPLAY, keyName = "timingSound", name = "Timing cue sound",
        description = "Play a short chime at the actual station speedup effect; replaces the delayed Alembic chime", position = 9)
    default boolean timingSound() { return true; }

	@Alpha
	@ConfigItem(
		section = DISPLAY,
		keyName = "stationColor",
		name = "Station highlight",
		description = "Colour used for station outlines and the active inventory slot",
		position = 5
	)
	default Color stationColor()
	{
		return new Color(105, 190, 205, 255);
	}

	@Range(min = 1, max = 8)
	@ConfigItem(
		section = DISPLAY,
		keyName = "outlineWidth",
		name = "Outline width",
		description = "Width of scene-object outlines",
		position = 6
	)
	default int outlineWidth()
	{
		return 3;
	}

	@Range(min = 0, max = 4)
	@ConfigItem(
		section = DISPLAY,
		keyName = "outlineFeather",
		name = "Outline feather",
		description = "Softness of scene-object outlines",
		position = 7
	)
	default int outlineFeather()
	{
		return 1;
	}
}

package com.mixologybatch;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.MenuAction;

final class StationMenuGuard
{
	private StationMenuGuard()
	{
	}

	static Station stationForObjectId(int objectId)
	{
		for (Station station : Station.values())
		{
			if (station.getLabObject().getObjectId() == objectId)
			{
				return station;
			}
		}
		return null;
	}

	static boolean canUseStation(
		Guidance guidance,
		Station station,
		boolean stationContainsPotion,
		Station partialInventoryStation)
	{
		if (stationContainsPotion)
		{
			return true;
		}
		if (guidance.getPhase() == Guidance.Phase.PROCESSING)
		{
			return guidance.getEntry() != null
				&& guidance.getEntry().getStation() == station
				&& (guidance.getAction() == Guidance.Action.USE_STATION
					|| guidance.getAction() == Guidance.Action.WAIT_STATION);
		}
		return partialInventoryStation == station;
	}

	/**
	 * Collects station menu entries by their exact scene object. This prevents a
	 * Check option belonging to one nearby object from being swapped with the
	 * processing option belonging to another.
	 */
	static final class MenuScan
	{
		private final List<TargetEntries> targets = new ArrayList<>();

		void accept(
			int index,
			int objectId,
			int sceneX,
			int sceneY,
			MenuAction type,
			String option)
		{
			Station station = stationForObjectId(objectId);
			if (station == null)
			{
				return;
			}

			TargetEntries target = findOrCreate(station, objectId, sceneX, sceneY);
			if (type == MenuAction.GAME_OBJECT_FIRST_OPTION)
			{
				target.processingIndex = index;
			}
			else if ("Check".equalsIgnoreCase(option))
			{
				target.checkIndex = index;
			}
		}

		MenuSwap select()
		{
			TargetEntries selected = null;
			for (TargetEntries target : targets)
			{
				if (target.processingIndex >= 0
					&& (selected == null
						|| target.processingIndex > selected.processingIndex))
				{
					selected = target;
				}
			}

			if (selected == null || selected.checkIndex < 0)
			{
				return null;
			}
			return new MenuSwap(
				selected.station,
				selected.processingIndex,
				selected.checkIndex);
		}

		private TargetEntries findOrCreate(
			Station station,
			int objectId,
			int sceneX,
			int sceneY)
		{
			for (TargetEntries target : targets)
			{
				if (target.objectId == objectId
					&& target.sceneX == sceneX
					&& target.sceneY == sceneY)
				{
					return target;
				}
			}

			TargetEntries target = new TargetEntries(station, objectId, sceneX, sceneY);
			targets.add(target);
			return target;
		}
	}

	static final class MenuSwap
	{
		private final Station station;
		private final int processingIndex;
		private final int checkIndex;

		private MenuSwap(Station station, int processingIndex, int checkIndex)
		{
			this.station = station;
			this.processingIndex = processingIndex;
			this.checkIndex = checkIndex;
		}

		Station getStation()
		{
			return station;
		}

		int getProcessingIndex()
		{
			return processingIndex;
		}

		int getCheckIndex()
		{
			return checkIndex;
		}
	}

	private static final class TargetEntries
	{
		private final Station station;
		private final int objectId;
		private final int sceneX;
		private final int sceneY;
		private int processingIndex = -1;
		private int checkIndex = -1;

		private TargetEntries(Station station, int objectId, int sceneX, int sceneY)
		{
			this.station = station;
			this.objectId = objectId;
			this.sceneX = sceneX;
			this.sceneY = sceneY;
		}
	}
}

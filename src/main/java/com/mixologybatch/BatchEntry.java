package com.mixologybatch;

final class BatchEntry
{
	private final Potion potion;
	private final Station station;
	private final int inventorySlot;
	private final int stationOrdinal;
	private final int stationPosition;
	private final int stationTotal;

	BatchEntry(
		Potion potion,
		Station station,
		int inventorySlot,
		int stationOrdinal,
		int stationPosition,
		int stationTotal)
	{
		this.potion = potion;
		this.station = station;
		this.inventorySlot = inventorySlot;
		this.stationOrdinal = stationOrdinal;
		this.stationPosition = stationPosition;
		this.stationTotal = stationTotal;
	}

	Potion getPotion()
	{
		return potion;
	}

	Station getStation()
	{
		return station;
	}

	int getInventorySlot()
	{
		return inventorySlot;
	}

	int getStationOrdinal()
	{
		return stationOrdinal;
	}

	int getStationPosition()
	{
		return stationPosition;
	}

	int getStationTotal()
	{
		return stationTotal;
	}

	BatchEntry remap(Potion remappedPotion, int remappedInventorySlot)
	{
		return new BatchEntry(
			remappedPotion,
			station,
			remappedInventorySlot,
			stationOrdinal,
			stationPosition,
			stationTotal);
	}
}

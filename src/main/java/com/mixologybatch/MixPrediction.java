package com.mixologybatch;

import java.util.EnumMap;
import java.util.List;

final class MixPrediction
{
	private final Potion potion;
	private final int inventorySlot;
	private final EnumMap<Potion, Integer> baselineCounts;
	private final int expiresAfterTick;

	MixPrediction(
		Potion potion,
		int inventorySlot,
		EnumMap<Potion, Integer> baselineCounts,
		int expiresAfterTick)
	{
		this.potion = potion;
		this.inventorySlot = inventorySlot;
		this.baselineCounts = new EnumMap<>(baselineCounts);
		this.expiresAfterTick = expiresAfterTick;
	}

	Potion getPotion()
	{
		return potion;
	}

	int getInventorySlot()
	{
		return inventorySlot;
	}

	Potion findActualPotion(List<InventorySlot> inventory)
	{
		if (inventorySlot >= 0 && inventorySlot < inventory.size())
		{
			InventorySlot predictedSlot = inventory.get(inventorySlot);
			if (predictedSlot.isPotion())
			{
				return predictedSlot.getPotion();
			}
		}

		EnumMap<Potion, Integer> actualCounts = BatchStateResolver.potionCounts(inventory);
		for (Potion candidate : Potion.values())
		{
			if (actualCounts.getOrDefault(candidate, 0)
				> baselineCounts.getOrDefault(candidate, 0))
			{
				return candidate;
			}
		}
		return null;
	}

	boolean isExpired(int currentTick)
	{
		return currentTick > expiresAfterTick;
	}
}

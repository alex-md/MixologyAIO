package com.mixologybatch;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MixPredictionTest
{
	@Test
	public void predictedSlotReconcilesToThePotionActuallyMade()
	{
		List<InventorySlot> inventory = emptyInventory();
		MixPrediction prediction = prediction(Potion.ALA, 4, inventory, 20);
		inventory.set(4, InventorySlot.fromItemId(Potion.MAL.getUnfinishedItemId()));

		assertEquals(Potion.MAL, prediction.findActualPotion(inventory));
	}

	@Test
	public void countIncreaseFindsPotionPlacedInADifferentSlot()
	{
		List<InventorySlot> inventory = emptyInventory();
		inventory.set(2, InventorySlot.fromItemId(Potion.ALA.getFinishedItemId()));
		MixPrediction prediction = prediction(Potion.MAL, 4, inventory, 20);
		inventory.set(7, InventorySlot.fromItemId(Potion.MML.getUnfinishedItemId()));

		assertEquals(Potion.MML, prediction.findActualPotion(inventory));
	}

	@Test
	public void noInventoryChangeDoesNotConfirmThePrediction()
	{
		List<InventorySlot> inventory = emptyInventory();
		MixPrediction prediction = prediction(Potion.MAL, 4, inventory, 20);

		assertNull(prediction.findActualPotion(inventory));
	}

	@Test
	public void expiryIncludesTheLastExpectedCompletionTick()
	{
		MixPrediction prediction = prediction(Potion.MAL, 4, emptyInventory(), 20);

		assertFalse(prediction.isExpired(20));
		assertTrue(prediction.isExpired(21));
	}

	private static MixPrediction prediction(
		Potion potion,
		int slot,
		List<InventorySlot> inventory,
		int expiry)
	{
		EnumMap<Potion, Integer> counts = BatchStateResolver.potionCounts(inventory);
		return new MixPrediction(potion, slot, counts, expiry);
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
}

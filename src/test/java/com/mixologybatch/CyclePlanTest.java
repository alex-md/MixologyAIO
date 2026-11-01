package com.mixologybatch;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CyclePlanTest
{
	@Test
	public void projectsARefillAroundPotionsInLaterSlots()
	{
		EnumMap<Potion, Integer> counts = new EnumMap<>(Potion.class);
		counts.put(Potion.ALA, 1);
		counts.put(Potion.MAL, 6);
		BatchPlan target = BatchPlan.create(counts, StationOrder.CRYSTALLISE_HOMOGENISE_CONCENTRATE);
		List<InventorySlot> inventory = emptyInventory();
		inventory.set(18, InventorySlot.fromItemId(Potion.MAL.getUnfinishedItemId()));
		inventory.set(19, InventorySlot.fromItemId(Potion.MMA.getUnfinishedItemId()));

		CyclePlan cycle = CyclePlan.create(target, inventory);

		assertTrue(cycle.isValid());
		assertEquals(7, cycle.size());
		assertEquals(0, cycle.firstEmptySlot(inventory));
		assertEquals(Station.CONCENTRATE, cycle.entryForSlot(18, Potion.MAL).getStation());
		assertEquals(Station.CONCENTRATE, cycle.entryForSlot(19, Potion.MMA).getStation());
	}

	@Test
	public void generatedRecipesFollowTheStationFlattenedPlan()
	{
		BatchPlan target = BatchPlan.defaultPlan();
		List<InventorySlot> inventory = emptyInventory();
		CyclePlan cycle = CyclePlan.create(target, inventory);

		for (int rank = 0; rank < target.size(); rank++)
		{
			int nextSlot = cycle.firstEmptySlot(inventory);
			Potion nextPotion = BatchStateResolver.nextNeededPotion(target, cycle, inventory, nextSlot);

			assertEquals(target.get(rank).getStation(), cycle.entryForSlot(nextSlot, nextPotion).getStation());
			assertEquals(target.get(rank).getPotion(), nextPotion);
			inventory.set(nextSlot, InventorySlot.fromItemId(nextPotion.getUnfinishedItemId()));
		}
	}

	@Test
	public void finishedPotionCountsButConsumedPotionBecomesNeededAgain()
	{
		BatchPlan target = BatchPlan.defaultPlan();
		List<InventorySlot> inventory = emptyInventory();
		inventory.set(0, InventorySlot.fromItemId(target.get(0).getPotion().getFinishedItemId()));
		CyclePlan withFinishedPotion = CyclePlan.create(target, inventory);

		int nextSlot = withFinishedPotion.firstEmptySlot(inventory);
		assertEquals(
			target.get(1).getPotion(),
			BatchStateResolver.nextNeededPotion(target, withFinishedPotion, inventory, nextSlot));

		inventory.set(0, InventorySlot.empty());
		CyclePlan afterConsumption = CyclePlan.create(target, inventory);
		nextSlot = afterConsumption.firstEmptySlot(inventory);
		assertEquals(
			target.get(0).getPotion(),
			BatchStateResolver.nextNeededPotion(target, afterConsumption, inventory, nextSlot));
	}

	@Test
	public void leftoversDoNotMakeRefillingJumpBetweenStations()
	{
		BatchPlan target = BatchPlan.defaultPlan();
		List<InventorySlot> inventory = emptyInventory();
		inventory.set(8, InventorySlot.fromItemId(Potion.MAL.getFinishedItemId()));
		inventory.set(15, InventorySlot.fromItemId(Potion.ALA.getUnfinishedItemId()));
		inventory.set(25, InventorySlot.fromItemId(Potion.MML.getFinishedItemId()));
		CyclePlan cycle = CyclePlan.create(target, inventory);

		int previousStationOrdinal = -1;
		while (cycle.firstEmptySlot(inventory) >= 0)
		{
			int nextSlot = cycle.firstEmptySlot(inventory);
			Potion nextPotion = BatchStateResolver.nextNeededPotion(target, cycle, inventory, nextSlot);
			BatchEntry nextEntry = cycle.entryForSlot(nextSlot, nextPotion);

			assertTrue(nextEntry.getStationOrdinal() >= previousStationOrdinal);
			previousStationOrdinal = nextEntry.getStationOrdinal();
			inventory.set(nextSlot, InventorySlot.fromItemId(nextPotion.getUnfinishedItemId()));
		}
	}

	@Test
	public void movingAnOrdinaryItemDoesNotPermanentlyShrinkCapacity()
	{
		List<InventorySlot> inventory = emptyInventory();
		for (int slot = 0; slot < 4; slot++)
		{
			inventory.set(slot, InventorySlot.fromItemId(Potion.MAL.getUnfinishedItemId()));
		}
		inventory.set(4, InventorySlot.fromItemId(1));
		CyclePlan cycle = CyclePlan.create(BatchPlan.defaultPlan(), inventory);
		assertEquals(27, cycle.getPotionCapacity());

		inventory.set(4, InventorySlot.empty());
		inventory.set(5, InventorySlot.fromItemId(1));
		cycle.observeInventory(inventory);
		assertEquals(27, cycle.getPotionCapacity());
		assertEquals(4, cycle.firstEmptySlot(inventory));

		inventory.set(5, InventorySlot.empty());
		cycle.observeInventory(inventory);
		assertEquals(28, cycle.getPotionCapacity());
	}

	@Test
	public void movingDigweedTransfersItsSingleReservation()
	{
		List<InventorySlot> inventory = emptyInventory();
		inventory.set(4, InventorySlot.fromItemId(ItemID.MM_LAB_SPECIAL_HERB));
		CyclePlan cycle = CyclePlan.create(BatchPlan.defaultPlan(), inventory);

		inventory.set(4, InventorySlot.empty());
		inventory.set(5, InventorySlot.fromItemId(ItemID.MM_LAB_SPECIAL_HERB));
		cycle.observeInventory(inventory);

		assertEquals(27, cycle.getPotionCapacity());
		assertEquals(0, cycle.firstEmptySlot(inventory));
	}

	@Test
	public void consumedDigweedReservationClearsWhenPotionFillsItsSlot()
	{
		List<InventorySlot> inventory = emptyInventory();
		inventory.set(4, InventorySlot.fromItemId(ItemID.MM_LAB_SPECIAL_HERB));
		CyclePlan cycle = CyclePlan.create(BatchPlan.defaultPlan(), inventory);

		inventory.set(4, InventorySlot.empty());
		cycle.observeInventory(inventory);
		assertEquals(27, cycle.getPotionCapacity());

		inventory.set(4, InventorySlot.fromItemId(Potion.MAL.getUnfinishedItemId()));
		cycle.observeInventory(inventory);
		assertEquals(28, cycle.getPotionCapacity());
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

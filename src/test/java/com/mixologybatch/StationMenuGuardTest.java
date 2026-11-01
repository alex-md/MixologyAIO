package com.mixologybatch;

import net.runelite.api.MenuAction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class StationMenuGuardTest
{
	@Test
	public void onlyPlannedStationIsUsableDuringProcessing()
	{
		BatchEntry entry = BatchPlan.defaultPlan().get(0);
		Guidance guidance = Guidance.processing(entry, false);

		assertTrue(StationMenuGuard.canUseStation(guidance, entry.getStation(), false, null));
		assertFalse(StationMenuGuard.canUseStation(guidance, Station.HOMOGENISE, false, null));
		assertFalse(StationMenuGuard.canUseStation(guidance, Station.CONCENTRATE, false, null));
	}

	@Test
	public void allStationsAreGuardedWhileMixing()
	{
		BatchEntry entry = BatchPlan.defaultPlan().get(0);
		Guidance guidance = Guidance.mixing(
			entry,
			MixStep.resolve(entry.getPotion(), new int[]{0, 0, 0}, false));

		for (Station station : Station.values())
		{
			assertFalse(StationMenuGuard.canUseStation(guidance, station, false, null));
		}
	}

	@Test
	public void earliestPartialInventoryStationRemainsUsableWhileMixing()
	{
		BatchEntry entry = BatchPlan.defaultPlan().get(0);
		Guidance guidance = Guidance.mixing(
			entry,
			MixStep.resolve(entry.getPotion(), new int[]{0, 0, 0}, false));

		assertTrue(StationMenuGuard.canUseStation(
			guidance, Station.HOMOGENISE, false, Station.HOMOGENISE));
		assertFalse(StationMenuGuard.canUseStation(
			guidance, Station.CRYSTALLISE, false, Station.HOMOGENISE));
		assertFalse(StationMenuGuard.canUseStation(
			guidance, Station.CONCENTRATE, false, Station.HOMOGENISE));
	}

	@Test
	public void stationHoldingAPotionRemainsUsableForRecovery()
	{
		assertTrue(StationMenuGuard.canUseStation(
			Guidance.invalid("Wrong station"), Station.CONCENTRATE, true, null));
	}

	@Test
	public void stationObjectIdsAreRecognized()
	{
		for (Station station : Station.values())
		{
			assertSame(station, StationMenuGuard.stationForObjectId(
				station.getLabObject().getObjectId()));
		}
	}

	@Test
	public void menuSwapUsesTheHighestPriorityExactObject()
	{
		StationMenuGuard.MenuScan scan = new StationMenuGuard.MenuScan();
		int alembic = Station.CRYSTALLISE.getLabObject().getObjectId();
		int retort = Station.CONCENTRATE.getLabObject().getObjectId();
		scan.accept(0, alembic, 10, 20, MenuAction.GAME_OBJECT_FIRST_OPTION, "Crystallise");
		scan.accept(1, alembic, 10, 20, MenuAction.GAME_OBJECT_SECOND_OPTION, "Check");
		scan.accept(2, retort, 11, 20, MenuAction.GAME_OBJECT_SECOND_OPTION, "Check");
		scan.accept(3, retort, 11, 20, MenuAction.GAME_OBJECT_FIRST_OPTION, "Concentrate");

		StationMenuGuard.MenuSwap swap = scan.select();

		assertEquals(Station.CONCENTRATE, swap.getStation());
		assertEquals(3, swap.getProcessingIndex());
		assertEquals(2, swap.getCheckIndex());
	}

	@Test
	public void menuSwapNeverBorrowsCheckFromAnotherSceneObject()
	{
		StationMenuGuard.MenuScan scan = new StationMenuGuard.MenuScan();
		int agitator = Station.HOMOGENISE.getLabObject().getObjectId();
		scan.accept(0, agitator, 10, 20, MenuAction.GAME_OBJECT_SECOND_OPTION, "Check");
		scan.accept(1, agitator, 11, 20, MenuAction.GAME_OBJECT_FIRST_OPTION, "Homogenise");

		assertNull(scan.select());
	}
}

package com.mixologybatch;

import java.util.EnumMap;
import java.util.EnumSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BatchPlanTest
{
	@Test
	public void defaultPlanMatchesRequestedInventory()
	{
		BatchPlan plan = BatchPlan.defaultPlan();

		assertTrue(plan.isValid());
		assertEquals(28, plan.size());
		assertEquals(10, plan.get(0).getStationTotal());
		assertEquals(9, plan.get(10).getStationTotal());
		assertEquals(9, plan.get(19).getStationTotal());
		assertEquals(Station.CRYSTALLISE, plan.get(0).getStation());
		assertEquals(Station.HOMOGENISE, plan.get(10).getStation());
		assertEquals(Station.CONCENTRATE, plan.get(19).getStation());

		EnumMap<Potion, Integer> totals = new EnumMap<>(Potion.class);
		for (BatchEntry entry : plan.getEntries())
		{
			totals.merge(entry.getPotion(), 1, Integer::sum);
		}
		assertEquals(Integer.valueOf(3), totals.get(Potion.MMA));
		assertEquals(Integer.valueOf(4), totals.get(Potion.MML));
		assertEquals(Integer.valueOf(3), totals.get(Potion.AAM));
		assertEquals(Integer.valueOf(3), totals.get(Potion.ALA));
		assertEquals(Integer.valueOf(3), totals.get(Potion.LLL));
		assertEquals(Integer.valueOf(3), totals.get(Potion.MLL));
		assertEquals(Integer.valueOf(3), totals.get(Potion.ALL));
		assertEquals(Integer.valueOf(6), totals.get(Potion.MAL));
	}

	@Test
	public void threeCopiesCreateOneVariantPerStation()
	{
		EnumMap<Potion, Integer> counts = new EnumMap<>(Potion.class);
		counts.put(Potion.ALA, 3);
		BatchPlan plan = BatchPlan.create(counts, StationOrder.CRYSTALLISE_HOMOGENISE_CONCENTRATE);

		assertEquals(3, plan.size());
		assertEquals(EnumSet.allOf(Station.class), EnumSet.of(
			plan.get(0).getStation(), plan.get(1).getStation(), plan.get(2).getStation()));
	}

	@Test
	public void oversizedPlanHasActionableError()
	{
		EnumMap<Potion, Integer> counts = new EnumMap<>(Potion.class);
		counts.put(Potion.MAL, 29);
		BatchPlan plan = BatchPlan.create(counts, StationOrder.CRYSTALLISE_HOMOGENISE_CONCENTRATE);

		assertTrue(!plan.isValid());
		assertTrue(plan.getError().contains("29"));
		assertTrue(plan.getError().contains("28"));
	}
}

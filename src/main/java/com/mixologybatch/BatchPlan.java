package com.mixologybatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class BatchPlan
{
	static final int INVENTORY_SIZE = 28;

	private final List<BatchEntry> entries;
	private final String error;

	private BatchPlan(List<BatchEntry> entries, String error)
	{
		this.entries = entries;
		this.error = error;
	}

	static BatchPlan create(Map<Potion, Integer> counts, StationOrder stationOrder)
	{
		Station[] stations = stationOrder.getStations();
		List<List<Potion>> batches = new ArrayList<>(stations.length);
		for (int i = 0; i < stations.length; i++)
		{
			batches.add(new ArrayList<>());
		}

		int total = 0;
		for (Potion potion : Potion.values())
		{
			int count = counts.getOrDefault(potion, 0);
			if (count < 0)
			{
				return invalid("Potion counts cannot be negative.");
			}
			total += count;
			for (int copy = 0; copy < count; copy++)
			{
				batches.get(copy % stations.length).add(potion);
			}
		}

		if (total > INVENTORY_SIZE)
		{
			return invalid("Configured batch has " + total + " potions; reduce it to 28 or fewer.");
		}

		List<BatchEntry> result = new ArrayList<>(total);
		int slot = 0;
		for (int stationOrdinal = 0; stationOrdinal < stations.length; stationOrdinal++)
		{
			List<Potion> batch = batches.get(stationOrdinal);
			for (int position = 0; position < batch.size(); position++)
			{
				result.add(new BatchEntry(
					batch.get(position),
					stations[stationOrdinal],
					slot++,
					stationOrdinal,
					position,
					batch.size()));
			}
		}

		return new BatchPlan(Collections.unmodifiableList(result), null);
	}

    /** Balanced contiguous station groups, with carried potions reserved in their physical ranks. */
    static BatchPlan forInventory(Map<Potion, Integer> counts, StationOrder stationOrder, List<InventorySlot> inventory)
    {
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        if (total < 0 || total > INVENTORY_SIZE) { return invalid("Batch exceeds inventory capacity."); }
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) { if (inventory.get(i).isPotion()) { slots.add(i); } }
        for (int i = 0; i < inventory.size() && slots.size() < total; i++) { if (inventory.get(i).isEmpty()) { slots.add(i); } }
        if (slots.size() != total) { return invalid("Retained potions exceed the new batch capacity."); }
        Collections.sort(slots);
        int[][] assigned = new int[3][Potion.values().length];
        int[] capacities = {total / 3 + (total % 3 > 0 ? 1 : 0), total / 3 + (total % 3 > 1 ? 1 : 0), total / 3};
        int[] free = capacities.clone();
        int[] remaining = new int[Potion.values().length];
        for (Potion potion : Potion.values()) { remaining[potion.ordinal()] = counts.getOrDefault(potion, 0); }
        int station = 0, within = 0;
        for (int rank = 0; rank < slots.size(); rank++) {
            while (station < 2 && within >= capacities[station]) { within = 0; station++; }
            InventorySlot actual = inventory.get(slots.get(rank));
            if (actual.isPotion()) {
                assigned[station][actual.getPotion().ordinal()]++; remaining[actual.getPotion().ordinal()]--; free[station]--;
            }
            within++;
        }
        for (Potion potion : Potion.values()) {
            if (remaining[potion.ordinal()] < 0) { return invalid("Retained potion is missing from the target."); }
            for (int copy = 0; copy < remaining[potion.ordinal()]; copy++) {
                int best = -1;
                for (int i = 0; i < 3; i++) {
                    if (free[i] > 0 && (best < 0 || assigned[i][potion.ordinal()] < assigned[best][potion.ordinal()]
                        || (assigned[i][potion.ordinal()] == assigned[best][potion.ordinal()] && free[i] > free[best]))) { best = i; }
                }
                if (best < 0) { return invalid("The batch does not fit around retained potions."); }
                assigned[best][potion.ordinal()]++; free[best]--;
            }
        }
        List<BatchEntry> entries = new ArrayList<>();
        Station[] stations = stationOrder.getStations(); int slot = 0;
        for (int i = 0; i < 3; i++) {
            int position = 0;
            for (Potion potion : Potion.values()) {
                for (int copy = 0; copy < assigned[i][potion.ordinal()]; copy++) {
                    entries.add(new BatchEntry(potion, stations[i], slot++, i, position++, capacities[i]));
                }
            }
        }
        return new BatchPlan(Collections.unmodifiableList(entries), null);
    }

	static BatchPlan defaultPlan()
	{
		EnumMap<Potion, Integer> counts = new EnumMap<>(Potion.class);
		counts.put(Potion.MMA, 3);
		counts.put(Potion.MML, 4);
		counts.put(Potion.AAM, 3);
		counts.put(Potion.ALA, 3);
		counts.put(Potion.LLL, 3);
		counts.put(Potion.MLL, 3);
		counts.put(Potion.ALL, 3);
		counts.put(Potion.MAL, 6);
		return create(counts, StationOrder.CRYSTALLISE_HOMOGENISE_CONCENTRATE);
	}

	private static BatchPlan invalid(String error)
	{
		return new BatchPlan(Collections.emptyList(), error);
	}

	boolean isValid()
	{
		return error == null;
	}

	String getError()
	{
		return error;
	}

	int size()
	{
		return entries.size();
	}

	BatchEntry get(int slot)
	{
		return entries.get(slot);
	}

	List<BatchEntry> getEntries()
	{
		return entries;
	}

	int getConfiguredCount(Potion potion)
	{
		int count = 0;
		for (BatchEntry entry : entries)
		{
			if (entry.getPotion() == potion)
			{
				count++;
			}
		}
		return count;
	}

	int getConfiguredCount(Station station, Potion potion)
	{
		int count = 0;
		for (BatchEntry entry : entries)
		{
			if (entry.getStation() == station && entry.getPotion() == potion)
			{
				count++;
			}
		}
		return count;
	}
}

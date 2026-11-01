package com.mixologybatch;

import java.util.EnumMap;
import java.util.Map;

/** Persisted as stable enum-name:quantity pairs; all arithmetic uses long. */
final class RewardTargets
{
    static EnumMap<RewardItem, Integer> parse(String text) {
        EnumMap<RewardItem, Integer> result = new EnumMap<>(RewardItem.class);
        for (String entry : text.split(",")) {
            String[] pair = entry.split(":");
            if (pair.length != 2) { continue; }
            try {
                RewardItem item = RewardItem.valueOf(pair[0]);
                int quantity = Integer.parseInt(pair[1]);
                if (item != RewardItem.NONE && quantity > 0 && quantity <= 100000) { result.put(item, quantity); }
            } catch (IllegalArgumentException ignored) { /* Ignore obsolete/corrupt entries. */ }
        }
        return result;
    }
    static long[] costs(Map<RewardItem, Integer> targets) {
        long[] total = new long[3];
        targets.forEach((reward, quantity) -> {
            if (quantity < 0 || quantity > 100000) { throw new IllegalArgumentException("Invalid reward quantity"); }
            for (Component c : Component.values()) { total[c.ordinal()] += (long) quantity * reward.componentCost(c); }
        });
        return total;
    }
    static long[] deficits(long[] target, int[] resin) {
        long[] result = new long[3];
        for (int c = 0; c < 3; c++) { result[c] = Math.max(0, target[c] - resin[c]); }
        return result;
    }
    static String encode(Map<RewardItem, Integer> targets) {
        StringBuilder text = new StringBuilder();
        targets.forEach((reward, quantity) -> { if (quantity > 0) { text.append(reward.name()).append(':').append(quantity).append(','); } });
        return text.toString();
    }
}

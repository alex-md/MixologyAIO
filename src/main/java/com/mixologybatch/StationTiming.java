package com.mixologybatch;

import java.util.EnumMap;
import net.runelite.api.gameval.SpotanimID;

/** Actual game effect opens a window; progress updates, success and time close it. */
final class StationTiming
{
    private static final class State {
        int potion, progress, remainingUpdates;
        long expiresAt;
        boolean succeeded, effectSeen;
    }
    private final EnumMap<Station, State> states = new EnumMap<>(Station.class);
    StationTiming() { for (Station station : Station.values()) { states.put(station, new State()); } }

    static long now() { return System.nanoTime() / 1_000_000L; }
    void reset() { for (Station station : Station.values()) { setPotion(station, 0); } }
    void setPotion(Station station, int potion) {
        State state = states.get(station);
        if (state.potion != potion || potion == 0) {
            state.remainingUpdates = 0; state.expiresAt = 0; state.progress = 0; state.succeeded = false; state.effectSeen = false;
        }
        state.potion = potion;
    }
    boolean effect(int spotAnimation, long now) {
        Station station = stationForEffect(spotAnimation);
        if (station == null) { return false; }
        State state = states.get(station);
        if (state.potion == 0 || state.succeeded || isUrgent(station, now)) { return false; }
        state.remainingUpdates = station == Station.CRYSTALLISE ? 1 : 2;
        state.effectSeen = true;
        state.expiresAt = now + state.remainingUpdates * 600L;
        return true;
    }
    void progress(Station station, int value) {
        State state = states.get(station);
        if (value < state.progress) { state.remainingUpdates = 0; state.expiresAt = 0; state.effectSeen = false; }
        else if (value != state.progress && state.remainingUpdates > 0) { state.remainingUpdates--; }
        state.progress = value;
    }
    void success(Station station) { states.get(station).remainingUpdates = 0; states.get(station).expiresAt = 0; states.get(station).succeeded = true; }
    boolean isUrgent(Station station, long now) {
        State state = states.get(station);
        return state.potion != 0 && state.remainingUpdates > 0 && now < state.expiresAt;
    }
    String message(Station station, long now) {
        if (states.get(station).potion == 0) { return null; }
        if (station == Station.CONCENTRATE) { return "KEEP CLICKING"; }
        if (states.get(station).succeeded) { return "SPEEDUP APPLIED"; }
        return isUrgent(station, now) ? "CLICK NOW" : states.get(station).effectSeen ? "PROCESSING" : "WATCH FOR CUE";
    }
    static Station stationForEffect(int id) {
        if (id == SpotanimID.VFX_MACHINERY_ALCHEMY01_ALEMBIC01) { return Station.CRYSTALLISE; }
        if (id == SpotanimID.VFX_MACHINERY_ALCHEMY01_AGITATOR01) { return Station.HOMOGENISE; }
        return null;
    }
}

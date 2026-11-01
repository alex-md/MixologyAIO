package com.mixologybatch;

/** Learns the normal start-to-cue tick interval; never predicts the click window itself. */
final class AlembicCountdown
{
    private int startTick = -1, learnedTicks, lastProgress, pendingTicks;
    private long lastTickAt, startAt;
    private boolean running, cueSeen;

    void tick(long now) { lastTickAt = now; }
    void start(int tick, long now) {
        if (!running && !cueSeen) { startTick = tick; startAt = now; running = true; lastProgress = -1; }
    }
    void newPotion() { stop(); cueSeen = false; }
    void stop() { running = false; startTick = -1; }
    void reset() { newPotion(); learnedTicks = 0; pendingTicks = 0; lastTickAt = 0; }
    void progress(int value) {
        if (lastProgress >= 0 && value < lastProgress) { stop(); cueSeen = true; }
        lastProgress = value;
    }
    void effect(int tick) {
        if (running) {
            int elapsed = tick - startTick;
            if (elapsed >= 1 && elapsed <= 15) {
                // One normal observation enables the border. Require a repeated new
                // interval before changing calibration, so a delayed packet cannot jitter it.
                if (learnedTicks == 0 || learnedTicks == elapsed || pendingTicks == elapsed) { learnedTicks = elapsed; }
                pendingTicks = elapsed;
            }
        }
        stop(); cueSeen = true;
    }
    double fraction(int tick, long now) {
        if (!running || learnedTicks == 0 || lastTickAt == 0 || now < lastTickAt || now - lastTickAt > 1200) { return -1; }
        double ticks = (now - startAt) / 600.0;
        if (tick < startTick || ticks < 0) { return -1; }
        // The effect, not elapsed time, is the authority for green/ready.
        if (ticks > learnedTicks + 1) { return -1; }
        return Math.max(0, Math.min(.96, ticks / learnedTicks));
    }
}

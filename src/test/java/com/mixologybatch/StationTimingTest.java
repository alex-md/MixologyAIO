package com.mixologybatch;

import net.runelite.api.gameval.SpotanimID;
import org.junit.Test;
import static org.junit.Assert.*;

public class StationTimingTest
{
    private static final int ALEMBIC = SpotanimID.VFX_MACHINERY_ALCHEMY01_ALEMBIC01;
    private static final int AGITATOR = SpotanimID.VFX_MACHINERY_ALCHEMY01_AGITATOR01;
    @Test public void alembicWindowIsOneProgressUpdateAndNeverPersistsPastDeadline() {
        StationTiming timing = new StationTiming();
        assertFalse(timing.effect(ALEMBIC,1000));
        timing.setPotion(Station.CRYSTALLISE,1); timing.progress(Station.CRYSTALLISE,10);
        assertTrue(timing.effect(ALEMBIC,1000)); assertFalse(timing.effect(ALEMBIC,1001));
        assertEquals("CLICK NOW",timing.message(Station.CRYSTALLISE,1100));
        timing.progress(Station.CRYSTALLISE,11); assertFalse(timing.isUrgent(Station.CRYSTALLISE,1100));
        assertTrue(timing.effect(ALEMBIC,2000)); assertFalse(timing.isUrgent(Station.CRYSTALLISE,2600));
    }
    @Test public void agitatorWindowAllowsTwoUpdatesAndSuccessClearsItImmediately() {
        StationTiming timing = new StationTiming(); timing.setPotion(Station.HOMOGENISE,2);
        timing.effect(AGITATOR,1000); timing.progress(Station.HOMOGENISE,1);
        assertTrue(timing.isUrgent(Station.HOMOGENISE,1600));
        timing.progress(Station.HOMOGENISE,2); assertFalse(timing.isUrgent(Station.HOMOGENISE,1700));
        timing.effect(AGITATOR,2000); timing.success(Station.HOMOGENISE);
        assertFalse(timing.isUrgent(Station.HOMOGENISE,2001));
        assertFalse(timing.effect(AGITATOR,2002));
    }
    @Test public void interruptionsFailuresNewPotionsAndLogoutClearCues() {
        StationTiming timing = new StationTiming(); timing.setPotion(Station.CRYSTALLISE,1);
        timing.progress(Station.CRYSTALLISE,20); timing.effect(ALEMBIC,1000);
        timing.progress(Station.CRYSTALLISE,19); assertFalse(timing.isUrgent(Station.CRYSTALLISE,1001));
        timing.effect(ALEMBIC,2000); timing.setPotion(Station.CRYSTALLISE,2);
        assertFalse(timing.isUrgent(Station.CRYSTALLISE,2001));
        timing.effect(ALEMBIC,3000); timing.setPotion(Station.CRYSTALLISE,0);
        assertNull(timing.message(Station.CRYSTALLISE,3001));
        timing.setPotion(Station.CONCENTRATE,3); assertEquals("KEEP CLICKING",timing.message(Station.CONCENTRATE,4000));
        assertFalse(timing.effect(-1,4000)); timing.reset(); assertNull(timing.message(Station.CONCENTRATE,4001));
    }
}

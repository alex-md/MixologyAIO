package com.mixologybatch;

import java.lang.reflect.*;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.events.*;
import net.runelite.api.gameval.SpotanimID;
import org.junit.Test;
import static org.junit.Assert.*;

public class StationTimingEventsTest
{
    @Test public void actualEffectChimesOnceAndCompletionClearsCue() throws Exception {
        MixologyBatchPlugin plugin = new MixologyBatchPlugin(); int[] active = {1}, sounds = {0};
        Client client = proxy(Client.class,(p,m,a) -> {
            if (m.getName().equals("getVarbitValue")) { return active[0]; }
            if (m.getName().equals("playSoundEffect")) { sounds[0]++; return null; }
            throw new UnsupportedOperationException(m.getName());
        });
        inject(plugin,"client",client); inject(plugin,"config",new MixologyBatchConfig() {});
        GraphicsObject effect = proxy(GraphicsObject.class,(p,m,a) -> SpotanimID.VFX_MACHINERY_ALCHEMY01_ALEMBIC01);
        GraphicsObjectCreated event = new GraphicsObjectCreated(effect);
        plugin.onGraphicsObjectCreated(event); assertEquals(0,sounds[0]); // outside the lab
        inject(plugin,"inLab",true); plugin.onGraphicsObjectCreated(event); plugin.onGraphicsObjectCreated(event);
        assertEquals(1,sounds[0]); assertTrue(plugin.getStationTiming().isUrgent(Station.CRYSTALLISE,StationTiming.now()));
        SoundEffectPlayed delayed = new SoundEffectPlayed(null); delayed.setSoundId(2655); delayed.setDelay(1);
        plugin.onSoundEffectPlayed(delayed); assertTrue(delayed.isConsumed());
        VarbitChanged complete = new VarbitChanged(); complete.setVarbitId(Station.CRYSTALLISE.getPotionVarbit()); complete.setValue(0);
        plugin.onVarbitChanged(complete); assertNull(plugin.getTimingStation());
        active[0]=0; plugin.onGraphicsObjectCreated(event); assertEquals(1,sounds[0]);
    }
    @Test public void disablingTimingDoesNotSuppressTheGamesOriginalSound() throws Exception {
        MixologyBatchPlugin plugin = new MixologyBatchPlugin(); inject(plugin,"inLab",true);
        inject(plugin,"config",new MixologyBatchConfig() { public boolean stationTimingCues() { return false; } });
        SoundEffectPlayed original = new SoundEffectPlayed(null); original.setSoundId(2655); original.setDelay(1);
        plugin.onSoundEffectPlayed(original); assertFalse(original.isConsumed());
    }
    private static void inject(Object target,String name,Object value)throws Exception {Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    @SuppressWarnings("unchecked") private static <T>T proxy(Class<T>type,InvocationHandler h){return(T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},h);}
}

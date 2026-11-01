package com.mixologybatch;

import org.junit.Test;
import static org.junit.Assert.*;

public class AlembicCountdownTest
{
    private AlembicCountdown calibrated() {
        AlembicCountdown timer=new AlembicCountdown();timer.newPotion();timer.tick(1000);timer.start(0,1000);timer.effect(4);return timer;
    }
    @Test public void firstObservedCueCalibratesThenTheBorderFillsSmoothly() {
        AlembicCountdown timer=new AlembicCountdown();timer.tick(1000);timer.start(0,1000);
        assertEquals(-1,timer.fraction(2,1200),0); // No arbitrary initial countdown.
        timer.effect(4); timer.newPotion();timer.start(10,10000);timer.tick(10600);
        assertEquals(.375,timer.fraction(11,10900),1e-9);
        timer.tick(12400);assertEquals(.96,timer.fraction(14,12400),1e-9); // Never signals ready from a prediction.
        timer.effect(14); assertEquals(-1,timer.fraction(14,12400),0);
        timer.start(15,15000); timer.tick(15000); assertEquals(-1,timer.fraction(15,15000),0); // No restart after the cue.
    }
    @Test public void interruptionsFailuresAndStalledPacketsHideThePrediction() {
        AlembicCountdown timer=calibrated();timer.newPotion();timer.start(10,10000);timer.tick(10000);
        assertEquals(-1,timer.fraction(11,12001),0);
        timer.tick(12000);timer.progress(20);timer.progress(19);
        assertEquals(-1,timer.fraction(11,12000),0);
        timer.start(11,12000);assertEquals(-1,timer.fraction(11,12000),0);
        timer.start(11,11000);timer.stop();assertEquals(-1,timer.fraction(12,12000),0);
        timer.reset();timer.start(12,12000);timer.tick(12000);assertEquals(-1,timer.fraction(13,12000),0);
    }
    @Test public void oneDelayedSampleCannotChangeTheLearnedInterval() {
        AlembicCountdown timer=calibrated();timer.newPotion();timer.start(10,10000);timer.effect(15);
        timer.newPotion();timer.start(20,20000);timer.tick(21200);
        assertEquals(.5,timer.fraction(22,21200),1e-9);
        timer.effect(25);timer.newPotion();timer.start(30,30000);timer.tick(31200);
        assertEquals(.4,timer.fraction(32,31200),1e-9);
    }
}

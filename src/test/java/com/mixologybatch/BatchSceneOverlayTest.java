package com.mixologybatch;

import org.junit.Test;
import static org.junit.Assert.*;

public class BatchSceneOverlayTest
{
    private static Guidance mixing(Potion potion, int... slots) {
        return Guidance.mixing(new BatchEntry(potion, Station.CRYSTALLISE, 0, 0, 0, 1), MixStep.resolve(potion, slots, false));
    }
    @Test public void onlyTheNextObjectIsSelectedIncludingRepeatedIngredients() {
        assertEquals(LabObject.AGA_LEVER, BatchSceneOverlay.mixingTarget(mixing(Potion.ALA,0,0,0)));
        assertEquals(LabObject.LYE_LEVER, BatchSceneOverlay.mixingTarget(mixing(Potion.ALA,2,0,0)));
        assertEquals(LabObject.AGA_LEVER, BatchSceneOverlay.mixingTarget(mixing(Potion.ALA,2,3,0)));
        assertEquals(LabObject.MIXING_VESSEL, BatchSceneOverlay.mixingTarget(mixing(Potion.ALA,2,3,2)));
    }
    @Test public void actualContentsDriveHighlightAndNoLeverIsSelectedWhileProcessing() {
        assertEquals(LabObject.MOX_LEVER, BatchSceneOverlay.mixingTarget(mixing(Potion.MAL,3,0,0)));
        assertNull(BatchSceneOverlay.mixingTarget(Guidance.complete()));
        assertEquals("Pull MOX", MixologyPanel.describe(mixing(Potion.MAL,0,0,0)));
    }
}

package com.mixologybatch;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MixStepTest
{
	@Test
	public void walksCanonicalRecipeThenMixes()
	{
		assertEquals("M  A  L", Potion.MAL.getRecipeSequence());

		MixStep first = MixStep.resolve(Potion.MAL, new int[]{0, 0, 0}, false);
		MixStep second = MixStep.resolve(Potion.MAL, new int[]{1, 0, 0}, false);
		MixStep third = MixStep.resolve(Potion.MAL, new int[]{1, 2, 0}, false);
		MixStep mix = MixStep.resolve(Potion.MAL, new int[]{1, 2, 3}, false);

		assertEquals(Component.MOX, first.getComponent());
		assertEquals(1, first.getNumber());
		assertEquals(Component.AGA, second.getComponent());
		assertEquals(2, second.getNumber());
		assertEquals(Component.LYE, third.getComponent());
		assertEquals(3, third.getNumber());
		assertEquals(MixStep.Kind.MIX_VESSEL, mix.getKind());
		assertEquals(4, mix.getNumber());
	}

	@Test
	public void repeatedIngredientsAndRecoveryAreHandled()
	{
		MixStep canonical = MixStep.resolve(Potion.MMA, new int[]{1, 0, 0}, false);
		MixStep outOfOrder = MixStep.resolve(Potion.MMA, new int[]{2, 0, 0}, false);

		assertEquals(Component.MOX, canonical.getComponent());
		assertEquals(Component.MOX, outOfOrder.getComponent());
	}

	@Test
	public void wrongIngredientIsRejected()
	{
		MixStep step = MixStep.resolve(Potion.MMM, new int[]{2, 0, 0}, false);

		assertEquals(MixStep.Kind.INVALID, step.getKind());
		assertTrue(step.getError().contains("MMM"));
	}
}

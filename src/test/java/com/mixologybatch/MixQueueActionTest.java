package com.mixologybatch;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MixQueueActionTest
{
	@Test
	public void formatsLeverAndMixActions()
	{
		assertEquals("1 M", MixQueueAction.forStep(Potion.MAL, 1).getInstruction());
		assertEquals("2 A", MixQueueAction.forStep(Potion.MAL, 2).getInstruction());
		assertEquals("3 L", MixQueueAction.forStep(Potion.MAL, 3).getInstruction());
		assertEquals("4 MIX", MixQueueAction.forStep(Potion.MAL, 4).getInstruction());
	}
}

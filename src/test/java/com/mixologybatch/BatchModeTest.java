package com.mixologybatch;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BatchModeTest
{
	@Test
	public void emptyInventoryStartsRefilling()
	{
		assertEquals(BatchMode.REFILLING, BatchMode.initialize(0, 0, 28, false, false));
	}

	@Test
	public void mixerActivityResumesRefilling()
	{
		assertEquals(BatchMode.REFILLING, BatchMode.initialize(12, 0, 28, true, true));
	}

	@Test
	public void unfinishedPartialInventoryCanBeFinishedEarly()
	{
		assertEquals(BatchMode.FINISHING_PARTIAL,
			BatchMode.initialize(5, 0, 28, false, true));
	}

	@Test
	public void activeStationSurvivesPluginReloadAsPartialProcessing()
	{
		assertEquals(BatchMode.FINISHING_PARTIAL,
			BatchMode.initialize(0, 1, 28, false, false));
	}

	@Test
	public void fullBatchIncludingAnActiveStationKeepsProcessing()
	{
		assertEquals(BatchMode.PROCESSING,
			BatchMode.initialize(27, 1, 28, false, true));
	}

	@Test
	public void processedPartialInventoryDoesNotRestartMixingOnReload()
	{
		assertEquals(BatchMode.PROCESSING,
			BatchMode.initialize(5, 0, 28, false, false));
	}
}

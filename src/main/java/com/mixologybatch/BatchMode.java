package com.mixologybatch;

enum BatchMode
{
	REFILLING,
	PROCESSING,
	FINISHING_PARTIAL;

	boolean isRefilling()
	{
		return this == REFILLING;
	}

	boolean allowsRollingRefill()
	{
		return this == PROCESSING;
	}

	static BatchMode initialize(
		int inventoryPotionCount,
		int activeStationCount,
		int potionCapacity,
		boolean mixingActivity,
		boolean hasUnfinishedInventoryPotion)
	{
		int accountedPotionCount = inventoryPotionCount + activeStationCount;
		if (accountedPotionCount == 0 || mixingActivity)
		{
			return REFILLING;
		}
		if (accountedPotionCount < potionCapacity
			&& (hasUnfinishedInventoryPotion || activeStationCount > 0))
		{
			return FINISHING_PARTIAL;
		}
		return PROCESSING;
	}
}

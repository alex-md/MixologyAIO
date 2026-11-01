package com.mixologybatch;

final class InventorySlot
{
	private final boolean empty;
	private final int itemId;
	private final Potion potion;
	private final boolean finished;

	private InventorySlot(boolean empty, int itemId, Potion potion, boolean finished)
	{
		this.empty = empty;
		this.itemId = itemId;
		this.potion = potion;
		this.finished = finished;
	}

	static InventorySlot empty()
	{
		return new InventorySlot(true, -1, null, false);
	}

	static InventorySlot fromItemId(int itemId)
	{
		if (itemId <= 0)
		{
			return empty();
		}
		Potion potion = Potion.fromItemId(itemId);
		return new InventorySlot(false, itemId, potion, potion != null && potion.isFinishedItem(itemId));
	}

	boolean isEmpty()
	{
		return empty;
	}

	boolean isPotion()
	{
		return potion != null;
	}

	int getItemId()
	{
		return itemId;
	}

	Potion getPotion()
	{
		return potion;
	}

	boolean isFinished()
	{
		return finished;
	}
}

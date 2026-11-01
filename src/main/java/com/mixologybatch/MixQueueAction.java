package com.mixologybatch;

final class MixQueueAction
{
	private final Potion potion;
	private final int step;
	private final Component component;

	private MixQueueAction(Potion potion, int step, Component component)
	{
		this.potion = potion;
		this.step = step;
		this.component = component;
	}

	static MixQueueAction forStep(Potion potion, int step)
	{
		Component component = step >= 1 && step <= 3 ? potion.getRecipe()[step - 1] : null;
		return new MixQueueAction(potion, step, component);
	}

	Potion getPotion()
	{
		return potion;
	}

	int getStep()
	{
		return step;
	}

	Component getComponent()
	{
		return component;
	}

	String getInstruction()
	{
		return step == 4 ? "4 MIX" : step + " " + component.getCode();
	}
}

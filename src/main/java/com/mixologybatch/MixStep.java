package com.mixologybatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class MixStep
{
	enum Kind
	{
		PULL_LEVER,
		MIX_VESSEL,
		INVALID
	}

	private final Kind kind;
	private final int number;
	private final Component component;
	private final String error;

	private MixStep(Kind kind, int number, Component component, String error)
	{
		this.kind = kind;
		this.number = number;
		this.component = component;
		this.error = error;
	}

	static MixStep resolve(Potion potion, int[] mixerSlots, boolean vesselReady)
	{
		if (vesselReady)
		{
			return new MixStep(Kind.MIX_VESSEL, 4, null, null);
		}

		List<Component> remaining = new ArrayList<>(Arrays.asList(potion.getRecipe()));
		int filled = 0;
		boolean sawEmpty = false;
		for (int value : mixerSlots)
		{
			if (value == 0)
			{
				sawEmpty = true;
				continue;
			}
			if (sawEmpty)
			{
				return invalid("Mixer slots are not contiguous; empty the vessel and restart this potion.");
			}
			Component component = Component.fromMixerValue(value);
			if (component == null || !remaining.remove(component))
			{
				return invalid("Mixer contents do not match " + potion.name() + ".");
			}
			filled++;
		}

		if (filled == 3)
		{
			return new MixStep(Kind.MIX_VESSEL, 4, null, null);
		}

		Component next = remaining.get(0);
		return new MixStep(Kind.PULL_LEVER, filled + 1, next, null);
	}

	private static MixStep invalid(String error)
	{
		return new MixStep(Kind.INVALID, 0, null, error);
	}

	Kind getKind()
	{
		return kind;
	}

	int getNumber()
	{
		return number;
	}

	Component getComponent()
	{
		return component;
	}

	String getError()
	{
		return error;
	}
}

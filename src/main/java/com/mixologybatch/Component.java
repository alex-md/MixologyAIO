package com.mixologybatch;

import java.awt.Color;

enum Component
{
	MOX('M', 1, new Color(3, 169, 244), LabObject.MOX_LEVER),
	AGA('A', 2, new Color(0, 230, 118), LabObject.AGA_LEVER),
	LYE('L', 3, new Color(233, 30, 99), LabObject.LYE_LEVER);

	private final char code;
	private final int mixerValue;
	private final Color color;
	private final LabObject lever;

	Component(char code, int mixerValue, Color color, LabObject lever)
	{
		this.code = code;
		this.mixerValue = mixerValue;
		this.color = color;
		this.lever = lever;
	}

	char getCode()
	{
		return code;
	}

	int getMixerValue()
	{
		return mixerValue;
	}

	Color getColor()
	{
		return color;
	}

	LabObject getLever()
	{
		return lever;
	}

	static Component fromMixerValue(int value)
	{
		for (Component component : values())
		{
			if (component.mixerValue == value)
			{
				return component;
			}
		}
		return null;
	}
}

package com.mixologybatch;

import net.runelite.api.gameval.VarbitID;

enum Station
{
	CRYSTALLISE("Alembic", "Crystallise", "CRY", LabObject.ALEMBIC, VarbitID.MM_LAB_ALEMBIC_POTION),
	HOMOGENISE("Agitator", "Homogenise", "HOM", LabObject.AGITATOR, VarbitID.MM_LAB_AGITATOR_POTION),
	CONCENTRATE("Retort", "Concentrate", "CON", LabObject.RETORT, VarbitID.MM_LAB_RETORT_POTION);

	private final String objectName;
	private final String actionName;
	private final String shortName;
	private final LabObject labObject;
	private final int potionVarbit;

	Station(String objectName, String actionName, String shortName, LabObject labObject, int potionVarbit)
	{
		this.objectName = objectName;
		this.actionName = actionName;
		this.shortName = shortName;
		this.labObject = labObject;
		this.potionVarbit = potionVarbit;
	}

	String getObjectName()
	{
		return objectName;
	}

	String getActionName()
	{
		return actionName;
	}

	String getShortName()
	{
		return shortName;
	}

	LabObject getLabObject()
	{
		return labObject;
	}

	int getPotionVarbit()
	{
		return potionVarbit;
	}
}

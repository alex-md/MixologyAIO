package com.mixologybatch;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

import static com.mixologybatch.Component.AGA;
import static com.mixologybatch.Component.LYE;
import static com.mixologybatch.Component.MOX;

/**
 * Potion order matches the game's potion-type varbit values (ordinal + 1).
 * Recipe codes use the actual guided lever order, hence ALA/MLL/ALL rather
 * than the internal AAL/LLM/LLA item names.
 */
enum Potion
{
	MMM("Mammoth-might mix", ItemID.MM_POTION_MMM_UNFINISHED, ItemID.MM_POTION_MMM_FINISHED, MOX, MOX, MOX),
	MMA("Mystic mana amalgam", ItemID.MM_POTION_MMA_UNFINISHED, ItemID.MM_POTION_MMA_FINISHED, MOX, MOX, AGA),
	MML("Marley's moonlight", ItemID.MM_POTION_MML_UNFINISHED, ItemID.MM_POTION_MML_FINISHED, MOX, MOX, LYE),
	AAA("Alco-augmentator", ItemID.MM_POTION_AAA_UNFINISHED, ItemID.MM_POTION_AAA_FINISHED, AGA, AGA, AGA),
	AAM("Azure aura mix", ItemID.MM_POTION_AAM_UNFINISHED, ItemID.MM_POTION_AAM_FINISHED, AGA, AGA, MOX),
	ALA("Aqualux amalgam", ItemID.MM_POTION_AAL_UNFINISHED, ItemID.MM_POTION_AAL_FINISHED, AGA, LYE, AGA),
	LLL("Liplack liquor", ItemID.MM_POTION_LLL_UNFINISHED, ItemID.MM_POTION_LLL_FINISHED, LYE, LYE, LYE),
	MLL("Megalite liquid", ItemID.MM_POTION_LLM_UNFINISHED, ItemID.MM_POTION_LLM_FINISHED, MOX, LYE, LYE),
	ALL("Anti-leech lotion", ItemID.MM_POTION_LLA_UNFINISHED, ItemID.MM_POTION_LLA_FINISHED, AGA, LYE, LYE),
	MAL("Mixalot", ItemID.MM_POTION_MAL_UNFINISHED, ItemID.MM_POTION_MAL_FINISHED, MOX, AGA, LYE);

	private static final Map<Integer, Potion> ITEM_LOOKUP;

	static
	{
		Map<Integer, Potion> lookup = new HashMap<>();
		for (Potion potion : values())
		{
			lookup.put(potion.unfinishedItemId, potion);
			lookup.put(potion.finishedItemId, potion);
		}
		ITEM_LOOKUP = Collections.unmodifiableMap(lookup);
	}

	private final String displayName;
	private final int unfinishedItemId;
	private final int finishedItemId;
	private final Component[] recipe;

	Potion(String displayName, int unfinishedItemId, int finishedItemId, Component... recipe)
	{
		this.displayName = displayName;
		this.unfinishedItemId = unfinishedItemId;
		this.finishedItemId = finishedItemId;
		this.recipe = recipe;
	}

	String getDisplayName()
	{
		return displayName;
	}

	int getUnfinishedItemId()
	{
		return unfinishedItemId;
	}

	int getFinishedItemId()
	{
		return finishedItemId;
	}

	Component[] getRecipe()
	{
		return recipe.clone();
	}

	String getRecipeSequence()
	{
		return recipe[0].getCode()
			+ "  " + recipe[1].getCode()
			+ "  " + recipe[2].getCode();
	}

	boolean isFinishedItem(int itemId)
	{
		return itemId == finishedItemId;
	}

	int level() { return new int[]{60, 63, 66, 60, 69, 72, 60, 75, 78, 81}[ordinal()]; }
	int xp() { return new int[]{190, 215, 240, 190, 265, 290, 190, 315, 340, 365}[ordinal()]; }
	int weight() { return this == MAL ? 3 : (this == MMM || this == AAA || this == LLL) ? 5 : 4; }
	int paste(int color) {
		int count = 0;
		for (Component c : recipe) { if (c.ordinal() == color) { count++; } }
		return count * 10;
	}
	int resin(int color) {
		int count = paste(color) / 10;
		if (count == 0) { return 0; }
		return this == MAL || count >= 2 ? 20 : 10;
	}

	static Potion fromItemId(int itemId)
	{
		return ITEM_LOOKUP.get(itemId);
	}

	static Potion fromVarbit(int value)
	{
		int index = value - 1;
		return index >= 0 && index < values().length ? values()[index] : null;
	}
}

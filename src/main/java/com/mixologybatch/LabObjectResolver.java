package com.mixologybatch;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

@Singleton
final class LabObjectResolver
{
	private final Client client;

	@Inject
	private LabObjectResolver(Client client)
	{
		this.client = client;
	}

	TileObject find(LabObject target)
	{
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return null;
		}
		LocalPoint localPoint = LocalPoint.fromWorld(worldView, target.getLocation());
		if (localPoint == null)
		{
			return null;
		}
		Tile tile = worldView.getScene().getTiles()[worldView.getPlane()][localPoint.getSceneX()][localPoint.getSceneY()];
		if (tile == null)
		{
			return null;
		}

		for (TileObject object : tile.getGameObjects())
		{
			if (matches(object, target))
			{
				return object;
			}
		}
		if (matches(tile.getDecorativeObject(), target))
		{
			return tile.getDecorativeObject();
		}
		if (matches(tile.getWallObject(), target))
		{
			return tile.getWallObject();
		}
		if (matches(tile.getGroundObject(), target))
		{
			return tile.getGroundObject();
		}
		return null;
	}

	private static boolean matches(TileObject object, LabObject target)
	{
		return object != null && object.getId() == target.getObjectId();
	}
}

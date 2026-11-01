package com.mixologybatch;

public enum StationOrder
{
	CRYSTALLISE_HOMOGENISE_CONCENTRATE(
		"Crystallise > Homogenise > Concentrate",
		Station.CRYSTALLISE, Station.HOMOGENISE, Station.CONCENTRATE),
	HOMOGENISE_CRYSTALLISE_CONCENTRATE(
		"Homogenise > Crystallise > Concentrate",
		Station.HOMOGENISE, Station.CRYSTALLISE, Station.CONCENTRATE),
	CRYSTALLISE_CONCENTRATE_HOMOGENISE(
		"Crystallise > Concentrate > Homogenise",
		Station.CRYSTALLISE, Station.CONCENTRATE, Station.HOMOGENISE),
	HOMOGENISE_CONCENTRATE_CRYSTALLISE(
		"Homogenise > Concentrate > Crystallise",
		Station.HOMOGENISE, Station.CONCENTRATE, Station.CRYSTALLISE),
	CONCENTRATE_CRYSTALLISE_HOMOGENISE(
		"Concentrate > Crystallise > Homogenise",
		Station.CONCENTRATE, Station.CRYSTALLISE, Station.HOMOGENISE),
	CONCENTRATE_HOMOGENISE_CRYSTALLISE(
		"Concentrate > Homogenise > Crystallise",
		Station.CONCENTRATE, Station.HOMOGENISE, Station.CRYSTALLISE);

	private final String displayName;
	private final Station[] stations;

	StationOrder(String displayName, Station... stations)
	{
		this.displayName = displayName;
		this.stations = stations;
	}

	Station[] getStations()
	{
		return stations.clone();
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}

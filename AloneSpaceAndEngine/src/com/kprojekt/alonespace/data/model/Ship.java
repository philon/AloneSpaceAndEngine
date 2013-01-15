package com.kprojekt.alonespace.data.model;

import java.util.List;

import android.graphics.drawable.Drawable;

/**
 * @author Krzysiek Bobnis
 * @since 16:13:13 06-01-2013
 */
public class Ship
{
	private final String id;
	private final transient String name;
	private final transient String desc;
	private final transient Drawable img;
	private final List<ShipPart> parts;

	public Ship( String id, String name, String desc, Drawable img, List<ShipPart> collection )
	{
		this.id = id;
		this.name = name;
		this.desc = desc;
		this.img = img;
		this.parts = collection;
	}

	public Ship( Ship copy )
	{
		this.id = copy.id;
		this.name = copy.name;
		this.desc = copy.desc;
		this.img = copy.img;
		this.parts = copy.parts;
	}
}
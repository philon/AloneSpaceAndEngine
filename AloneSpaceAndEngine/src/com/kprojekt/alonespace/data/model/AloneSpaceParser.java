package com.kprojekt.alonespace.data.model;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;

import com.kprojekt.alonespace.utils.AssetHelper;
import com.kprojekt.alonespace.utils.Utils;
import com.kprojekt.alonespace.utils.XMLHelper;
import com.kprojekt.alonespace.utils.XMLLoadException;

public class AloneSpaceParser
{

	public static Node getNodeFromInputStream( InputStream is, String nodeName )
	{
		DocumentBuilder docBuilder;
		Document doc;
		try
		{
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			doc = docBuilder.parse( is, null );
		}
		catch( Exception e )
		{
			throw new RuntimeException( e );
		}

		return doc.getElementsByTagName( nodeName ).item( 0 );
	}

	public static HashMap<String, HashMap<String, ShipPart>> parseShipPart2( Node tagShipPartCategories, AssetManager assetManager, HashMap<String, ActionTemplate> actions, HashMap<String, ShipPartCategory> shipPartCats )
	{
		Node dShipPartCategory = XMLHelper.getChildrenOfName( tagShipPartCategories, "default-ship-part-category" ).get(
				0 );
		Node dShipPart = XMLHelper.getChildrenOfName( dShipPartCategory, "default-ship-part" ).get( 0 );
		String dName = XMLHelper.getAttrOfName( dShipPart, "name" );
		String dDesc = XMLHelper.getAttrOfName( dShipPart, "desc" );
		String dImg = XMLHelper.getAttrOfName( dShipPart, "img" );
		String dImgDefault = XMLHelper.getAttrOfName( dShipPart, "imgDefault" );

		Map<String, Action> actionList = AloneSpaceParser.parseActions( XMLHelper.getChildrenOfName( dShipPart,
				"action" ), actions );

		HashMap<String, HashMap<String, ShipPart>> shipParts = new HashMap<String, HashMap<String, ShipPart>>();
		for( Node tagShipParCategory : XMLHelper.getChildrenOfName( tagShipPartCategories, "ship-part-category" ) )
		{
			String catId = XMLHelper.getAttrOfName( tagShipParCategory, "id" );
			if( shipPartCats.get( catId ) == null )
			{
				throw new XMLLoadException( "There is no category with id " + catId );
			}
			shipParts.put( catId, new HashMap<String, ShipPart>() );

			for( Node tagShipPart : XMLHelper.getChildrenOfName( tagShipParCategory, "ship-part" ) )
			{
				String id = XMLHelper.getAttrOfName( tagShipPart, "id" );

				Map<String, Action> tmpActions = new HashMap<String, Action>( actionList );
				tmpActions.putAll( AloneSpaceParser.parseActions( XMLHelper.getChildrenOfName( tagShipPart, "action" ),
						actions ) );

				String imgPath = dImg.replace( "{catId}", catId ).replace( "{id}", id );
				Drawable loadImage = AssetHelper.loadImage( imgPath, assetManager );
				if( loadImage == null )
				{
					imgPath = dImgDefault;
					loadImage = AssetHelper.loadImage( imgPath, assetManager );
				}
				else
				{
					System.out.println( "nie jest nullem: " + imgPath );
				}

				ShipPart tmp = new ShipPart( id, dName.replace( "{catId}", catId ).replace( "{id}", id ),
						dDesc.replace( "{catId}", catId ).replace( "{id}", id ), loadImage, tmpActions.values(),
						shipPartCats.get( catId ), imgPath );

				HashMap<String, ShipPart> hashMap = shipParts.get( catId );
				hashMap.put( id, tmp );
			}
		}
		return shipParts;

	}

	public static HashMap<String, Ship> parseShips( Node tagShips, HashMap<String, HashMap<String, ShipPart>> shipParts, AssetManager assetManager, HashMap<String, ShipPartCategory> shipPartCategories )
	{
		Node dShip = XMLHelper.getChildrenOfName( tagShips, "default-ship" ).get( 0 );
		String dName = XMLHelper.getAttrOfName( dShip, "name" );
		String dDesc = XMLHelper.getAttrOfName( dShip, "desc" );
		String dImg = XMLHelper.getAttrOfName( dShip, "img" );

		TreeSet<String> shipIds = new TreeSet<String>();
		HashMap<String, Ship> ships = new HashMap<String, Ship>();

		for( Node tmp : XMLHelper.getChildrenOfName( tagShips, "ship" ) )
		{
			String id = XMLHelper.getAttrOfName( tmp, "id" );
			if( !shipIds.add( id ) )
			{
				throw new XMLLoadException( "Ship with this id " + id + "was declared in before" );
			}

			List<ShipPart> parts = new ArrayList<ShipPart>();
			TreeSet<String> partCategories = new TreeSet<String>();

			for( Node tagPart : XMLHelper.getChildrenOfName( tmp, "part" ) )
			{
				String partId = XMLHelper.getAttrOfName( tagPart, "id" );
				String categoryId = XMLHelper.getAttrOfName( tagPart, "category-id" );
				if( !partCategories.add( categoryId ) )
				{
					throw new XMLLoadException(
							"Ship from model can have only one part of each category. Duplicate category: "
									+ categoryId + " in ship " + id );
				}
				HashMap<String, ShipPart> category = shipParts.get( categoryId );
				if( category == null )
				{
					throw new RuntimeException( "THere is no ship part of category " + categoryId );
				}
				ShipPart shipPart = category.get( partId );
				if( shipPart == null )
				{
					throw new RuntimeException( "There is no ship part " + shipPart + " in category " + categoryId );
				}
				parts.add( shipPart );
			}

			//checking if ship contains all the obligatory categories
			for( ShipPartCategory category : shipPartCategories.values() )
			{
				if( category.isObligatory() )
				{
					boolean found = false;
					for( ShipPart tmp2 : parts )
					{
						if( tmp2.getCategory().getId() == category.getId() )
						{
							found = true;
							break;
						}
					}
					if( !found )
					{
						throw new XMLLoadException( "Ship " + id + " doesnt have obligatory ship part of category: "
								+ category.getId() );
					}
				}
			}

			ships.put( id, new Ship( id, dName.replace( "{id}", id ), dDesc.replace( "{id}", id ),
					AssetHelper.loadImage( dImg, assetManager ), parts ) );
		}
		return ships;
	}

	public static HashMap<String, ShipPartCategory> parseShipCategories( Node tagShipPartCategory, AssetManager assetManager, HashMap<String, ActionTemplate> actions )
	{
		Node dShipPartCat = XMLHelper.getChildrenOfName( tagShipPartCategory, "default-ship-part-category" ).get( 0 );
		String dCatName = XMLHelper.getAttrOfName( dShipPartCat, "name" );
		String dCatDesc = XMLHelper.getAttrOfName( dShipPartCat, "desc" );
		String dCatImg = XMLHelper.getAttrOfName( dShipPartCat, "img" );
		Boolean dObligatory = Boolean.parseBoolean( XMLHelper.getAttrOfName( dShipPartCat, "obligatory" ) );

		TreeSet<String> usedPartIds = new TreeSet<String>();
		HashMap<String, ShipPartCategory> shipPartCategories = new HashMap<String, ShipPartCategory>();
		for( Node shipPartCategoryTmp : XMLHelper.getChildrenOfName( tagShipPartCategory, "ship-part-category" ) )
		{
			String id = XMLHelper.getAttrOfName( shipPartCategoryTmp, "id" );
			if( !usedPartIds.add( id ) )
			{
				throw new XMLLoadException( "There is already a ship part category with id " + id );
			}
			String name = Utils.safeGet( XMLHelper.getAttrOfName( shipPartCategoryTmp, "name" ), dCatName );
			String desc = Utils.safeGet( XMLHelper.getAttrOfName( shipPartCategoryTmp, "desc" ), dCatDesc );
			String img = Utils.safeGet( XMLHelper.getAttrOfName( shipPartCategoryTmp, "img" ), dCatImg );
			String tmp = XMLHelper.getAttrOfName( shipPartCategoryTmp, "obligatory" );
			boolean obligatory = tmp == null ? dObligatory : Utils.safeGet( Boolean.parseBoolean( tmp ), dObligatory );

			ShipPartCategory shipPartCategory = new ShipPartCategory( id, name.replace( "{catId}", id ), desc.replace(
					"{catId}", id ), AssetHelper.loadImage( img, assetManager ), obligatory );

			shipPartCategories.put( id, shipPartCategory );
		}
		return shipPartCategories;
	}

	private static Map<String, Action> parseActions( List<Node> tagActions, HashMap<String, ActionTemplate> actionTemplates )
	{
		Map<String, Action> res = new HashMap<String, Action>();
		for( Node dAction : tagActions )
		{
			String id = XMLHelper.getAttrOfName( dAction, "id" );
			if( actionTemplates.get( id ) == null )
			{
				throw new RuntimeException( "Action of id " + id + " has wrong id. Please add this id to actions tag" );
			}
			float value = Float.parseFloat( XMLHelper.getAttrOfName( dAction, "value" ) );
			res.put( id, new Action( actionTemplates.get( id ), value ) );
		}
		return res;
	}

	public static HashMap<String, ActionTemplate> parseActionTemplates( Node tagActions, AssetManager assetManager )
	{
		Node defaultAction = XMLHelper.getChildrenOfName( tagActions, "default-action-template" ).get( 0 );
		String dImg = XMLHelper.getAttrOfName( defaultAction, "img" );
		String dName = XMLHelper.getAttrOfName( defaultAction, "name" );
		String dDesc = XMLHelper.getAttrOfName( defaultAction, "desc" );

		List<Node> actions = XMLHelper.getChildrenOfName( tagActions, "action-template" );

		HashMap<String, ActionTemplate> actionList = new HashMap<String, ActionTemplate>();
		for( Node action : actions )
		{
			String id = XMLHelper.getAttrOfName( action, "id" );
			ActionTemplate.Type type = ActionTemplate.Type.valueOf( id );
			actionList.put( id, new ActionTemplate( type, AssetHelper.loadImage( dImg, assetManager ), dName, dDesc ) );
		}
		return actionList;
	}

	public static Ship parseDefaultShip( Node tagShips, HashMap<String, Ship> parseShips )
	{
		String startingShipId = XMLHelper.getAttrOfName( tagShips, "starting" );
		return parseShips.get( startingShipId );
	}
}
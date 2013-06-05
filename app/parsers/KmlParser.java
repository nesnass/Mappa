package parsers;

import helpers.MyConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.List;

import models.Feature;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.IconStyle;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;

public class KmlParser {

	public static boolean getKmlForSession(String session_id, File kmlFile) {
		
		List<Feature> featureList = Feature.find.fetch("featureSession").where().eq("featureSession.facebook_group_id", session_id).findList();
		Iterator<Feature> it = featureList.iterator();
		Feature f;
		
		final Kml kml = new Kml();
		
		while(it.hasNext()) {
			f = it.next();
			kml.createAndSetPlacemark()
				.withDescription(f.properties.description)
				.withName(f.properties.source_type).withOpen(Boolean.TRUE)
				.createAndSetPoint().addToCoordinates(f.geometry.getLng(), f.geometry.getLat());
		}
		try {
			if( kml.marshal(kmlFile) )
				return true;
			else
				return false;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	
	
	public static boolean getKmlForUser(String user_id, File kmlFile) {
		
		List<Feature> featureList = Feature.find.fetch("featureUser").where().eq("featureUser.id", user_id).findList();
		Iterator<Feature> it = featureList.iterator();
		Feature f;
		
		final Kml kml = new Kml();
		final Document document = kml.createAndSetDocument()
				.withName("User_"+user_id+".kml").withOpen(true);
	
		document.createAndAddStyle()
			.withId("myBalloonStyle")
			.createAndSetBalloonStyle()
			.withId("ID")
			.withBgColor("ffffffbb")
			.withTextColor("ff000000")
		//	.withText("<![CDATA[<table width=\"400\"><tr><td>" + "<b><font color='#CC0000' size='+3'>$[name] Ahhhhhhhhh!  </font></b>" + "<br/><br/></td></tr>" + "<tr><td><font face='Courier'>$[description]</font>" + "<br/><br/></td></tr></table>]]>");
			.withText("<![CDATA[" + "<b><font color='#CC0000' size='+3'>$[name]</font></b>" + "<br/><br/>" + "<font face='Courier'>$[description]</font>" + "]]>");
	//	<div style="width: 200px; height: 50px; overflow: auto; overflow-x:hidden;">
		document.createAndAddStyle()
			.withId("myIconStyle")
			.createAndSetIconStyle()
			.withScale(1)
			.createAndSetIcon()
			.withHref(MyConstants.KML_MAPPA_ICON);
		
		while(it.hasNext()) {
			f = it.next();
			
			Placemark placemark = document.createAndAddPlacemark()
					.withName(f.featureUser.full_name)
					.withDescription("<table width=\"300px\"><tr><td><p>" + f.properties.description + "</p></td></tr><tr><td>" + "<img border=\"0\" src=\""+f.retrieveImages().standard_resolution+"\" alt=\"Pulpit rock\" width=\"300\" ></td></tr></table>" )
					.withStyleUrl("#myBalloonStyle");
					
			Point point = placemark.withStyleUrl("#myIconStyle").createAndSetPoint();
			List<Coordinate> coord = point.createAndSetCoordinates();
			coord.add(new Coordinate(f.geometry.getLng(), f.geometry.getLat()));
			
//			kml.createAndSetPlacemark()
//				.withDescription(f.properties.description)
//				.withName(f.featureUser.full_name).withOpen(Boolean.TRUE)
//				.createAndSetPoint().addToCoordinates(f.geometry.getLng(), f.geometry.getLat());
		}
		try {
			if( kml.marshal(kmlFile) )
				return true;
			else
				return false;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
}

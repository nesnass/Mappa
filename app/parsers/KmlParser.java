package parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.List;

import models.Feature;

import de.micromata.opengis.kml.v_2_2_0.Kml;

public class KmlParser {

	public static boolean getKmlForSession(String session_id, File kmlFile) {
		
		List<Feature> featureList = Feature.find.fetch("featureSession").where().eq("featureSession.facebook_group_id", Long.valueOf(session_id)).findList();
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
}

package helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import flexjson.JSONSerializer;

import models.Feature;


/**
 * @author Muhammad Fahied & Richard Nesnass
 */

public class FeatureCollection {
	public  String type = "FeatureCollection";
	public ArrayList<Feature> features;
	public Meta meta = new Meta();
	
	public FeatureCollection() {
		this.features = new ArrayList<Feature>();
	}
	
	public FeatureCollection(Collection<Feature> features) {
		this();
		for(Feature f : features)
		{
			this.features.add(f);
		}
	}
	
	public void add(Feature f) {
		this.features.add(f);
	}
	
	public List<Feature> getFeatures() {
		return features;
	}
	
	public String toJson()
	{ 
		Iterator<Feature> it = features.iterator();
		Feature f = null;
		String jsonString = "{\"type\" : \"FeatureCollection\", \"meta\" : " + meta.toJson() + ", \"features\":[";
		while(it.hasNext())
		{
			f = it.next();
			jsonString += f.toJson();
			if(it.hasNext())
				jsonString += ",";
		}
		jsonString += "]}";
		return jsonString;
	}
	
}
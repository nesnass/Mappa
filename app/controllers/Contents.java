package controllers;

import parsers.InstagramParser;
import parsers.TwitterParser;
import models.Feature;
import models.MUser;
import play.mvc.Controller;
import play.mvc.Result;

public class Contents extends Controller
{
	
	// ******** This method adapted, but seems incomplete??
	public static Result contentOfFeature(long id)
	{
		Feature feature = Feature.find.byId(id);

		if (feature == null) {
			return ok("This POI does not exist anymore.");
		} else {

			String decString = feature.properties.description;

			decString = decString.replaceAll("^\"|\"$", "");
			String description = TwitterParser.parse(decString, "Overlay");
			String image = "";
			MUser user = new MUser();

			if (feature.properties.imageStandardResolutionURL != "")
			{
				image = "<div id=\"image-holder\"> "
						+ "<img src="
						+ feature.properties.imageStandardResolutionURL
						+ " alt=\"Smiley face\"  width=\"612\" > " + "</div> ";
			}
			
			if (feature.featureUser != null)
			{
				user.id = feature.featureUser.facebook_id;
				user.full_name = feature.featureUser.full_name;	
			}
			return ok();
		}
	}
	
	
	public static Result contentOfInstaPOI(String id)
	{
		Feature feature;
		feature = InstagramParser.getInstaByMediaId(id);
		if(feature != null)
			return ok(feature.toJson());
		else
			return ok("POI Not Found");
	}
	


}

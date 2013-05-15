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
	public static Result contentOfFeature(String id)
	{
		Feature feature = Feature.find.byId(Long.parseLong(id));

		if (feature == null) {
			return ok("This POI does not exist anymore.");
		} else {

			String decString = feature.properties.description;

			decString = decString.replaceAll("^\"|\"$", "");
			String description = TwitterParser.parse(decString, "Overlay");
			String image = "";
			MUser user = new MUser();

			if (feature.retrieveImages().standard_resolution != "")
			{
				image = "<div id=\"image-holder\"> "
						+ "<img src="
						+ feature.retrieveImages().standard_resolution
						+ " alt=\"Smiley face\"  width=\"612\" > " + "</div> ";
			}
			
			if (feature.featureUser != null)
			{
				user.setId( feature.featureUser.getId() );
				user.full_name = feature.featureUser.full_name;	
			}
			
			response().setContentType("text/html; charset=utf-8");
			return ok(image);
		}
	}
	
	
	public static Result contentOfInstaPOI(String id)
	{
		Feature feature;
		feature = InstagramParser.getInstaByMediaId(id);
		if(feature != null) {
			response().setContentType("text/html; charset=utf-8");
			String s = feature.toJson();
			return ok(s);
		}
		else
			return ok("POI Not Found");
	}
	


}

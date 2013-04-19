package controllers;

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

			String decString = feature.description;

			decString = decString.replaceAll("^\"|\"$", "");
			String description = TwitterParser.parse(decString, "Overlay");
			String image = "";
			MUser user = new MUser();

			if (feature.imageStandardResolutionURL != "")
			{
				image = "<div id=\"image-holder\"> "
						+ "<img src="
						+ feature.imageStandardResolutionURL
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
	
	
//	public static Result contentOfInstaPOI(String id)
//	{
//		Feature feature;
//		try {
//			feature = InstagramParser.getInstaByMediaId(id);
//			return ok(toJson(feature));
			
//			String decString = feature.properties.get("description").toString();
//			decString = decString.replaceAll("^\"|\"$", "");
//			String description = TwitterHelper.parse(decString, "Instagram");
//			
//			String image = "";
//			User user = new User();
//			
//			if (feature.properties.get("standard_resolution") != null) {
//				image  = "<div id=\"image-holder\"> " +
//	                    "<img src="+feature.properties.get("standard_resolution").toString()+" alt=\"Smiley face\"  width=\"612\" height=\"612\" > " +
//	                    "</div> " ;
//			}
//			
//			if (feature.properties.get("user") != null) {
//				
//				JsonNode userNode = toJson(feature.properties.get("user"));
//				user.id =userNode.get("id").asText();
//				user.full_name = userNode.get("full_name").asText();;
//				
//			}
//
//			Html content = new Html(image + description);
//
//			return ok(index.render(user, content));
//		} catch (Exception e) {
//			return ok("This POI does not exist anymore.");
//		}
		
//	}
	


}

package controllers;

import static play.libs.Json.toJson;
import helpers.FeatureCollection;
import java.util.List;
import models.Feature;
import models.MUser;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * @author Muhammad Fahied & Richard Nesnass
 */
public class Users extends Controller
{
	public static Result fetchGeoFeaturesByUser(String userID)
	{
		Long id = new Long(userID);
		MUser user = MUser.find.byId(id);
		if (user == null) {
			return ok(toJson(new String[0]));
		}
		List<Feature> featureList = Feature.find.where().eq("featureUser", user).findList();
		
		// *********************************************
		// What is the need for FeatureCollection class?
		// *********************************************
		FeatureCollection features = new FeatureCollection(featureList);
		return ok(toJson(features));
	}
	
/*	
	public static void saveFeatureRefForUser(String userID, String full_name, Feature feature)
	{
		Long id = new Long(userID);
		MUser user = MUser.find.byId(id);
		if (user == null) {
			MUser newUser = new MUser(id, full_name);
			user.features.add(feature);
			user.update();
		}
			else {
				User user = User.find().byId(id);
				user.features.add(feature);
				user.update();
		}
	}
*/
}

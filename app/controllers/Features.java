package controllers;

//http://flexjson.sourceforge.net/
import static play.libs.Json.toJson;
import helpers.FeatureCollection;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import com.avaje.ebean.Ebean;

import external.InstagramParser;
import external.MyConstants;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData.FilePart;
import models.*;
import models.geometry.Geometry;



/**
 * @author Richard Nesnass
 */
public class Features extends Controller
{
	private static int MAX_FEATURES_TO_GET_IN_BOUNDING_BOX = 18;
	private static ObjectMapper mapper = new ObjectMapper();
//	private static JSONSerializer serializer = new JSONSerializer();

	// GET /user/:userId
	public static Result fetchGeoFeaturesByUser(String userID)
	{
		MUser user = MUser.find.where().eq("facebook_id", Long.valueOf(userID)).findUnique();
		if (user == null) {
			List<String> empty = new ArrayList<String>();
			return ok(toJson(empty));
		}
		
		FeatureCollection features = new FeatureCollection(user.userFeatures);
		return ok(features.toJson());
		//return ok(serializer.include("features").exclude("*.class").serialize(features));
	}
	
	// GET /search/:hashTag
	public static Result fetchGeoFeaturesByTag(String hashTag)
	{
		Tag foundTag = Tag.find.fetch("tagFeatures").where().eq("tag", hashTag).findUnique();
		FeatureCollection featureCollection = new FeatureCollection(foundTag.tagFeatures);
		return ok(featureCollection.toJson());
		// Include the features linked to this tag, but not the user details or the 'class' key
		//return ok(serializer.include("tagFeatures").exclude("*.featureUser").exclude("*.class").serialize(foundTag));
	}
	
	// GET /geo
	public static Result fetchAllGeoFeautres()
	{
		List<Feature> featureList = Feature.find.all();
		FeatureCollection featureCollection = new FeatureCollection(featureList);
		//return ok(serializer.include("tagFeatures").include("features").exclude("*.featureUser").exclude("*.class").serialize(featureCollection));
		return ok(featureCollection.toJson());
	}
	
	//  GET /geo/:id
	public static Result featureById(String id) {
		Feature feature = Feature.find.byId(id);
		if (feature == null) {
			return ok("POI Not found");
		}
		return ok(feature.toJson());
	}
	
	// Return a list of the maxItem closest features to the given source
	public static Iterable<Feature> getClosest(final Feature source, final List<Feature> others, int maxItems) {
        Collections.sort(others, source);
        return others.subList(0, Math.min(maxItems, others.size()));
    }
	
	// GET /geo/box/
	public static Result geoFeaturesInBoundingBox(double lng1, double lat1, double lng2, double lat2) throws Exception
	{	
		List<Feature> allFeaturesWithinBounds = Ebean.find(Feature.class)
			.where()
			.filterMany("featureGeometry")
			.between("coordinate_0", lng1, lng2)
			.between("coordinate_1", lat1, lat2)
			.findList();
		
		// Create a reference to the center of the bounding box
		Geometry sourceGeometry = new Geometry((lng2-lng1)/2+lng1, (lat2-lat1)/2+lat1);
		Feature source = new Feature(sourceGeometry);
		
		// Retrieve the list of closest features to the source, add to it the Instagram found closest also
		ArrayList<Feature> closestToSource = (ArrayList<Feature>) getClosest(source, allFeaturesWithinBounds, MAX_FEATURES_TO_GET_IN_BOUNDING_BOX);
		List<Feature> instaPOIs = InstagramParser.searchInstaPOIsByBBox(lng1, lat1, lng2, lat2);
		closestToSource.addAll(instaPOIs);
		FeatureCollection collection = new FeatureCollection(closestToSource);
		return ok(collection.toJson());
	}
	
	// POST /geo
	public static Result createGeoFeature()
	{

		FilePart featureFilePart;
		BufferedReader fileReader;
		long facebook_id = 0;
		JsonNode featureNode = null;
		MUser user = null;
		Feature newFeature = null;
		String source_type = "";
		
		try {
			featureFilePart = ctx().request().body().asMultipartFormData().getFile("feature");
			fileReader = new BufferedReader(new FileReader(featureFilePart.getFile()));
			featureNode = mapper.readTree(fileReader);
			facebook_id = featureNode.get("properties").get("user").get("id").asLong();
			source_type = featureNode.get("properties").get("source_type").asText();
			user = MUser.find.where().eq("facebook_id", facebook_id).findUnique();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// User is the facebook user. does not exist in DB, then create it
		if(user == null && facebook_id != 0)
		{
			user = new MUser(facebook_id, featureNode.get("properties").get("user").get("full_name").asText());
			user.location_0 = featureNode.get("properties").get("user").get("location").get(0).asLong();
			user.location_1 = featureNode.get("properties").get("user").get("location").get(1).asLong();
		}

		try {
			// Setup a new feature, including geometry
			newFeature = new Feature(featureNode);
			// Set the user reference
			newFeature.featureUser = user;
			// Add the feature to the user
			user.userFeatures.add(newFeature);

			if(source_type.equalsIgnoreCase(MyConstants.Strings.OVERLAY.toString()))
			{
				;
			}
			else if(source_type.equalsIgnoreCase("mapped_instagram"))
			{
				long mapper_id = 0;
				mapper_id = featureNode.get("properties").get("mapper").get("id").asLong();
				MUser mapperUser = MUser.find.where().eq("facebook_id", mapper_id).findUnique();
				
				// User is the facebook user. does not exist in DB, then create it
				if(mapperUser == null && mapper_id != 0)
				{
					mapperUser = new MUser(mapper_id, featureNode.get("properties").get("mapper").get("full_name").asText());
					mapperUser.location_0 = featureNode.get("properties").get("mapper").get("location").get(0).asLong();
					mapperUser.location_1 = featureNode.get("properties").get("mapper").get("location").get(1).asLong();
				}

				// Set the mapperUser reference
				newFeature.mapperUser = mapperUser;
				Ebean.save(mapperUser);
			}

			// Save the feature in DB, the feature and tag save will cascade from user due to mapping settings
			Ebean.save(user);
		}
		catch (javax.persistence.PersistenceException e)
		{
			ObjectNode result = Json.newObject();
			String message = e.getMessage();
			CharSequence seq = "duplicate key value violates unique constraint";
			if(message.contains(seq)) {
				result.put("status", "KO");
				result.put("message", "Tag " + message.substring(message.indexOf("Detail: ")+8, message.length()-2));
			}
			else {
				result.put("status", "KO");
				result.put("message", message);
			}
		    return badRequest(result);
		}
		
		
		return ok(newFeature.toJson());
	}
}
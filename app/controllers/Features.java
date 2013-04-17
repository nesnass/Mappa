package controllers;

//http://flexjson.sourceforge.net/
import static play.libs.Json.toJson;
import helpers.FeatureCollection;
import helpers.GeoCalculations;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.imgscalr.Scalr;

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
	private static int MOST_RECENT_FEATURES_TO_GET = 3;

	
	// GET /user/:userId
	public static Result getGeoFeaturesByUser(String userID)
	{
		MUser user = MUser.find.where().eq("facebook_id", Long.valueOf(userID)).findUnique();
		if (user == null) {
			List<String> empty = new ArrayList<String>();
			return ok(toJson(empty));
		}
		
		FeatureCollection features = new FeatureCollection(user.userFeatures);
		return ok(features.toJson());
	}
	
	
	// GET /search/:hashTag
	public static Result getGeoFeaturesByTag(String hashTag)
	{
		Tag foundTag = Tag.find.fetch("tagFeatures").where().eq("tag", hashTag).findUnique();
		FeatureCollection featureCollection = new FeatureCollection(foundTag.tagFeatures);
		return ok(featureCollection.toJson());
	}
	
	
	// GET /geo
	public static Result getAllGeoFeautres()
	{
		List<Feature> featureList = Feature.find.all();
		FeatureCollection featureCollection = new FeatureCollection(featureList);
		return ok(featureCollection.toJson());
	}
	
	
	//  GET /geo/:id
	public static Result getFeatureById(String id) {
		Feature feature = Feature.find.byId(id);
		if (feature == null) {
			return ok("POI Not found");
		}
		return ok(feature.toJson());
	}
	
	
	// Return a list of the maxItem closest Features to the given source Feature
	private static List<Feature> sortAndLimitClosestFeaturesToSource(final Feature source, final List<Feature> others, int maxItems) {
        Collections.sort(others, source);
        return others.subList(0, Math.min(maxItems, others.size()));
    }
	
	
	// Given bounding coordinates, return all MAPPA Features within
	private static List<Feature> getFeaturesClosestToSource(double lat1, double lng1, double lat2, double lng2)
	{
		// First .between coordinate should be smaller than the second, swap if necessary
		double longHolder = lng1;
		double latHolder = lat1;
		if(lng1 > lng2)
		{
			lng1 = lng2;
			lng2 = longHolder;
		}
		if(lat1 > lat2)
		{
			lat1 = lat2;
			lat2 = latHolder;
		}
		
		List<Feature> allFeaturesWithinBounds = Feature.find.where()
				.between("featureGeometry.coordinate_0", lng1, lng2)
				.between("featureGeometry.coordinate_1", lat1, lat2)
				.findList();

		if(allFeaturesWithinBounds.size() == 0)
			return allFeaturesWithinBounds;
		// Create a reference to the center of the bounding box by getting a midpoint
		double midpoint[] = GeoCalculations.midpointCoordsFromStartEndCoords(lat1, lng1, lat2, lng2);
		Geometry sourceGeometry = new Geometry(midpoint[1], midpoint[0]);
		Feature source = new Feature(sourceGeometry);

		// Retrieve the list of closest features to the source, add to it the Instagram found closest also
		return sortAndLimitClosestFeaturesToSource(source, allFeaturesWithinBounds, MAX_FEATURES_TO_GET_IN_BOUNDING_BOX);
	}

	
	// GET /geo/box/
	// ******* Needs further testing to confirm reliable results ********
	public static Result getGeoFeaturesInBoundingBox(double lng1, double lat1, double lng2, double lat2)
	{	
		List<Feature> closestToSource = getFeaturesClosestToSource(lat1, lng1, lat2, lng2);
		List<Feature> instaPOIs = InstagramParser.searchInstaPOIsByBBox(lng1, lat1, lng2, lat2);
	//	closestToSource.addAll(instaPOIs);
		FeatureCollection collection = new FeatureCollection(closestToSource);
		return ok(collection.toJson());
	}
	
	
	// GET /geo/radius/:lng/:lat/:radiusInMeters
	// *********  There is no 'near' call within the EBean implementation, so we call search by forming a bounding box first, 
	// *********  then search within it using circular radius
	public static Result getFeaturesInRadius(double lng, double lat, int radius)
	{
		double lowBound[] = GeoCalculations.destinationCoordsFromDistance(lat, lng, 315, radius);    	// Top left corner
		double highBound[] = GeoCalculations.destinationCoordsFromDistance(lat, lng, 135, radius);		// Bottom right corner
		List<Feature> featuresInRadius = getFeaturesClosestToSource(lowBound[0], lowBound[1], highBound[0], highBound[1]);
		
		// ********* For more precise results, this box set should now be searched for a circular radius set within
		
		List<Feature> instaPOIs;
		try {
			instaPOIs = InstagramParser.searchInstaByRadius(lng, lat, radius);
//			featuresInRadius.addAll(instaPOIs);
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		FeatureCollection collection = new FeatureCollection(featuresInRadius);
		return ok(collection.toJson());
	}
	
	
	// GET /geo/recent/:lng/:lat
	public static Result getMostRecentGeoFeatures(double lng, double lat)
	{
		// ******** Thinking this should do a radius search first for the given lat / lng, then display the most recent? ********
		
		// Find all features Limited to nearest 18
		List<Feature> features = Feature.find.where().orderBy("created_time desc").setMaxRows(MOST_RECENT_FEATURES_TO_GET).findList();
		
		List<Feature> instaPOIs;
		try {
			instaPOIs = InstagramParser.searchRecentInstaFeatures(lat, lng);
//			features.addAll(instaPOIs);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		FeatureCollection collection = new FeatureCollection(features);
		return ok(collection.toJson());
	}
	
	
	// POST /geo
	public static Result createGeoFeature()
	{
		ObjectMapper mapper = new ObjectMapper();
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

			if(source_type.equalsIgnoreCase(MyConstants.FeatureStrings.OVERLAY.toString()))
			{
				if (ctx().request().body().asMultipartFormData().getFile("picture") != null) 
				{
					FilePart filePart = ctx().request().body().asMultipartFormData().getFile("picture");
					newFeature.image_url_standard_resolution = uploadFeatureImages(filePart.getFile(), MyConstants.S3Strings.SIZE_ORIGINAL);
					newFeature.image_url_thumbnail = uploadFeatureImages(filePart.getFile(), MyConstants.S3Strings.SIZE_THUMBNAIL);
				}
			}
			else if(source_type.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString()))
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
	
	
	// Save image to Amazon S3 as original or thumbnail, return the url
	private static String uploadFeatureImages(File f, MyConstants.S3Strings size) {
		S3File s3File = new S3File();
		
		if(size == MyConstants.S3Strings.SIZE_ORIGINAL)
		{
			s3File.type = MyConstants.S3Strings.SIZE_ORIGINAL.toString();
			s3File.file = f;	
		}
		else if(size == MyConstants.S3Strings.SIZE_THUMBNAIL)
		{
			try {
				BufferedImage image = ImageIO.read(f);
				image = Scalr.resize(image, 150);
				File tmpFile = new File("thumbnail");
				ImageIO.write(image, "jpg", tmpFile);
				s3File.type = MyConstants.S3Strings.SIZE_THUMBNAIL.toString();
				s3File.file = tmpFile;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		s3File.save();
		return s3File.getUrlAsString();
	}
}
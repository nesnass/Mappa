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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.imgscalr.Scalr;

import com.avaje.ebean.Ebean;

import helpers.MyConstants;
import parsers.InstagramParser;
import parsers.KmlParser;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData.FilePart;
import models.*;
import models.geometry.Point;

/**
 * @author Richard Nesnass
 */
public class Features extends Controller
{

	// get Features by User ID
	public static Result getGeoFeaturesByUserId(String userID)
	{
		MUser user = MUser.find.where().eq("id", userID).findUnique();
		if (user == null) {
			List<String> empty = new ArrayList<String>();
			return ok(toJson(empty));
		}
		FeatureCollection features = new FeatureCollection(user.userFeatures);
		
		response().setContentType("text/html; charset=utf-8");
		String s = features.toJson(); 
		return ok(s);
	}
	
	// get Features by User Name
	public static Result getGeoFeaturesByUserName(String userName)
	{
		MUser user = MUser.find.where().eq("username", userName).findUnique();
		if (user == null) {
			List<String> empty = new ArrayList<String>();
			return ok(toJson(empty));
		}
		FeatureCollection features = new FeatureCollection(user.userFeatures);
		
		response().setContentType("text/html; charset=utf-8");
		String s = features.toJson(); 
		return ok(s);
	}

	// Get a Feature list for a facebook group ID
	public static Result getKmlBySessionId(String sessionID)
	{
		File kmlFile = new File(sessionID+".kml");
		KmlParser.getKmlForSession(sessionID, kmlFile);
		
		response().setContentType("application/x-download"); 
	//	response().setContentType("application/vnd.google-earth.kml+xml");
		response().setHeader("Content-disposition","attachment; filename="+sessionID+".kml"); 
		return ok(kmlFile);
	}
	// Get a Feature list for a facebook user ID
	public static Result getKmlByUserId(String userID)
	{
		File kmlFile = new File(userID+".kml");
		KmlParser.getKmlForUser(userID, kmlFile);
		
		response().setContentType("application/x-download"); 
	//	response().setContentType("application/vnd.google-earth.kml+xml");
		response().setHeader("Content-disposition","attachment; filename="+userID+".kml"); 
		return ok(kmlFile);
	}
	
	// GET /search/:hashTag
	public static Result getGeoFeaturesByTag(String hashTag, String sessionIDs)
	{
		Tag foundTag = Tag.find.fetch("tagFeatures").fetch("tagFeatures.featureSession").where().eq("tag", hashTag).findUnique();
		if(foundTag != null) {
			List<String> sessions = Arrays.asList(sessionIDs.split(","));
			FeatureCollection featureCollection = new FeatureCollection(foundTag.tagFeatures);
			
			Iterator<Feature> it = featureCollection.iterator();
			while(it.hasNext())
			{
				Feature fit = it.next();
				if(!sessions.contains(fit.featureSession.getFacebook_group_id()))
				{
					it.remove();
				}
			}

			response().setContentType("text/html; charset=utf-8");
			String s = featureCollection.toJson();
			return ok(s);
		}
		else
			return ok("POI Not Found");
	}
	
	
	// GET /geo?sessions=1234567
	public static Result getAllGeoFeatures(String sessionIDs)
	{
		FeatureCollection featureCollection = new FeatureCollection();
		List<Feature> featureList;
		List<String> sessions = Arrays.asList(sessionIDs.split(","));
		
		if(sessions.size() == 0) {
			featureList = Feature.find.fetch("featureSession").where().eq("featureSession.privacy", "open").findList();
			featureCollection.add(featureList);
		}
		else {
			featureList = Feature.find.fetch("featureSession").findList();
			Iterator<Feature> it = featureList.iterator();
			while(it.hasNext())
			{
				Feature fit = it.next();
				if(!sessions.contains(fit.featureSession.getFacebook_group_id()))
				{
					it.remove();
				}
			}
			featureCollection.add(featureList);
		}
		
		response().setContentType("text/html; charset=utf-8");
		String s = featureCollection.toJson();
		return ok(s);
	}
	
	
	//  GET /geo/:id/:sessionIDs
	public static Result getFeatureById(String id, String sessionID) {
		Feature feature = Feature.find.fetch("featureSession").where().idEq(Long.valueOf(id)).findUnique();
		Session s = feature.featureSession;
		if (feature == null || !s.getFacebook_group_id().equalsIgnoreCase(sessionID)) {
			return ok("POI Not Found");
		}
		
		response().setContentType("text/html; charset=utf-8");
		String str = feature.toJson(); 
		return ok(str);
	}
	

	//  DELETE /geo/?...&...
	public static Result deleteGeoFeature(String id, String user_id, String sessionID) {

		response().setContentType("text/html; charset=utf-8");
		String str = "Delete not implemented"; 
		return ok(str);
	}
	
	// Return a list of the maxItem closest Features to the given source Feature
	private static List<Feature> sortAndLimitClosestFeaturesToSource(final Feature source, final List<Feature> others, int maxItems) {
        Collections.sort(others, source);
        return others.subList(0, Math.min(maxItems, others.size()));
    }
	
	
	// Given bounding coordinates, return all MAPPA Features within
	private static List<Feature> getFeaturesClosestToSource(double lat1, double lng1, double lat2, double lng2, double[] midpoint, String sessionIDs)
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
		
		List<Feature> allFeaturesWithinBounds;
		List<String> sessions = Arrays.asList(sessionIDs.split(","));
		
		if(sessions.size() == 0) {
			allFeaturesWithinBounds = Feature.find.fetch("featureSession").where()
					.between("lng", lng1, lng2)
					.between("lat", lat1, lat2)
					.eq("featureSession.privacy", "open")
					.findList();
		}
		else {
			allFeaturesWithinBounds = Feature.find.where()
					.between("lng", lng1, lng2)
					.between("lat", lat1, lat2)
					.findList();
			
			Iterator<Feature> it = allFeaturesWithinBounds.iterator();
			while(it.hasNext())
			{
				Feature fit = it.next();
				if(!sessions.contains(fit.featureSession.getFacebook_group_id()))
				{
					it.remove();
				}
			}
		}
		
		if(allFeaturesWithinBounds.size() == 0)
			return allFeaturesWithinBounds;
		
		Point sourceGeometry = new Point(midpoint[1], midpoint[0]);
		Feature source = new Feature(sourceGeometry);

		// Retrieve the list of closest features to the source, add to it the Instagram found closest also
		return sortAndLimitClosestFeaturesToSource(source, allFeaturesWithinBounds, MyConstants.MAX_FEATURES_TO_GET);
	}

	
	// GET /geo/box/?...&...
	// ******* Needs further testing to confirm reliable results ********
	public static Result getGeoFeaturesInBoundingBox(String ln1, String la1, String ln2, String la2, String sessionIDs)
	{	
		double lng1 = Double.parseDouble(ln1);
		double lat1 = Double.parseDouble(la1);
		double lng2 = Double.parseDouble(ln2);
		double lat2 = Double.parseDouble(la2);
		
		// Create a reference to the center of the bounding box by getting a midpoint
		double midpoint[] = GeoCalculations.midpointCoordsFromStartEndCoords(lat1, lng1, lat2, lng2);
		// Calculate the radius for a circle containing the bounding box
		int radius = (int) Math.round(GeoCalculations.haversine(lat1, lng1, lat2, lng2))*500; // Radius from diameter, in meters

		List<Feature> closestToSource = getFeaturesClosestToSource(lat1, lng1, lat2, lng2, midpoint, sessionIDs);
		List<Feature> instaPOIs = InstagramParser.getQuery(MyConstants.QueryStrings.BOUNDING_BOX, midpoint[0], midpoint[1], radius);
		
		closestToSource.addAll(instaPOIs);
		FeatureCollection collection = new FeatureCollection(closestToSource);
		if(instaPOIs.size() == 0) {
			collection.meta.code = "204";
			collection.meta.error_message = "No response from Instagram. Refresh to try again";
		}
		response().setContentType("text/html; charset=utf-8");
		String s = collection.toJson();
		return ok(s);
	}
	
	
	// GET /geo/radius/:lng/:lat/:radiusInMeters/:sessionIDs
	// *********  There is no 'near' call within the EBean implementation, so we call search by forming an outer-bounding box, 
	// *********  then search within it using circular radius
	public static Result getFeaturesInRadius(String ln, String la, String rad, String sessionIDs)
	{
		double lng = Double.parseDouble(ln);
		double lat = Double.parseDouble(la);
		double radius = Double.parseDouble(rad);
		
		double outerBoxHypetnuse = Math.sqrt(((radius*MyConstants.RADIUS_MULTIPLIER)*(radius*MyConstants.RADIUS_MULTIPLIER))*2);
		double lowBound[] = GeoCalculations.destinationCoordsFromDistance(lat, lng, 315, outerBoxHypetnuse);    	// Top left corner
		double highBound[] = GeoCalculations.destinationCoordsFromDistance(lat, lng, 135, outerBoxHypetnuse);		// Bottom right corner
		
		// Create a reference to the center of the bounding box by getting a midpoint
		double midpoint[] = GeoCalculations.midpointCoordsFromStartEndCoords(lowBound[0], lowBound[1], highBound[0], highBound[1]);
		
		List<Feature> featuresInRadius = getFeaturesClosestToSource(lowBound[0], lowBound[1], highBound[0], highBound[1], midpoint, sessionIDs);
		
		// ********* For more precise results, this box set should now be searched for a circular radius set within
		
		List<Feature> instaPOIs;
		try {
			instaPOIs = InstagramParser.getQuery(MyConstants.QueryStrings.RADIUS, lat, lng, (int) Math.round(radius*MyConstants.RADIUS_MULTIPLIER));
			featuresInRadius.addAll(instaPOIs);
			// *************   Should this list be sorted by distance?
			FeatureCollection collection = new FeatureCollection(featuresInRadius);
			if(instaPOIs != null && instaPOIs.size() == 0) {
				collection.meta.code = "204";
				collection.meta.error_message = "No response from Instagram. Refresh to try again";
			}
			response().setContentType("text/html; charset=utf-8");
			String s = collection.toJson();
			return ok(s);
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ok();
	}
	
	
	// GET /geo/recent/:lng/:lat/:sessionIDs
	public static Result getMostRecentGeoFeatures(String ln, String la, String sessionIDs)
	{
		double lng = Double.parseDouble(ln);
		double lat = Double.parseDouble(la);
		
		// ******** Thinking this should do a radius search first for the given lat / lng, then display the most recent? ********
		
		// Find all features Limited to nearest 18, remove if no session id is contained within, or return "open" POIs
		List<Feature> features;
		List<String> sessions = Arrays.asList(sessionIDs.split(","));
		
		if(sessions.size() == 0) {
			features = Feature.find.fetch("featureSession").where().eq("featureSession.privacy", "open").orderBy("created_time desc").setMaxRows(MyConstants.MAX_FEATURES_TO_GET).findList();
		}
		else {
			features = Feature.find.where().orderBy("created_time desc").setMaxRows(MyConstants.MAX_FEATURES_TO_GET).findList();
			
			Iterator<Feature> it = features.iterator();
			while(it.hasNext())
			{
				Feature fit = it.next();
				if(!sessions.contains(fit.featureSession.getFacebook_group_id()))
				{
					it.remove();
				}
			}
		}
		List<Feature> instaPOIs;
		try {
			instaPOIs = InstagramParser.getQuery(MyConstants.QueryStrings.RECENT, lat, lng, MyConstants.DEFAULT_INSTAGRAM_DISTANCE);
			features.addAll(instaPOIs);
			FeatureCollection collection = new FeatureCollection(features);
			if(instaPOIs != null && instaPOIs.size() == 0) {
				collection.meta.code = "204";
				collection.meta.error_message = "No response from Instagram. Refresh to try again";
			}
//			Logger.info("FeaturesInRadius:" + collection.toJson());
			response().setContentType("text/html; charset=utf-8");
			String s = collection.toJson();
			return ok(s);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ok();
	}
	
	// PUT /geo
	// Update an existing geo feature
	public static Result updateGeoFeature()
	{
		ObjectMapper mapper = new ObjectMapper();
		FilePart jsonFilePart;
		BufferedReader fileReader;
		JsonNode featureNode = null;
		Feature updatedFeature = null;
		String source_type = "";
		
		try {
			jsonFilePart = ctx().request().body().asMultipartFormData().getFile("feature");
			fileReader = new BufferedReader(new FileReader(jsonFilePart.getFile()));
			featureNode = mapper.readTree(fileReader);
			source_type = featureNode.get("properties").get("source_type").asText();
			long fid = featureNode.get("id").asLong();
			updatedFeature = Feature.find.byId(fid);

			if(updatedFeature == null)
			{
				ObjectNode result = Json.newObject();
				result.put("status", "KO");
				result.put("message", "Feature does not exist");
			    return badRequest(result);
			}
				
			// Update the properties
			// extract properties from node and then set
			updatedFeature.assignProperties(featureNode);
			
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
		if(source_type.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPA.toString()))
		{
			if (ctx().request().body().asMultipartFormData().getFile("picture") != null) 
			{
				FilePart filePart = ctx().request().body().asMultipartFormData().getFile("picture");
				
				// Assuming a feature always has an image attached
				updatedFeature.deleteImages();
				updatedFeature.imageStandardResolutionFile = uploadFeatureImages(filePart.getFile(), MyConstants.S3Strings.SIZE_ORIGINAL, null);
				updatedFeature.imageThumbnailFile = uploadFeatureImages(filePart.getFile(), MyConstants.S3Strings.SIZE_THUMBNAIL, updatedFeature.imageStandardResolutionFile.getUuid());
			}
		}
		else if(source_type.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString()))
		{
			;
		}
		Ebean.save(updatedFeature);
		response().setContentType("text/html; charset=utf-8");
		String s = updatedFeature.toJson();
		return ok(s);
	}

	// POST /geo
	// Create a new geo feature
	public static Result createGeoFeature()
	{
		ObjectMapper mapper = new ObjectMapper();
		FilePart featureFilePart;
		BufferedReader fileReader;
		String id = "";
		JsonNode featureNode = null;
		MUser user = null;
		Feature newFeature = null;
		String source_type = "";
		
		try {
			featureFilePart = ctx().request().body().asMultipartFormData().getFile("feature");
			fileReader = new BufferedReader(new FileReader(featureFilePart.getFile()));
			featureNode = mapper.readTree(fileReader);
			id = featureNode.get("properties").get("user").get("id").asText();
			source_type = featureNode.get("properties").get("source_type").asText();
			user = MUser.find.where().eq("id", id).findUnique();
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
		if(user == null && !id.equals(""))
		{
			user = new MUser(id, featureNode.get("properties").get("user").get("full_name").asText(), featureNode.get("properties").get("user").path("username").asText());
			user.setLng( featureNode.get("properties").get("user").get("location").get(0).asDouble());
			user.setLat( featureNode.get("properties").get("user").get("location").get(1).asDouble());
		}

		try {
			// Setup a new feature, including geometry
			newFeature = new Feature(featureNode);
			// Set the user reference
			newFeature.featureUser = user;
			// Add the feature to the user
			user.userFeatures.add(newFeature);

			if(source_type.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPA.toString()))
			{
				if (ctx().request().body().asMultipartFormData().getFile("picture") != null) 
				{
					FilePart filePart = ctx().request().body().asMultipartFormData().getFile("picture");
					newFeature.imageStandardResolutionFile = uploadFeatureImages(filePart.getFile(), MyConstants.S3Strings.SIZE_ORIGINAL, null);
					newFeature.imageThumbnailFile = uploadFeatureImages(filePart.getFile(), MyConstants.S3Strings.SIZE_THUMBNAIL, newFeature.imageStandardResolutionFile.getUuid());
					newFeature.retrieveImages().standard_resolution = newFeature.imageStandardResolutionFile.getUrlAsString();
					newFeature.retrieveImages().thumbnail = newFeature.imageThumbnailFile.getUrlAsString();
				}
			}
			else if(source_type.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString()))
			{
				id = "";
				id = featureNode.get("properties").get("mapper").get("id").asText();
				MUser mapperUser = MUser.find.where().eq("id", id).findUnique();
				
				// User is the Facebook user. does not exist in DB, then create it
				if(mapperUser == null && !id.equals(""))
				{
					mapperUser = new MUser(id, featureNode.get("properties").get("mapper").get("full_name").asText(), featureNode.get("properties").get("user").path("username").asText());
					mapperUser.setLng( featureNode.get("properties").get("mapper").get("location").get(0).asDouble() );
					mapperUser.setLat( featureNode.get("properties").get("mapper").get("location").get(1).asDouble() );
				}

				// Set the mapperUser reference
				newFeature.featureMapper = mapperUser;
				// Add the feature to the mapping user
				mapperUser.userFeatures.add(newFeature);
				Ebean.save(mapperUser);
			}

			// Save the feature in DB, the feature and tag save will cascade from user due to mapping settings
			Ebean.save(user);
			newFeature.setOrigin_id();
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
		
		String jsn = newFeature.toJson();

		response().setContentType("text/html; charset=utf-8");
		return ok(jsn);
	}
	
	
	// Save image to Amazon S3 as original or thumbnail, return the url
	private static S3File uploadFeatureImages(File f, MyConstants.S3Strings size, UUID uuid) {
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
				File tmpFile = new File(".jpg");
				ImageIO.write(image, "jpg", tmpFile);
				s3File.type = MyConstants.S3Strings.SIZE_THUMBNAIL.toString();
				s3File.file = tmpFile;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(uuid != null)
			s3File.setUuid(uuid);
		s3File.save();
		return s3File;
	}
}
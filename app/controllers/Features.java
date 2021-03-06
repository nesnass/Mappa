package controllers;

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
	// get Features by User Name
	public static Result getGeoFeaturesByUserName(String userName, String sessionID)
	{
		MUser user = MUser.find.fetch("userFeatures").fetch("userFeatures.featureSession").where().eq("username", userName).eq("userFeatures.featureSession.facebook_group_id", sessionID).findUnique();
		if (user != null) {
			FeatureCollection featureCollection = new FeatureCollection(user.userFeatures);
			response().setContentType("application/json; charset=utf-8");
			String s = featureCollection.toJson(); 
			return ok(s);
		}
		return ok("No Item Found");
	}
	
	// Get a KML for a facebook group ID
	public static Result getKmlBySessionId(String sessionID)
	{
		File kmlFile = new File(sessionID+".kml");
		KmlParser.getKmlForSession(sessionID, kmlFile);
		response().setContentType("application/x-download"); 
		//	response().setContentType("application/vnd.google-earth.kml+xml");
		response().setHeader("Content-disposition","attachment; filename="+sessionID+".kml"); 
		return ok(kmlFile);
	}
	
	// Get a KML for a facebook user ID
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
	public static Result getGeoFeaturesByTag(String hashTag, String sessionID)
	{
		Tag foundTag = Tag.find.fetch("tagFeatures").fetch("tagFeatures.featureSession").where().eq("tag", hashTag).eq("tagFeatures.featureSession.facebook_group_id", sessionID).findUnique();
		if(foundTag != null && foundTag.tagFeatures != null) {
			FeatureCollection featureCollection = new FeatureCollection(foundTag.tagFeatures);
			response().setContentType("application/json; charset=utf-8");
			String s = featureCollection.toJson();
			return ok(s);			
		}
		return ok("No Item Found");
	}
	
	
	// GET /geo?sessions=...
	public static Result getAllGeoFeatures(String sessionID) {
		Session s = Session.find.fetch("sessionFeatures").where().eq("facebook_group_id", sessionID).findUnique();
		if(s != null) {
			List<Feature> featureList = s.sessionFeatures;
			FeatureCollection featureCollection = new FeatureCollection(featureList);
			response().setContentType("application/json; charset=utf-8");
			String js = featureCollection.toJson();
			return ok(js);
		}
		return ok("No Item Found");
	}
	
	
	//  GET /geo/:id/:sessionIDs
	public static Result getFeatureById(String id, String sessionID) {
		Feature feature = Feature.find.fetch("featureSession").where().idEq(Long.valueOf(id)).findUnique();
		if (feature != null && feature.featureSession.getFacebook_group_id().equalsIgnoreCase(sessionID)) {
			response().setContentType("application/json; charset=utf-8");
			String js = feature.toJson(); 
			return ok(js);
		}
		return ok("POI Not Found");
	}
	

	//  DELETE /geo/?...&...
	public static Result deleteGeoFeature(String id, String user_id, String session) {
		String str = "POI not found";
		Feature f = Feature.find.fetch("featureUser").fetch("featureSession").where().idEq(Long.valueOf(id)).findUnique();
		if(f != null) {
			String uid = f.featureUser.getFacebook_id();
			String sid = f.featureSession.getFacebook_group_id();
			if(uid.equalsIgnoreCase(user_id) && sid.equalsIgnoreCase(session)) {
				f.updateTags(null);
				Ebean.delete(f);
				str = "POI Deleted";
			}
			else
				str = "Unable to Delete";
		}
		response().setContentType("application/json; charset=utf-8");
		return ok(str);
	}
	
	// Return a list of the maxItem closest Features to the given source Feature
	private static List<Feature> sortAndLimitClosestFeaturesToSource(final Feature source, final List<Feature> others, int maxItems) {
        Collections.sort(others, source.new DistanceComparator());
        return others.subList(0, Math.min(maxItems, others.size()));
    }
	
	// Return a list of the maxItem recent Features to the given source Feature
	private static List<Feature> sortAndLimitRecentFeaturesToSource(final Feature source, final List<Feature> others, int maxItems) {
        Collections.sort(others, source.new TimeStampComparator());
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
			allFeaturesWithinBounds = Feature.find.fetch("featureSession").where()
					.between("lng", lng1, lng2)
					.between("lat", lat1, lat2)
					.findList();
			
			Iterator<Feature> it = allFeaturesWithinBounds.iterator();
			while(it.hasNext())
			{
				Feature fit = it.next();
				Session s = fit.featureSession;
				if(s != null && !sessions.contains(s.getFacebook_group_id()))
				{
					it.remove();
				}
			}
		}
		
		if(allFeaturesWithinBounds.size() == 0)
			return new ArrayList<Feature>();
		
		Point sourceGeometry = new Point(midpoint[1], midpoint[0]);
		Feature source = new Feature(sourceGeometry);

		// Retrieve the list of closest features to the source, add to it the Instagram found closest also
		return sortAndLimitClosestFeaturesToSource(source, allFeaturesWithinBounds, MyConstants.MAX_FEATURES_TO_GET);
	}

	
	// GET geo/box/?lng1=[lng1]&lat1=[lat1]&lng2=[lng2]&lat2=[lat2]&sessions=[sessions]
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
		List<Feature> instaPOIs = new ArrayList<Feature>();
		try {
			instaPOIs = InstagramParser.getQuery(MyConstants.QueryStrings.BOUNDING_BOX, midpoint[0], midpoint[1], radius);
			closestToSource.addAll(instaPOIs);
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		closestToSource.addAll(instaPOIs);

		FeatureCollection collection = new FeatureCollection(closestToSource);
		if(instaPOIs.size() == 0) {
			collection.meta.code = "204";
			collection.meta.error_message = "No response from Instagram. Refresh to try again";
		}
		response().setContentType("application/json; charset=utf-8");
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
		
		List<Feature> instaPOIs = new ArrayList<Feature>();
		try {
			instaPOIs = InstagramParser.getQuery(MyConstants.QueryStrings.RADIUS, lat, lng, (int) Math.round(radius*MyConstants.RADIUS_MULTIPLIER));
			featuresInRadius.addAll(instaPOIs);
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		FeatureCollection collection = new FeatureCollection(featuresInRadius);
		if(instaPOIs.size() == 0) {
			collection.meta.code = "204";
			collection.meta.error_message = "No response from Instagram. Refresh to try again";
		}
		response().setContentType("application/json; charset=utf-8");
		String s = collection.toJson();
		return ok(s);
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
		List<Feature> instaPOIs = new ArrayList<Feature>();

		try {
			instaPOIs = InstagramParser.getQuery(MyConstants.QueryStrings.RECENT, lat, lng, MyConstants.DEFAULT_INSTAGRAM_DISTANCE);
			features.addAll(instaPOIs);
			FeatureCollection collection;
			String s = "";
			if(features.size() > 0) {
				collection = new FeatureCollection(sortAndLimitRecentFeaturesToSource(features.get(0), features, MyConstants.MAX_FEATURES_TO_GET ));
				if(instaPOIs.size() == 0) {
					collection.meta.code = "204";
					collection.meta.error_message = "No response from Instagram. Refresh to try again";
				}
				s = collection.toJson();
			}
			else
				s = "No POIs found";
			response().setContentType("application/json; charset=utf-8");
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
			updatedFeature = Feature.find.fetch("featureTags").where().eq("id", fid).findUnique();

			if(updatedFeature == null)
			{
				ObjectNode result = Json.newObject();
				result.put("status", "KO");
				result.put("message", "Feature does not exist");
			    return badRequest(result);
			}
			if(source_type.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPA.toString()))
			{
				updatedFeature.properties.description = featureNode.get("properties").get("description").asText();
				if (ctx().request().body().asMultipartFormData().getFile("picture") != null) 
				{
					FilePart filePart = ctx().request().body().asMultipartFormData().getFile("picture");
					
					// Assuming a feature always has an image attached
					updatedFeature.deleteImages();
					// Set regular parameters
					updatedFeature.imageStandardResolutionFile = uploadFeatureImages(filePart.getFile(), MyConstants.S3Strings.SIZE_ORIGINAL, null);
					updatedFeature.imageThumbnailFile = uploadFeatureImages(filePart.getFile(), MyConstants.S3Strings.SIZE_THUMBNAIL, updatedFeature.imageStandardResolutionFile.getUuid());
					updatedFeature.retrieveImages().standard_resolution = updatedFeature.imageStandardResolutionFile.getUrlAsString();
					updatedFeature.retrieveImages().thumbnail = updatedFeature.imageThumbnailFile.getUrlAsString();
				}
			}
			else if(source_type.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString()))
			{
				updatedFeature.properties.mapper_description = featureNode.get("properties").path("mapper_description").getTextValue();
			}
			// Update the properties
			// extract properties from node and then set
			updatedFeature.assignProperties(featureNode, source_type);
			
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

		Ebean.save(updatedFeature);
		response().setContentType("application/json; charset=utf-8");
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
			
			source_type = featureNode.get("properties").get("source_type").asText();
			if(source_type.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPA.toString()))
				id = featureNode.get("properties").get("user").get("id").asText();
			else
				id = featureNode.get("properties").get("mapper").get("id").asText();
			user = MUser.find.where().eq("facebook_id", id).findUnique();
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
		
		// User is the facebook user. If it does not exist in DB, then create it
		// Current client supplied JSON needs refactoring to provide an 'origin' section. At present user and mapper are mixing up types depending on source_type..
		if(user == null && !id.equals(""))
		{
			String fn = "", un = "", pp = "";
			double ln = 0, la = 0;
			if(source_type.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPA.toString())) {
				fn = featureNode.get("properties").get("user").get("full_name").asText();
				un = featureNode.get("properties").get("user").path("username").asText();
				ln = featureNode.get("properties").get("user").get("location").get(0).asDouble();
				la = featureNode.get("properties").get("user").get("location").get(1).asDouble();
				pp = featureNode.get("properties").get("user").path("profile_picture").asText();
			}
			else if(source_type.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString())) {
				fn = featureNode.get("properties").get("mapper").get("full_name").asText();
				// Username and mapper's profile pic currently not being sent in JSON?
				un = featureNode.get("properties").get("mapper").path("username").asText();
				ln = featureNode.get("properties").get("mapper").get("location").get(0).asDouble();
				la = featureNode.get("properties").get("mapper").get("location").get(1).asDouble();
				pp = featureNode.get("properties").get("mapper").path("profile_picture").asText();
			}
			user = new MUser(id, fn, un, pp);
			user.setLng(ln);
			user.setLat(la);
		}

		try {
			// Setup a new feature, including geometry
			newFeature = new Feature();
			if(source_type.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPA.toString()))
			{
				newFeature.properties.description = featureNode.get("properties").get("description").asText();
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
				newFeature.properties.description = featureNode.get("properties").get("description").asText();
				newFeature.properties.mapper_description = featureNode.get("properties").get("mapper_description").asText();
				newFeature.retrieveImages().standard_resolution = featureNode.get("properties").get("images").path("standard_resolution").asText();
				newFeature.retrieveImages().thumbnail = featureNode.get("properties").get("images").path("thumbnail").getTextValue();
				// 'name' not included in regular 'Overlay' feature??  '.path' call is used to return a 'missing node' instead of null if node not found
				newFeature.properties.icon_url = MyConstants.NEW_FEATURE_SERVER_NAME_PORT + "/resources/images/mapped_instagram.png";
			}
			user.userFeatures.add(newFeature);
			// Save the feature in DB, the feature and tag save will cascade from user due to mapping settings
			Ebean.save(user);
			// Assign remaining general properties
			newFeature.assignProperties(featureNode, source_type);
			// Set the user reference
			newFeature.featureUser = user;
			// Add the feature to the user
			Ebean.save(newFeature);
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

		response().setContentType("application/json; charset=utf-8");
		return ok(jsn);
	}
	
	
	// Save image to Amazon S3 as original or thumbnail, return the url
	private static S3File uploadFeatureImages(File f, MyConstants.S3Strings size, UUID uuid) {
		S3File s3File = new S3File();
		
		if(size == MyConstants.S3Strings.SIZE_ORIGINAL)
		{
			try {
				BufferedImage image = ImageIO.read(f);
				image = Scalr.resize(image, Scalr.Method.BALANCED, 960);
				File tmpFile = new File(".jpg");
				ImageIO.write(image, "jpg", tmpFile);
			//	image.flush();
				s3File.type = MyConstants.S3Strings.SIZE_ORIGINAL.toString();
				s3File.file = tmpFile;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if(size == MyConstants.S3Strings.SIZE_THUMBNAIL)
		{
			try {
				BufferedImage image = ImageIO.read(f);
				image = Scalr.resize(image, 150);
				File tmpFile = new File(".jpg");
				ImageIO.write(image, "jpg", tmpFile);
			//	image.flush();
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
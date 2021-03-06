package controllers;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.avaje.ebean.Ebean;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import models.*;

public class TestControllerOld extends Controller {

	private static JsonNode node;

	public static Result allusers() {
		List<MUser> theList = MUser.all();
		Iterator<MUser> it = theList.iterator();
		String result = "";
		while(it.hasNext()) {
			MUser mu = it.next();
			result += mu.full_name + " : [ ";
			Iterator<Feature> it2 = mu.userFeatures.iterator();
			while(it2.hasNext()) {
				Feature f = it2.next();
				result += f.toString();
			}
			result += "]";
		}
		return ok(Json.toJson(result));
	}

	public static Result alltags() {
		List<Tag> theList = Tag.all();
		Iterator<Tag> it = theList.iterator();
		String result = "";
		while(it.hasNext()) {
			Tag t = it.next();
			result += t.tag + " : [" + t.tagFeatures.toString() + "], ";
		}
		return ok(Json.toJson(result));
	}
	
	public static Result getUserFeatures() {
		ObjectNode result = Json.newObject();
		node = ctx().request().body().asJson();
		long userid = node.get("userid").asLong();
		MUser user = MUser.find.byId(userid);
		if(user != null)
		{
			List<Feature> fList = Feature.find.where().eq("featureUser", user).findList();
			Geometry g = fList.get(0).geometry;
			return ok(user.toString() + "List length: " + fList.size() + "First Item name: " + fList.get(0).description + "Geom: " + g.coordinate_0);
		}
		else {
			result.put("status", "KO");
			result.put("message", "Invalid user");
			return badRequest(result);
		}
	}
	
	// Given a JSON user id and feature parameters, set up a new feature
	public static Result addUserFeature() {
		node = ctx().request().body().asJson();
		long userid = node.get("userid").asLong();
		MUser user = MUser.find.byId(userid);
		ObjectMapper mapper = new ObjectMapper();
		
		if(user == null)
		{
			ObjectNode result = Json.newObject();
			result.put("status", "KO");
		    result.put("message", "User not found");
		    return badRequest(result);
		}
		
		Feature newFeature = null;
		try {
			// Create a feature from JSON
			newFeature = mapper.readValue(node.get("feature"), Feature.class);
		//	newFeature = new Feature();
		//	newFeature.description = "test description";
			// Add the embedded geometry to feature
		//	newFeature.geometry = mapper.readValue(node.get("feature").get("geometry"), Geometry.class);

			// Add any tags supplied to the feature
			//String[] taglist = mapper.readValue(node.get("feature").get("tags"), String[].class);
		//	String[] taglist = new String[0];
			// Set the user reference
			newFeature.featureUser = user;
			
			// Add the feature to the user
			user.userFeatures.add(newFeature);
		/*	
			for(int i=0; i < taglist.length; i++)
			{
				String t = taglist[i];
				
				// Check to see if tag is already in DB
				Tag existingTag = Tag.find.where().eq("tag", t).findUnique();
				
				// If tag already exists, add an existing tag reference to this feature
				if(existingTag != null)
				{
					newFeature.featureTags.add(existingTag);
					existingTag.tagFeatures.add(newFeature);
				}
				// Otherwise add the new tag and a reference to the feature
				else
				{
					Tag newTag = new Tag(t);
					newTag.tagFeatures.add(newFeature);
					newFeature.featureTags.add(newTag);
				}
			}
		*/	
			// Save the feature in DB, the feature and tag save will cascade from user due to mapping settings
			Ebean.save(user);
			
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ok();

	}
	
	public static Result addUser() {
		node = ctx().request().body().asJson();
		String text = node.get("text").asText();
		ObjectNode result = Json.newObject();
		
		if(text != null) {
			MUser user = new MUser(text);
			user.save();
			return ok(Json.toJson(user));
		}
		else {
			result.put("status", "KO");
		    result.put("message", "Please provide a valid 'text'");
		    return badRequest(result);
		}
	}

}

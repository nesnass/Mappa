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
import models.geometry.Geometry;
import models.geometry.Point;

// http://flexjson.sourceforge.net/
	
	
	
/**
 * @author Richard Nesnass
 */
public class TestController extends Controller {

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
			result += " ]";
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
			Point g = fList.get(0).featureGeometry;
			return ok(user.toString() + "List length: " + fList.size() + "First Item name: " + fList.get(0).description + "Geom: " + g.lng);
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
		Feature newFeature = null;
		
		if(user == null)
		{
			ObjectNode result = Json.newObject();
			result.put("status", "KO");
		    result.put("message", "User not found");
		    return badRequest(result);
		}
		try {
			// Create a feature including subclasses from JSON :)
			ObjectMapper mapper = new ObjectMapper();
			newFeature = mapper.readValue(node.get("feature"), Feature.class);
			// Set the user reference
			newFeature.featureUser = user;
			// Set the reverse Tag reference
			Iterator<Tag> it = newFeature.featureTags.iterator();
			while(it.hasNext())
			{
				it.next().tagFeatures.add(newFeature);
			}
			// Add the feature to the user
			user.userFeatures.add(newFeature);
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
		} catch (javax.persistence.PersistenceException e) {
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

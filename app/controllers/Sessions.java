package controllers;

//http://flexjson.sourceforge.net/
import java.io.IOException;
import java.util.Iterator;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.avaje.ebean.Ebean;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

import play.libs.Json;
import play.mvc.Result;
import play.mvc.Controller;
import models.*;

/**
 * @author Richard Nesnass
 */
public class Sessions extends Controller
{
	public static Result fetchGeoFeaturesByTag(String hashTag)
	{
		JSONSerializer serializer = new JSONSerializer();
		Tag foundTag = Tag.find.where().eq("tag", hashTag).findUnique();
		// Include the features linked to this tag, but not the user details or the 'class' key
		return ok(serializer.include("tagFeatures").exclude("*.featureUser").exclude("*.class").serialize(foundTag));
	}
	
}
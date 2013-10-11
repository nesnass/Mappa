package controllers;

import helpers.SessionCollection;
import java.util.Iterator;
import java.util.List;
import org.codehaus.jackson.JsonNode;
import com.avaje.ebean.Ebean;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Controller;
import models.*;

/**
 * @author Richard Nesnass
 */
public class Sessions extends Controller
{	
	// POST /session
	// Create a new session, return the session id
	@BodyParser.Of(BodyParser.Json.class)
	public static Result createSession()
	{
		JsonNode node = request().body().asJson();
		Session newSession = new Session(node, "none");
		newSession.save();
		response().setContentType("application/json; charset=utf-8");
		String s = newSession.toJson();
		return ok(s);
	}
	
	//  GET /sessionlist?status=...
	//  Status determines whether clear or blacklisted sessions are retrieved
	public static Result getSessionList(String status) {
		SessionCollection sessionCollection = new SessionCollection();
		List<Session> sessionList = Session.find.where().eq("blacklisted", Boolean.valueOf(status)).orderBy("created_time desc").findList();
		if (sessionList == null || sessionList.size() == 0) {
			return ok("No sessions found");
		}
		sessionCollection.add(sessionList);
		response().setContentType("application/json; charset=utf-8");
		String s = sessionCollection.toJson();
		return ok(s);
	}
	
	//  GET /session?id=...
	public static Result getSessionByFBId(String fbid) {
		Session session = Session.find.where().eq("facebook_group_id", fbid).findUnique();
		if (session == null) {
			return ok("Session Not found");
		}
		response().setContentType("application/json; charset=utf-8");
		String s = session.toJson();
		return ok(s);
	}
	
	//  DELETE /session?id=...
	public static Result deleteSession(String fbid) {
		String str = "Session not found";
		Session s = Session.find.fetch("sessionFeatures").where().eq("facebook_group_id", fbid).findUnique();
		List<Feature> featureList = s.sessionFeatures;
		Iterator<Feature> it = featureList.iterator();
		while(it.hasNext())
		{
			Feature f = it.next();
			if(f != null) {
				f.updateTags(null);
				Ebean.delete(f);
				str = "POIs Deleted";
			}
		}
		Ebean.delete(s);
		response().setContentType("application/json; charset=utf-8");
		return ok(str);
	}
	
	//  PUT /session?id=...&status=...
	//  For blacklist - status == "true" | "false"
	public static Result updateSession(String fbid, String status) {
		Session s = Session.find.where().eq("facebook_group_id", fbid).findUnique();
		if (s != null) {
			s.setBlacklisted(Boolean.valueOf(status));
			s.save();
			response().setContentType("application/json; charset=utf-8");
			String js = s.toJson();
			return ok(js);	
		}
		return ok("Session Not found");
	}
	
}
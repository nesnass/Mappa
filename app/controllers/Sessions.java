package controllers;

import org.codehaus.jackson.JsonNode;
import play.mvc.Result;
import play.mvc.Controller;
import models.*;

/**
 * @author Richard Nesnass
 */
public class Sessions extends Controller
{	
	// POST /session/new
	// Create a new session, return the session id
	public static Result create()
	{
		JsonNode node = ctx().request().body().asJson();
		Session newSession = new Session(node);
		newSession.save();
		response().setContentType("text/html; charset=utf-8");
		String s = newSession.toJson();
		return ok(s);
	}
	
	//  GET /session/:id
	public static Result getSessionById(String id) {
		Session session = Session.find.byId(Long.valueOf(id));
		if (session == null) {
			return ok("Session Not found");
		}
		response().setContentType("text/html; charset=utf-8");
		String s = session.toJson();
		return ok(s);
	}
	
	
}
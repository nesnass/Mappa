package controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class Authorisation extends Controller
{
	private static String instagramCode = "";
	
	// ******** This method adapted, but seems incomplete??
	public static Result instagram(String code)
	{
		instagramCode = code;
		return ok();
	}
	
	public static String getInstagramCode() {
		return Authorisation.instagramCode;
	}
}

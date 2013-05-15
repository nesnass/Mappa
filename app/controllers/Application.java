package controllers;

import play.*;
import play.mvc.*;
import play.data.*;
import static play.data.Form.*;
import play.data.validation.Constraints.*;

import java.util.*;
import views.html.*;

public class Application extends Controller {
  
	
    /**
     * Describes the KML request form.
     */
    public static class Kml {
        @Required public String facebook_group_id;
    }
    
    
    public static Result index() {
     //   return ok(index.render("Your new application is ready."));
    	return ok();
    }
    
    /**
     * Handles the form submission.
     */
/*    public static Result getKml() {
        Form<Kml> form = form(Kml.class).bindFromRequest();
        if(form.hasErrors()) {
            return badRequest(index.render(form));
        } else {
            Kml data = form.get();
            return ok(
                kml.render(data.session_id)
            );
        }
    }
*/
  
}

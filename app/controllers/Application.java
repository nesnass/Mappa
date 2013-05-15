package controllers;

import play.*;
import play.mvc.*;
import play.data.*;
import static play.data.Form.*;
import play.data.validation.Constraints.*;

import java.util.*;

import models.Feature;
import models.MUser;
import models.Tag;
import views.html.*;

public class Application extends Controller {
  
	
    /**
     * Describes the KML request form.
     */
    public static class Kml {
        @Required public String facebook_group_id;
    }
    
    
    public static Result index() {
    	return ok();
 //       return ok(index.render("Your new application is ready."));
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
  
/*    
    public static Result clearDB() {
    	
    
    	
    	List<MUser> userList = MUser.find.all();
    	Iterator<MUser> it2 = userList.iterator();
    	MUser u;
    	while(it2.hasNext()) {
    		u = it2.next();
    		
        	Iterator<Feature> it = u.userFeatures.iterator();
        	Feature f;
        	while(it.hasNext()) {
        		f = it.next();
        		
        		Iterator<Tag> it3 = f.featureTags.iterator();
        		while(it3.hasNext())
        		{
        			it3.next().tagFeatures.remove(f);
        			it3.remove();
        		}
        		
        	//	f.featureMapper.delete();
        		
        	}
    		
    		
    		it2.remove();
    	}
    	
    	return ok();
    }
 */   
}

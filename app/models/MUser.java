package models;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import play.data.validation.Constraints;
import play.db.ebean.Model;


/**
 * @author Richard Nesnass
 */

@Entity
@Table(name="s_user")
public class MUser extends Model
{
	private static final long serialVersionUID = 7448411543574042349L;

	@Id
	@GeneratedValue
	public long id;
	
	public long facebook_id;
	
	@Constraints.MaxLength(255)
	public String full_name;
	
	@Constraints.MaxLength(255)
	public String profile_picture;
	
	public long location_0;
	
	public long location_1;
	
	@OneToMany(mappedBy="featureUser", cascade=CascadeType.ALL)
	public List<Feature> userFeatures = new ArrayList<Feature>();
	
	public MUser() {
		// TODO Auto-generated constructor stub
	}
	
	public MUser(String full_name) {
		this();
		this.full_name = full_name;
	}
	
	public MUser(long fbid, String full_name) {	
		this();
		this.facebook_id = fbid;
		this.full_name = full_name;
	}
	
	public MUser(long fbid, String full_name, String profile_picture) {	
		this(fbid, full_name);
		this.profile_picture = profile_picture;
	}
	
	public static Model.Finder<Long, MUser> find =  new Model.Finder<Long, MUser>(Long.class, MUser.class);
	
	public static List<MUser> all() {
		return find.all();
	}
	
	public String toString()
	{
		return this.full_name + " #features: " + userFeatures.size();
	}
	
	// Created to map the json output matching the implementation currently running on client (client cannot be changed at this time)
	public String toJson()
	{
		String jsonString = 
				
			"{ \"location\" : [" + String.valueOf(this.location_0) +
							"," + String.valueOf(this.location_1) +
						"],\"id\" : \"" + String.valueOf(this.facebook_id) +
						"\",\"full_name\" : \"" + this.full_name + "\"}";

		return jsonString;
	}

}

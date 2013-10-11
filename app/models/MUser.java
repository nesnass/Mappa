package models;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;

import flexjson.JSON;

import play.data.validation.Constraints;
import play.db.ebean.Model;


/**
 * @author Richard Nesnass
 */

@Entity
@Table(name="s_user")
public class MUser extends Model {
	private static final long serialVersionUID = 7448411543574042349L;

	@Id
	@GeneratedValue
	private long id;
	
	@Constraints.MaxLength(255)
	private String facebook_id;
	
	@Constraints.MaxLength(255)
	public String full_name;
	
	@Constraints.MaxLength(255)
	public String profile_picture;

	@Constraints.MaxLength(255)
	private String username = "";
	
	private double lng;
	private double lat;

	@Transient
	private double[] location = new double[2];
	
	@OneToMany(mappedBy="featureUser")
	public List<Feature> userFeatures = new ArrayList<Feature>();
	
	public MUser() {
		// TODO Auto-generated constructor stub
	}
	
	public MUser(String facebook_id, String full_name, String username, String profile_picture) {	
		this();
		this.facebook_id = facebook_id;
		this.full_name = full_name;
		if(username != null)
			this.username = username;
		if(profile_picture != null)
			this.profile_picture = profile_picture;
	}
	
	public String getId() {
		return String.valueOf(id);
	}

	@JSON(include=true)
	public double[] getLocation() {
		if(lat == 0 && lng == 0)
			return null;
		location[0] = lng;
		location[1] = lat;
		return location;
	}
	
	@JSON(include=false)
	public double getLng() {
		return lng;
	}
	@JSON(include=false)
	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public void setLng(double lng) {
		this.lng = lng;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getFacebook_id() {
		return facebook_id;
	}

	public void setFacebook_id(String facebook_id) {
		this.facebook_id = facebook_id;
	}

	public static Model.Finder<Long, MUser> find =  new Model.Finder<Long, MUser>(Long.class, MUser.class);
	
	public static List<MUser> all() {
		return find.all();
	}
	
	public String toString() {
		return this.full_name + " #features: " + userFeatures.size();
	}
	
	// Created to map the json output matching the implementation currently running on client (client cannot be changed at this time)
	public String toJson() {
		return "";
	}
}

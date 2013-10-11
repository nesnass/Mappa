package models;

import helpers.MyConstants;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

import org.codehaus.jackson.JsonNode;

import com.avaje.ebean.Ebean;

import flexjson.JSON;
import play.db.ebean.Model;

/**
 * @author Richard Nesnass
 */

@Entity
@Table(name="s_session")
public class Session extends Model {
	private static final long serialVersionUID = 431420329862242291L;

	@Id
	@GeneratedValue
	private long id;
	
	private String facebook_group_id;
	
	// This gives a weak link to a MUser entry (MUser.facebook_id)
	private String facebook_creator_id;
	
	@Column(columnDefinition = "TEXT")
	private String stitle;
	
	@Column(columnDefinition = "TEXT")
	private String sdescription;	
	
	private String privacy;
	
	private Boolean blacklisted = false;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date created_time;
	
	@OneToMany(mappedBy="featureSession")
	public List<Feature> sessionFeatures = new ArrayList<Feature>();
	
	public static Model.Finder<Long, Session> find = new Model.Finder<Long, Session>(Long.class, Session.class);

	public Session() {
		created_time = new Date();
	}
	
	public Session(JsonNode node, String source_type) {
		this();
		JsonNode session, user;
		if(source_type.equalsIgnoreCase("none")) {
			session = node.get("session");
			user = node.get("user");
		}
		else if(source_type.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPA.toString())) {
			session = node.get("properties").path("sessions").path(0);
			user = node.get("properties").path("user");
		}
		else {
			session = node.get("properties").path("sessions").path(0);
			user = node.get("properties").path("mapper");
		}
		assignProperties(session, user);
	}
	
	public void assignProperties(JsonNode session, JsonNode user) {
		facebook_group_id = session.get("id").asText();
		stitle = session.path("name").asText();
		sdescription = session.path("description").asText();
		privacy = session.path("privacy").asText();

		// When sessions are created exclusively, this lets us track the user creating them
		facebook_creator_id = user.get("id").asText();
		MUser muser = MUser.find.where().eq("facebook_id", facebook_creator_id).findUnique();
		if(muser == null) {
			muser = new MUser(facebook_creator_id, user.get("full_name").asText(), user.path("username").asText(), user.path("profile_picture").asText());
			Ebean.save(muser);
		}
	}
	
	@JSON(include=true)
	public String getId() {
			return String.valueOf(this.id);
	}
	
	public String getFacebook_group_id() {
		return facebook_group_id;
	}

	public void setFacebook_group_id(String facebook_group_id) {
		this.facebook_group_id = facebook_group_id;
	}

	@JSON(include=false)
	public String getTitle() {
		return stitle;
	}
	@JSON(include=false)
	public void setTitle(String stitle) {
		this.stitle = stitle;
	}
	
	public String getFacebook_creator_id() {
		return facebook_creator_id;
	}

	public void setFacebook_creator_id(String facebook_creator_id) {
		this.facebook_creator_id = facebook_creator_id;
	}

	public String getPrivacy() {
		return privacy;
	}

	public void setPrivacy(String privacy) {
		this.privacy = privacy;
	}
	
	public Boolean getBlacklisted() {
		return blacklisted;
	}

	public void setBlacklisted(Boolean bl) {
		this.blacklisted = bl;
	}

	public String getDescription() {
		return sdescription;
	}
	
	// Convert Date in milliseconds to Unix timestamp in seconds
	public Date getCreated_time() {
		return created_time;
	}
	
	@JSON(include=false)
	public long getCreated_timeUNIX() {
		return created_time.getTime()/1000;
	}

	public void setDescription(String sdescription) {
		this.sdescription = sdescription;
	}

	public long retrieveId() {
		return this.id;
	}
	
	// Created to map the json output matching the implementation currently running on client (client cannot be changed at this time)
	public String toJson() {
		String jsonString = 
			"{ 	\"id\" : \"" + String.valueOf(this.id) +
				"\",\"facebook_group_id\" : \"" + this.facebook_group_id +
				"\",\"title\" : \"" + this.stitle +
				"\",\"description\" : \"" + this.sdescription + "\"}";
		return jsonString;
	}
}

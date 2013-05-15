package models;

import javax.persistence.*;

import org.codehaus.jackson.JsonNode;

import play.data.validation.Constraints;
import play.db.ebean.Model;

/**
 * @author Richard Nesnass
 */

@Entity
@Table(name="s_session")
public class Session extends Model
{
	private static final long serialVersionUID = 431420329862242291L;

	@Id
	@GeneratedValue
	public long id;
	
	private long facebook_group_id;
	
	private long facebook_creator_id;
	
	@Constraints.MaxLength(255)
	private String stitle;
	
	@Constraints.MaxLength(255)
	private String sdescription;	
	
	public static Model.Finder<Long, Session> find = new Model.Finder<Long, Session>(Long.class, Session.class);

	public Session()
	{
	}
	
	public Session(JsonNode session)
	{
		this();
		assignProperties(session);
	}
	
	public void assignProperties(JsonNode session)
	{
		facebook_group_id = session.get("facebook_group_id").asLong();
		facebook_creator_id = session.path("facebook_creator_id").asLong();
		stitle = session.get("title").asText();
		sdescription = session.get("description").asText();
	}
	
	public long getFacebook_group_id() {
		return facebook_group_id;
	}

	public void setFacebook_group_id(long facebook_group_id) {
		this.facebook_group_id = facebook_group_id;
	}

	public String getTitle() {
		return stitle;
	}

	public void setTitle(String stitle) {
		this.stitle = stitle;
	}

	public long getFacebook_creator_id() {
		return facebook_creator_id;
	}

	public void setFacebook_creator_id(long facebook_creator_id) {
		this.facebook_creator_id = facebook_creator_id;
	}

	public String getDescription() {
		return sdescription;
	}

	public void setDescription(String sdescription) {
		this.sdescription = sdescription;
	}

	// Created to map the json output matching the implementation currently running on client (client cannot be changed at this time)
	public String toJson()
	{
		String jsonString = 
				
			"{ 	\"id\" : \"" + String.valueOf(this.id) +
				"\",\"facebook_group_id\" : \"" + this.facebook_group_id +
				"\",\"title\" : \"" + this.stitle +
				"\",\"description\" : \"" + this.sdescription + "\"}";

		return jsonString;
	}
}

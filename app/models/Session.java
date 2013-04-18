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
	
	public long facebook_group_id;
	
	@Constraints.MaxLength(255)
	public String title;
	
	@Constraints.MaxLength(255)
	public String description;
	
	public static Model.Finder<Long, Session> find = new Model.Finder<Long, Session>(Long.class, Session.class);

	public Session()
	{
	}
	
	public Session(JsonNode session)
	{
		this();
		setProperties(session);
	}
	
	public void setProperties(JsonNode session)
	{
		facebook_group_id = session.get("facebook_group_id").asLong();
		title = session.get("title").asText();
		description = session.get("description").asText();
	}
	
	// Created to map the json output matching the implementation currently running on client (client cannot be changed at this time)
	public String toJson()
	{
		String jsonString = 
				
			"{ 	\"id\" : \"" + String.valueOf(this.id) +
				"\",\"facebook_group_id\" : \"" + this.facebook_group_id +
				"\",\"title\" : \"" + this.title +
				"\",\"description\" : \"" + this.description + "\"}";

		return jsonString;
	}
}

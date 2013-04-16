package models;

import javax.persistence.*;
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
}

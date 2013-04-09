package models;

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
	
	@Constraints.MaxLength(255)
	public String full_name;
	
	@Constraints.MaxLength(255)
	public String profile_picture;
	
	public long location_0;
	
	public long location_1;
	
	@OneToMany(mappedBy="featureUser", cascade=CascadeType.ALL)
	public List<Feature> userFeatures;
	
	public MUser() {
		// TODO Auto-generated constructor stub
	}
	
	public MUser(String full_name) {
		this();
		this.full_name = full_name;
	}
	
	public MUser(String full_name, String profile_picture) {	
		this();
		this.full_name = full_name;
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

}

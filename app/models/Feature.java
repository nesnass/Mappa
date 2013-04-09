package models;


import java.util.Date;
import java.util.List;
import javax.persistence.*;
import play.data.validation.*;
import play.db.ebean.Model;

/**
 * @author Richard Nesnass
 */

@Entity
@Table(name="s_feature")
public class Feature extends Model
{
	private static final long serialVersionUID = 6285870362122377542L;
	
	@Id
	@GeneratedValue
	private long id;

	@Embedded()
	public Geometry geometry;
	
	@ManyToOne()
	public MUser featureUser;
	
	@ManyToMany(cascade=CascadeType.ALL)
	public List<Tag> featureTags;

	@Constraints.MaxLength(30)
	public String type = "Feature";
	
	@Temporal(TemporalType.TIMESTAMP)
	public Date created_time;
	
	@Constraints.MaxLength(255)
	public String descr_url;
	
	@Constraints.MaxLength(255)
	public String description;
	
	@Constraints.MaxLength(255)
	public String icon_url;
	
	@Constraints.MaxLength(255)
	public String image_url_high_resolution;
	
	@Constraints.MaxLength(255)
	public String image_url_standard_resolution;
	
	@Constraints.MaxLength(255)
	public String image_url_thumbnail;

	@Constraints.MaxLength(255)
	public String name;
	
	@Constraints.MaxLength(30)
	public String source_type;
	

	public Feature() {
		created_time = new Date();
	//	tags = new HashSet<Tag>();
	}
	
	public Feature(Geometry geometry) {
		this();
		this.geometry = geometry;
	}

	public static Model.Finder<String, Feature> find = new Model.Finder<String, Feature>(String.class, Feature.class);
	
	public String toString()
	{
		return this.name;
	}
}
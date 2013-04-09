package models;

import java.util.List;
import java.util.Set;
import play.db.ebean.*;
import javax.persistence.*;
import play.data.validation.Constraints;

/**
 * @author Richard Nesnass
 */
	
@Entity
@Table(name="s_tag")
public class Tag extends Model
{
	private static final long serialVersionUID = -9167380498437464027L;
	
	@Id
	@GeneratedValue
	protected long id;

	@Constraints.MaxLength(50)
	@Column(unique=true)
	public String tag;

	@ManyToMany()
	public Set<Feature> tagFeatures;

	public Tag()
	{
	//	features = new HashSet<Feature>();
	}
	
	public Tag(String tag)
	{
		this();
		this.tag = tag;
	}
	
	public static Finder<Long, Tag> find = new Finder<Long, Tag>(Long.class, Tag.class);
	
    public static List<Tag> all() {
    	return find.all();
    }

    public static void delete(long id) {
        find.ref(id).delete();
    }
  
}

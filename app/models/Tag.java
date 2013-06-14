package models;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import play.db.ebean.*;
import javax.persistence.*;

import flexjson.JSON;

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
	private long id;

	@Constraints.MaxLength(50)
	@Column(unique=true)
	private String tag;

	@ManyToMany(cascade=CascadeType.ALL)
	public Set<Feature> tagFeatures = new HashSet<Feature>();

	public Tag()
	{
	//	features = new HashSet<Feature>();
	}
	
	public Tag(String tag)
	{
		this();
		this.tag = tag;
	}
	
	public String toString() {
		return tag;
	}

	public long retrieveId(){
		return id;
	}
	
	@JSON(include=false)
	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
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

package models;

import java.util.List;
import javax.persistence.*;
import play.db.ebean.*;

/**
 * @author Muhammad Fahied & Richard Nesnass
 */

@Entity
public class HashTagTable extends Model
{
	private static final long serialVersionUID = -1923186650582749448L;

	@Id
	public long id;
	
	public String hashTag;
	
	public List<Feature> features;
	
	public static Model.Finder<String, HashTagTable> find = new Model.Finder<String, HashTagTable>(String.class, HashTagTable.class);
	
	public HashTagTable() {
		// TODO Auto-generated constructor stub
	}
	
	public HashTagTable(String hashTag) {
		this.hashTag = hashTag;
	}
	
//	public static HashTagTable byTag(String hashTag) {
//        return find.field("hashTag").equal(hashTag).get();
//   }
	
	public void removeFeature(Feature feature)
	{
		this.features.remove(feature);
		this.update();
	}

}

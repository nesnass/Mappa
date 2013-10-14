package models.geometry;


import javax.persistence.*;

import org.codehaus.jackson.JsonNode;
import org.jinstagram.entity.common.Location;

import play.db.ebean.Model;

/**
 * @author Muhammad Fahied & Richard Nesnass
 */

@Embeddable
public class Geometry extends Model
{
	private static final long serialVersionUID = -9167380498437464027L;
	
	private Point coordinates;
	
	public static Model.Finder<Long, Geometry> find = new Model.Finder<Long, Geometry>(Long.class, Geometry.class);
	
}

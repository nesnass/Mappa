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
	
	@Id
	@GeneratedValue
	private long id; 
	
	// longitude
	public double coordinate_0;
	
	// latitude
	public double coordinate_1;

	// altitude
	public double coordinate_2;
	
	@Transient
	public String type = "Point";
	
	public Geometry()
	{
	}
	
	public Geometry(double longtitude, double latitude)
	{
		coordinate_0 = longtitude;
		coordinate_1 = latitude;
	}
	
	public Geometry(double longtitude, double latitude, double altitude)
	{
		coordinate_0 = longtitude;
		coordinate_1 = latitude;
		coordinate_2 = altitude;
	}
	
	public Geometry(String type)
	{
		this();
		this.type = type;
	}
	
	public Geometry(JsonNode geometry)
	{
		this();
		setProperties(geometry);
	}
	
	public Geometry(Location location)
	{
		this();
		setProperties(location);
	}
	
	// Setup by JsonNode object
	public void setProperties(JsonNode geometry)
	{
		coordinate_0 = geometry.get("coordinates").path(0).asDouble();
		coordinate_1 = geometry.get("coordinates").path(1).asDouble();
		coordinate_2 = geometry.get("coordinates").path(1).asDouble();
		type = geometry.get("type").asText();
	}
	
	// Setup by jInstagram Location object
	public void setProperties(Location location)
	{
		coordinate_0 = location.getLongitude();
		coordinate_1 = location.getLatitude();
	}
	
	public static Model.Finder<Long, Geometry> find = new Model.Finder<Long, Geometry>(Long.class, Geometry.class);
	
}

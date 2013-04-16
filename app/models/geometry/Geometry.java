package models.geometry;


import javax.persistence.*;

import org.codehaus.jackson.JsonNode;

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
	
	public double coordinate_0;
	
	public double coordinate_1;
	
	@Transient
	public String type = "Point";
	
	public Geometry()
	{
	}
	
	public Geometry(double lon, double lat)
	{
		coordinate_0 = lon;
		coordinate_1 = lat;
	}
	public Geometry(String type)
	{
		this();
		this.type = type;
	}
	public Geometry(JsonNode geometry)
	{
		coordinate_0 = geometry.get("coordinates").get(0).asDouble();
		coordinate_1 = geometry.get("coordinates").get(1).asDouble();
		type = geometry.get("type").asText();
	}
	
	public static Model.Finder<String, Geometry> find = new Model.Finder<String, Geometry>(String.class, Geometry.class);
	

	
}

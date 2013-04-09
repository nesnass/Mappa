package models;


import javax.persistence.*;

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
	
	public float coordinate_0;
	
	public float coordinate_1;
	
	@Transient
	public String type;
	
	public Geometry()
	{
	}
	
	public Geometry(String type)
	{
		this();
		this.type = type;
	}
}

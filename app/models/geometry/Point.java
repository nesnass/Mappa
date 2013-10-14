package models.geometry;

import javax.persistence.Embeddable;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.codehaus.jackson.JsonNode;
import org.jinstagram.entity.common.Location;

import flexjson.JSON;

import play.db.ebean.Model;

@Embeddable
public class Point extends Model {

	@Id
	@GeneratedValue
	private long id;
	
	private String gtype = "Point";
	private double lng;
	private double lat;
	
	@Transient
	private double[] coordinates;

	private static final long serialVersionUID = 3913464290289955353L;

	public Point() {
		coordinates = new double[2];
	}

	public Point(double lng, double lat) {
		this();
		this.lng = lng;
		this.lat = lat;

		coordinates[0] = lng;
		coordinates[1] = lat;
	}

	public Point(JsonNode pointNode) {
		this();
		assignProperties(pointNode);
	}

	public Point(Location location) {
		this();
		assignProperties(location);
	}

	public String getType() {
		return gtype;
	}
	
	@JSON(include=false)
	public double getLng() {
		return lng;
	}

	public void setLng(double lng) {
		this.lng = lng;
	}

	@JSON(include=false)
	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public void assignProperties(JsonNode pointNode) {
		coordinates = new double[pointNode.get("coordinates").size()];
		lng = pointNode.get("coordinates").path(0).asDouble();
		lat = pointNode.get("coordinates").path(1).asDouble();
		coordinates[0] = lng;
		coordinates[1] = lat;
	}

	// Setup by jInstagram Location object
	public void assignProperties(Location location)
	{
		coordinates = new double[2];
		lng = location.getLongitude();
		lat = location.getLatitude();
		coordinates[0] = lng;
		coordinates[1] = lat;
	}

	/**
	 * @return the coordinates
	 */
	public double[] getCoordinates() {
		this.coordinates[0] = lng;
		this.coordinates[1] = lat;
		return coordinates;
	}

	/**
	 * @param coordinates the coordinates to set
	 */
	public void setCoordinates(double[] coordinates) {
		this.coordinates[0] = lng;
		this.coordinates[1] = lat;
		this.coordinates = coordinates;
	}
	
	/**
	 * @param determine equal to another Point?
	 */
	public boolean equals(Point otherPoint) {
		if(this.lng == otherPoint.coordinates[0] && this.lat == otherPoint.coordinates[1])
			return true;
		else
			return false;
	}

}

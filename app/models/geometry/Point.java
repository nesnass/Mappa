package models.geometry;

import javax.persistence.Embeddable;
import javax.persistence.Transient;

import org.codehaus.jackson.JsonNode;
import org.jinstagram.entity.common.Location;

import play.db.ebean.Model;

@Embeddable
public class Point extends Model {

	public String type = "Point";
	public double lng;
	public double lat;

	@Transient
	private double[] coordinates;

	private static final long serialVersionUID = 3913464290289955353L;

	public Point() {
	}

	public Point(double lng, double lat) {
		this.lng = lng;
		this.lat = lat;
	}

	public Point(JsonNode pointNode) {
		this();
		setProperties(pointNode);
	}

	public Point(Location location) {
		this();
		setProperties(location);
	}

	public void setProperties(JsonNode pointNode) {

		lng = pointNode.get("coordinates").path(0).asDouble();
		lat = pointNode.get("coordinates").path(0).asDouble();

	}



	// Setup by jInstagram Location object
	public void setProperties(Location location)
	{
		lng = location.getLongitude();
		lat = location.getLatitude();
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

	public static Model.Finder<Long, Geometry> find = new Model.Finder<Long, Geometry>(
			Long.class, Geometry.class);

}

package models.geometry;

import java.io.IOException;

import javax.persistence.Embeddable;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import play.db.ebean.Model;

@Embeddable
public class LineString extends Model {

	private static final long serialVersionUID = -6321118276175787909L;

	@Id
	@GeneratedValue
	private long id;
	
	private String gtype = "LineString";
	
	@Transient
	private Point[] lineString;
	
	public LineString(int length) {
		lineString = new Point[length];
	}

	/**
	 * @return set a point somewhere along the lineString
	 */
	public void setPoint(Point p, int index) {
		if(lineString.length > 0 && index < lineString.length) {
			lineString[index] = p;
		}
	}

	public String getType() {
		return gtype;
	}

	/**
	 * @return assign a lineString from a JSON String node
	 */
	public void assignProperties(String lineNodeString) {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode coordinates;
		try {
			coordinates = mapper.readTree(lineNodeString).get("coordinates");
			if(coordinates.isArray())
				lineString = mapper.readValue(coordinates, Point[].class);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * @return determine whether this is a linear ring by the GeoJSON definition      http://geojson.org/geojson-spec.html#id16
	 */
	public boolean isLinearRing() {
		if(this.lineString.length > 3 && lineString[0].equals(lineString[lineString.length-1]))
			return true;
		else
			return false;
	}

	/**
	 * @return the lineString
	 */
	public Point[] getLineString() {
		return lineString;
	}

	/**
	 * @param set the lineString
	 */
	public void setLineString(Point[] newLineString) {
		this.lineString = newLineString;
	}

}

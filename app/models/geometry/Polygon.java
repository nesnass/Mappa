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
public class Polygon extends Model {

	private static final long serialVersionUID = 3389015017719674584L;

	@Id
	@GeneratedValue
	private long id;
	
	private String gtype = "Polygon";
	
	@Transient
	private LineString[] polygon;
	
	public Polygon(int length) {
		polygon = new LineString[length];
	}

	/**
	 * @return set a lineString 
	 */
	public void setLineStringInPolygon(LineString ls, int index) {
		if(polygon.length > 0 && index < polygon.length) {
			polygon[index] = ls;
		}
	}

	public String getType() {
		return gtype;
	}

	/**
	 * @return assign a lineString from a JSON String node
	 */
	public void assignProperties(String polygonString) {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode coordinates;
		try {
			coordinates = mapper.readTree(polygonString).get("coordinates");
			if(coordinates.isArray())
				polygon = mapper.readValue(coordinates, LineString[].class);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @return the polygon
	 */
	public LineString[] getLineString() {
		return polygon;
	}

	/**
	 * @param set the lineString
	 */
	public void setLineString(LineString[] newPolygon) {
		this.polygon = newPolygon;
	}

}

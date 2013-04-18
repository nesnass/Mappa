package models;

import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.persistence.*;
import org.codehaus.jackson.JsonNode;
import external.MyConstants;
import models.geometry.Geometry;
import play.data.validation.*;
import play.db.ebean.Model;

/**
 * @author Richard Nesnass
 */

@Entity
@Table(name="s_feature")
public class Feature extends Model implements Comparator<Feature>
{
	private static final long serialVersionUID = 6285870362122377542L;
	
	@Id
	@GeneratedValue
	public long id;

	@Embedded()
	public Geometry featureGeometry;

	@ManyToOne()
	public MUser featureUser;
	
	@ManyToOne()
	public MUser mapperUser;

	@ManyToOne()
	public Session featureSession;
	
	@OneToOne(cascade=CascadeType.ALL)
	public S3File imageStandardResolution;
	
	@OneToOne(cascade=CascadeType.ALL)
	public S3File imageThumbnail;

	@ManyToMany(cascade=CascadeType.ALL)
	public Set<Tag> featureTags = new HashSet<Tag>();

	@Constraints.MaxLength(30)
	public String type = "Feature";
	
	@Temporal(TemporalType.TIMESTAMP)
	public Date created_time;
	
	@Constraints.MaxLength(255)
	public String descr_url = "";
	
	@Constraints.MaxLength(255)
	public String description = "";
	
	@Constraints.MaxLength(255)
	public String mapper_description = "";

	@Constraints.MaxLength(255)
	public String icon_url = "";

	@Constraints.MaxLength(255)
	public String name = "";
	
	@Constraints.MaxLength(30)
	public MyConstants.FeatureStrings source_type = MyConstants.FeatureStrings.OVERLAY;

	public Feature() {
		created_time = new Date();
	//	tags = new HashSet<Tag>();
	}
	
	public Feature(Geometry geometry) {
		this();
		this.featureGeometry = geometry;
	}
	
	// Create a new feature given a JSON node
	public Feature(JsonNode featureNode) {
		this();
		setProperties(featureNode);
	}
	
	public void setProperties(JsonNode featureNode)
	{
		// Set regular parameters
		if(this.featureGeometry == null)
			this.featureGeometry = new Geometry(featureNode.get("geometry"));
		else
			this.featureGeometry.setProperties(featureNode.get("geometry"));
		this.type = featureNode.get("type").asText();
		this.description = featureNode.get("properties").get("description").asText();

		String source = featureNode.get("properties").get("source_type").asText();
		if (source.equalsIgnoreCase(MyConstants.FeatureStrings.OVERLAY.toString()))
			this.source_type = MyConstants.FeatureStrings.OVERLAY;
		else if (source.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString()))
			this.source_type = MyConstants.FeatureStrings.MAPPED_INSTAGRAM;
		this.name = featureNode.get("properties").path("name").getTextValue();
		// Set source dependent parameters
		switch(this.source_type)
		{
			case OVERLAY :
				break;
			
			case MAPPED_INSTAGRAM:
				// 'name' not included in regular 'Overlay' feature??  '.path' call is used to return a 'missing node' instead of null if node not found
				this.mapper_description = featureNode.get("properties").path("mapper_description").getTextValue();
				this.icon_url = MyConstants.FEATURE_SERVER_NAME_PORT + "/assets/img/mInsta.png";
				
				// ******** Image URLs should be added here. Are they included in the MAPPED_INSTAGRAM JSON request?
				
				break;
		}
		
		// Set the Tag references, if any tags exist
		Iterator<JsonNode> tagsIterator = featureNode.get("properties").path("tags").iterator();
		while(tagsIterator.hasNext())
		{
			addTag(tagsIterator.next().getTextValue());
		}
	}
	
	public void addTag(String theTag)
	{
		Tag newTag = Tag.find.where().eq("tag", theTag).findUnique();
		if(newTag == null)
			newTag = new Tag(theTag);

		this.featureTags.add(newTag);
		newTag.tagFeatures.add(this);
		//newTag.save();
	}
	
	public void deleteImages()
	{
		this.imageStandardResolution.delete();
		this.imageThumbnail.delete();
		imageStandardResolution.delete();
		imageThumbnail.delete();
	}
	
	public static Model.Finder<Long, Feature> find = new Model.Finder<Long, Feature>(Long.class, Feature.class);
	
	public String toString()
	{
		return name;
	}
	
	// Retrieve the icon URL 
	public String getIconURL() {
		if(this.source_type == MyConstants.FeatureStrings.OVERLAY)
			return MyConstants.FEATURE_SERVER_NAME_PORT + "/assets/img/overlay.png";
		else if(this.source_type == MyConstants.FeatureStrings.MAPPED_INSTAGRAM)
			return MyConstants.FEATURE_SERVER_NAME_PORT + "/assets/img/mInsta.png";
		else return "";
	}
	
	// Retrieve the description URL
	public String getDescriptionURL() {
		return MyConstants.FEATURE_SERVER_NAME_PORT + "/content/" + this.id;
	}
	
	// Uses pythagoras to calculate the distance apart in terms of coordinates 
	public double getDistance(Feature other) {
		final double dx = this.featureGeometry.coordinate_0-other.featureGeometry.coordinate_0; 
        final double dy = this.featureGeometry.coordinate_1-other.featureGeometry.coordinate_1;
        return Math.sqrt(dx*dx + dy*dy);
	}

	@Override
	public int compare(Feature arg0, Feature arg1) {
		final double distanceDelta = getDistance(arg0) - getDistance(arg1); 
        return distanceDelta < 0 ? -1 : 1; 
	}
	
	// Created to map the json output matching the implementation currently running on client (client cannot be changed at this time)
	public String toJson()
	{
		// Get the listing of tags for this feature
		String tagJson = "[";
		Iterator<Tag> it = featureTags.iterator();
		while(it.hasNext())
		{
			tagJson += "\"" + it.next().tag + "\"";
			if(it.hasNext())
				tagJson+= ",";
		}
		tagJson +="]";
		
		String jsonString = 
				
			"{" +
					"\"id\" : \"" + String.valueOf(this.id) + "\"," +
					"\"geometry\" : {" +
										"\"type\" : \"" + this.featureGeometry.type + "\"," +
										"\"coordinates\" : [" + String.valueOf(this.featureGeometry.coordinate_0) +
					"," + String.valueOf(this.featureGeometry.coordinate_1) + 
				"]},\"properties\" : {" +
					"\"images\" : { \"thumbnail\" : \"" + this.imageThumbnail.getUrlAsString() +
									"\",\"standard_resolution\" : \"" + this.imageStandardResolution.getUrlAsString() +
					"\"},\"created_time\" : \"" + this.created_time.toString() +
					"\",\"source_type\" : \"" + this.source_type +
					"\",\"icon_url\" : \"" + this.getIconURL() +
					"\",\"desc_url\" : \"" + this.getDescriptionURL() +
					"\",\"description\" : \"" + this.description +
					"\",\"name\" : \"" + this.name;
			jsonString+=		"\",\"session\" : " + this.featureSession.toJson();
			jsonString+=		"\",\"user\" : " + this.featureUser.toJson() +
					",\"tags\" : " + tagJson +
				"}" +
			"}";

		return jsonString;
	}
}
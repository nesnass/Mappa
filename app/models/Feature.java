package models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.persistence.*;
import org.codehaus.jackson.JsonNode;
import org.jinstagram.entity.users.feed.MediaFeedData;

import helpers.GeoCalculations;
import helpers.MyConstants;
import models.geometry.Geometry;
import parsers.TwitterParser;
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

	public String alternate_id;
	
	@Embedded()
	public Geometry featureGeometry;

	@ManyToOne()
	public MUser featureUser;
	
	@ManyToOne()
	public MUser mapperUser;

	@ManyToOne()
	public Session featureSession;
	
	@OneToOne(cascade=CascadeType.ALL)
	public S3File imageStandardResolutionFile;

	@OneToOne(cascade=CascadeType.ALL)
	public S3File imageThumbnailFile;

	@Constraints.MaxLength(255)
	public String imageStandardResolutionURL = "";
	
	@Constraints.MaxLength(255)
	public String imageThumbnailURL = "";
	
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
	}
	
	// Construct a new feature given Geometry
	public Feature(Geometry geometry) {
		this();
		this.featureGeometry = geometry;
	}
	
	// Construct a new feature given a JSON node
	public Feature(JsonNode featureNode) {
		this();
		setProperties(featureNode);
	}
	
	// Setup by JsonNode object
	public void setProperties(JsonNode featureNode)
	{
		// Set relations
		if(this.featureGeometry == null)
			this.featureGeometry = new Geometry(featureNode.get("geometry"));
		else
			this.featureGeometry.setProperties(featureNode.get("geometry"));
		this.featureSession = Session.find.byId(featureNode.get("properties").get("session_id").asLong());

		// *************  Session should always be supplied in the JSON. This case should be removed when sessions are enabled
		if(this.featureSession == null) {
			Session newSession = new Session();

			newSession.facebook_group_id = 0;
			newSession.stitle = "Test Session Title";
			newSession.sdescription = "Test session Description";
			newSession.save();

			this.featureSession = newSession;
		}

		// Set regular parameters
		this.type = featureNode.get("type").asText();
		this.description = featureNode.get("properties").get("description").asText();

		String source = featureNode.get("properties").get("source_type").asText();
		if (source.equalsIgnoreCase(MyConstants.FeatureStrings.OVERLAY.toString()))
			this.source_type = MyConstants.FeatureStrings.OVERLAY;
		else if (source.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString()))
			this.source_type = MyConstants.FeatureStrings.MAPPED_INSTAGRAM;
		//this.name = featureNode.get("properties").path("name").getTextValue();

		Set<String> foundTags = new HashSet<String>();
		// Set source dependent parameters
		switch(this.source_type)
		{
		case OVERLAY :
			foundTags = TwitterParser.searchHashTags(this.description);
			break;

		case INSTAGRAM:
			break;

		case MAPPED_INSTAGRAM:
			// 'name' not included in regular 'Overlay' feature??  '.path' call is used to return a 'missing node' instead of null if node not found
			this.mapper_description = featureNode.get("properties").path("mapper_description").getTextValue();
			this.icon_url = MyConstants.FEATURE_SERVER_NAME_PORT + "/assets/img/mInsta.png";
			foundTags = TwitterParser.searchHashTags(this.mapper_description);

			// ******** Image URLs should be added here. Are they included in the MAPPED_INSTAGRAM JSON request?

			break;
		}

		// Set the Tag references, if any tags exist
		// Look through the description for tags
		Iterator<JsonNode> tagsIteratorFromNode = featureNode.get("properties").path("tags").iterator();
		while(tagsIteratorFromNode.hasNext())
		{
			foundTags.add(tagsIteratorFromNode.next().getTextValue());
		}
		// Add unique and non-existing tags to the database
		Iterator<String> tagsIteratorAllTags = foundTags.iterator();
		while(tagsIteratorAllTags.hasNext())
		{
			addTag(tagsIteratorAllTags.next());
		}

	}
	
	// Setup by jInstagram MediaFeedData object
	public void setProperties(MediaFeedData jInstagramMedia)
	{
		// Set regular parameters
		if(this.featureGeometry == null)
			this.featureGeometry = new Geometry(jInstagramMedia.getLocation());
		else
			this.featureGeometry.setProperties(jInstagramMedia.getLocation());
		this.type = "INSTAGRAM";
		this.alternate_id = jInstagramMedia.getId();
		this.description = jInstagramMedia.getCaption().getText();
		this.source_type = MyConstants.FeatureStrings.INSTAGRAM;
		this.imageThumbnailURL = jInstagramMedia.getImages().getThumbnail().getImageUrl();
		this.imageStandardResolutionURL = jInstagramMedia.getImages().getStandardResolution().getImageUrl();
		SimpleDateFormat sdf = new SimpleDateFormat();
		try {
			this.created_time.setTime(sdf.parse(jInstagramMedia.getCreatedTime()).getTime());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Set the Tag references, if any tags exist
		Iterator<String> tagsIterator = jInstagramMedia.getTags().iterator();
		while(tagsIterator.hasNext())
		{
			addTag(tagsIterator.next());
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
		this.imageStandardResolutionFile.delete();
		this.imageThumbnailFile.delete();
		this.imageStandardResolutionURL = "";
		this.imageThumbnailURL = "";
		imageStandardResolutionFile.delete();
		imageThumbnailFile.delete();
	}
	
	public static Model.Finder<Long, Feature> find = new Model.Finder<Long, Feature>(Long.class, Feature.class);
	
	public String toString()
	{
		return name;
	}
	
	public void setImageStandardResolutionFile(S3File imageStandardResolutionFile) {
		this.imageStandardResolutionFile = imageStandardResolutionFile;
		this.imageStandardResolutionURL = imageStandardResolutionFile.getUrlAsString();
	}
	
	public void setImageThumbnailFile(S3File imageThumbnailFile) {
		this.imageThumbnailFile = imageThumbnailFile;
		this.imageThumbnailURL = imageThumbnailFile.getUrlAsString();
	}
	
	// Retrieve the icon URL 
	public String getIconURL() {
		if(this.source_type == MyConstants.FeatureStrings.OVERLAY)
			return MyConstants.FEATURE_SERVER_NAME_PORT + "/assets/img/overlay.png";
		else if(this.source_type == MyConstants.FeatureStrings.MAPPED_INSTAGRAM)
			return MyConstants.FEATURE_SERVER_NAME_PORT + "/assets/img/mInsta.png";
		else if(this.source_type == MyConstants.FeatureStrings.INSTAGRAM)
			return MyConstants.FEATURE_SERVER_NAME_PORT + "/assets/img/mInsta.png";
		else return "";
	}
	
	// Retrieve the description URL
	public String getDescriptionURL() {
		return MyConstants.FEATURE_SERVER_NAME_PORT + "/content/" + this.id;
	}
	
	// Uses Pythagoras to calculate the distance apart in terms of coordinates 
	public double getDistance(Feature other) {
		final double dx = this.featureGeometry.coordinate_0-other.featureGeometry.coordinate_0; 
        final double dy = this.featureGeometry.coordinate_1-other.featureGeometry.coordinate_1;
        return Math.sqrt(dx*dx + dy*dy);
	}
	
	// Uses Haversine formula to calculate the distance apart in terms of coordinates 
	public double getHaversineDistance(Feature other)
	{
		return GeoCalculations.haversine(this.featureGeometry.coordinate_1, this.featureGeometry.coordinate_0,
										other.featureGeometry.coordinate_1, other.featureGeometry.coordinate_0);
	}

	// Currently using Haversine formula for comparison
	@Override
	public int compare(Feature arg0, Feature arg1) {
		final double distanceDelta = getHaversineDistance(arg0) - getHaversineDistance(arg1); 
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
		//another change
		String jsonString = "{";
		
		if(this.source_type == MyConstants.FeatureStrings.INSTAGRAM)
			jsonString+="\"id\" : \"" + this.alternate_id + "\",";
		else
			jsonString+="\"id\" : \"" + String.valueOf(this.id) + "\",";
		jsonString+="\"type\" : \"Feature\"," +
					"\"geometry\" : {";
		jsonString += 		"\"type\" : \"" + this.featureGeometry.type + "\"," +
							"\"coordinates\" : [" + String.valueOf(this.featureGeometry.coordinate_0) +
												"," + String.valueOf(this.featureGeometry.coordinate_1) + 
							"]}" +
					",\"properties\" : {" +
							"\"images\" : {" + 
									"\"thumbnail\" : \"" + this.imageThumbnailURL +
									"\",\"high_resolution\" : \"" + this.imageStandardResolutionURL +
									"\",\"standard_resolution\" : \"" + this.imageStandardResolutionURL +
							"\"}"; 
		jsonString += 		",\"created_time\" : " + String.valueOf(this.created_time.getTime()/1000);
		jsonString += 		",\"source_type\" : \"" + this.source_type +
							"\",\"icon_url\" : \"" + this.getIconURL() +
							"\",\"desc_url\" : \"" + this.getDescriptionURL() +
							"\",\"description\" : \"" + this.description +
							"\",\"name\" : \"" + "(name stub)\"";    // Is this supplied when a feature is created?
//		jsonString += 					"\",\"seesion_id\" : \"" + String.valueOf(this.featureSession.id);   // This should be removed and session sub key referred to instead. Deliberate spelling error to match!
if(this.featureSession != null)
		jsonString += 					"\",\"session\" : " + this.featureSession.toJson();
if(this.featureUser != null)
		jsonString += 					",\"user\" : " + this.featureUser.toJson();
		jsonString += 					",\"tags\" : " + tagJson +
					"}" +
			"}";

		return jsonString;
	}
}
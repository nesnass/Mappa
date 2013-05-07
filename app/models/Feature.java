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

import flexjson.JSONSerializer;
import helpers.GeoCalculations;
import helpers.MyConstants;
import models.geometry.Geometry;
import models.geometry.Point;
import parsers.TwitterParser;
import play.data.validation.*;
import play.db.ebean.Model;

/**
 * @author Richard Nesnass - AUS
 */

@Entity
@Table(name="s_feature")
public class Feature extends Model implements Comparator<Feature>
{
	private static final long serialVersionUID = 6285870362122377542L;
	
	@Id
	@GeneratedValue
	public long id;

	@ManyToOne()
	public MUser featureUser;
	
	@ManyToOne()
	public MUser featureMapper;

	@ManyToOne()
	public Session featureSession;
	
	@ManyToMany(cascade=CascadeType.ALL)
	public Set<Tag> featureTags = new HashSet<Tag>();
	
	@Embedded()
	public Point geometry;
	
	@OneToOne(cascade=CascadeType.ALL)
	public S3File imageStandardResolutionFile;

	@OneToOne(cascade=CascadeType.ALL)
	public S3File imageThumbnailFile;

	@Embedded()
	public Properties properties;

	@Embeddable
	public class Properties {

/****************************************************
 * 		These variables should be mapped to the ones above for JSON printing ^
 */
		@Transient
		public MUser user = featureUser;

		@Transient
		public MUser mapper = featureMapper;

		@Transient
		public Session session = featureSession;

		@Transient
		public Set<Tag> tags = featureTags;

// ***************************************************
		
		@Constraints.MaxLength(255)
		public String imageStandardResolutionURL = "";
		
		@Constraints.MaxLength(255)
		public String imageThumbnailURL = "";

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
	}

	public Feature() {
		properties = this.new Properties();
		properties.created_time = new Date();
	}
	
	// Construct a new feature given Geometry
	public Feature(Point point) {
		this();
		geometry = point;
	}
	
	// Construct a new feature given a JSON node
	public Feature(JsonNode featureNode) {
		this();
		assignProperties(featureNode);
	}
	
	// Setup by JsonNode object
	public void assignProperties(JsonNode featureNode)
	{
		// Set relations
		if(geometry == null)
		{
			//this.featureGeometry = new Geometry(featureNode.get("geometry"));
			
			geometry = new Point(featureNode.get("geometry"));
		}
		else
			geometry.assignProperties(featureNode.get("geometry"));
		featureSession = Session.find.byId(featureNode.get("properties").path("session_id").asLong());

		// *************  Session should always be supplied in the JSON. This case should be removed when sessions are enabled
		if(featureSession == null) {
			Session newSession = new Session();

			newSession.facebook_group_id = 0;
			newSession.stitle = "Test Session Title";
			newSession.sdescription = "Test session Description";
			newSession.save();

			featureSession = newSession;
		}

		// Set regular parameters
		properties.type = featureNode.get("type").asText();
		properties.description = featureNode.get("properties").get("description").asText();

		String source = featureNode.get("properties").get("source_type").asText();
		if (source.equalsIgnoreCase(MyConstants.FeatureStrings.OVERLAY.toString()))
			properties.source_type = MyConstants.FeatureStrings.OVERLAY;
		else if (source.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString()))
			properties.source_type = MyConstants.FeatureStrings.MAPPED_INSTAGRAM;
		//this.name = featureNode.get("properties").path("name").getTextValue();

		Set<String> foundTags = new HashSet<String>();
		// Set source dependent parameters
		switch(properties.source_type)
		{
		case OVERLAY :
			foundTags = TwitterParser.searchHashTags(properties.description);
			break;

		case INSTAGRAM:
			break;

		case MAPPED_INSTAGRAM:
			// 'name' not included in regular 'Overlay' feature??  '.path' call is used to return a 'missing node' instead of null if node not found
			properties.mapper_description = featureNode.get("properties").path("mapper_description").getTextValue();
			properties.icon_url = MyConstants.FEATURE_SERVER_NAME_PORT + "/assets/img/mInsta.png";
			foundTags = TwitterParser.searchHashTags(properties.mapper_description);

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
	public void assignProperties(MediaFeedData jInstagramMedia)
	{
		// Set regular parameters
		if(geometry == null)
			geometry = new Point(jInstagramMedia.getLocation());
		else
			geometry.assignProperties(jInstagramMedia.getLocation());
		properties.type = "INSTAGRAM";
		properties.description = jInstagramMedia.getCaption().getText();
		properties.source_type = MyConstants.FeatureStrings.INSTAGRAM;
		properties.imageThumbnailURL = jInstagramMedia.getImages().getThumbnail().getImageUrl();
		properties.imageStandardResolutionURL = jInstagramMedia.getImages().getStandardResolution().getImageUrl();
		SimpleDateFormat sdf = new SimpleDateFormat();
		try {
			properties.created_time.setTime(sdf.parse(jInstagramMedia.getCreatedTime()).getTime());
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

		featureTags.add(newTag);
		newTag.tagFeatures.add(this);
		//newTag.save();
	}
	
	public void deleteImages()
	{
		imageStandardResolutionFile.delete();
		imageThumbnailFile.delete();
		properties.imageStandardResolutionURL = "";
		properties.imageThumbnailURL = "";
		imageStandardResolutionFile.delete();
		imageThumbnailFile.delete();
	}
	
	public static Model.Finder<Long, Feature> find = new Model.Finder<Long, Feature>(Long.class, Feature.class);
	
	public String toString()
	{
		return properties.name;
	}
	
	// Retrieve the icon URL 
	public String getIconURL() {
		if(properties.source_type == MyConstants.FeatureStrings.OVERLAY)
			return MyConstants.FEATURE_SERVER_NAME_PORT + "/assets/img/overlay.png";
		else if(properties.source_type == MyConstants.FeatureStrings.MAPPED_INSTAGRAM)
			return MyConstants.FEATURE_SERVER_NAME_PORT + "/assets/img/mInsta.png";
		else if(properties.source_type == MyConstants.FeatureStrings.INSTAGRAM)
			return MyConstants.FEATURE_SERVER_NAME_PORT + "/assets/img/mInsta.png";
		else return "";
	}
	
	// Retrieve the description URL
	public String getDescriptionURL() {
		return MyConstants.FEATURE_SERVER_NAME_PORT + "/content/" + this.id;
	}
	
	// Uses Pythagoras to calculate the distance apart in terms of coordinates 
	public double getDistance(Feature other) {
		final double dx = geometry.getLng()-other.geometry.getLng(); 
        final double dy = geometry.getLat()-other.geometry.getLat();
        return Math.sqrt(dx*dx + dy*dy);
	}
	
	// Uses Haversine formula to calculate the distance apart in terms of coordinates 
	public double getHaversineDistance(Feature other)
	{
		return GeoCalculations.haversine(geometry.getLat(), geometry.getLng(),
										other.geometry.getLat(), other.geometry.getLng());
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
		JSONSerializer serializer = new JSONSerializer();
        return serializer.exclude("*.class").include("coordinates").exclude("lat").exclude("lng").exclude("imageStandardResolutionFile").exclude("imageThumbnailFile").serialize( this );
        
        
/*		// Get the listing of tags for this feature
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
					"\"type\" : \"Feature\"," +
					"\"geometry\" : {";
		jsonString += 		"\"type\" : \"" + this.featureGeometry.gtype + "\"," +
							"\"coordinates\" : [" + String.valueOf(this.featureGeometry.lng) +
												"," + String.valueOf(this.featureGeometry.lat) + 
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
							"\",\"name\" : \"" + "(name stub)";    // Is this supplied when a feature is created?
	//	jsonString += 					"\",\"seesion_id\" : \"" + String.valueOf(this.featureSession.id);   // This should be removed and session sub key referred to instead. Deliberate spelling error to match!
		jsonString += 					"\",\"session\" : " + this.featureSession.toJson();
		jsonString += 					",\"user\" : " + this.featureUser.toJson();
		jsonString += 					",\"tags\" : " + tagJson +
					"}" +
			"}";

		return jsonString;
		*/
	}
}
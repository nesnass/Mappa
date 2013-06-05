package models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.persistence.*;

import org.codehaus.jackson.JsonNode;
import org.jinstagram.entity.users.feed.MediaFeedData;

import flexjson.JSON;
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
	private long _id;
	
	private String origin_id;
	
	@Constraints.MaxLength(30)
	public String type = "Feature";

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

	@Embedded()
	private Images images;
	
	public String accessfeatureUser() {
		return featureUser.full_name;
	}
	
	@Embeddable
	public class Images 
	{
		@Constraints.MaxLength(255)
		public String standard_resolution = "";
		
		@Constraints.MaxLength(255)
		public String thumbnail = "";
	}
	
	@Embeddable
	public class Properties
	{	
		
/****************************************************
 * 		These non-persisted variables are mapped to the ones above for JSON printing ^
 */
		@Transient
		private Feature myParent;
		
		@Transient
		private String origin_id;
		
		@Transient
		private Images images;
		public Images getImages() { return images; }
		public void setImages(Images images) { this.images = images; }

		@Transient
		private MUser user;
		public MUser getUser() { return user; }
		public void setUser(MUser u) { user = u; }
		
		@Transient
		private MUser mapper;	
		public MUser getMapper() { return mapper; }
		public void setMapper(MUser m) { mapper = m; }
		
		@Transient
		private Session session;	
		public Session getSession() { return session; }
		public void setSession(Session s) { session = s; }
		
		@Transient
		private Set<Tag> taglist;
		
		@JSON(include=true)
		public String[] getTags() {
			String[] stringTags = new String[taglist.size()];
			Iterator<Tag> it = taglist.iterator();
			int i = 0;
			while(it.hasNext()) {
				stringTags[i] = it.next().getTag();
				i++;
			}
			return stringTags;
		}
		public void setTags(Set<Tag> t) { taglist = t; }

// ***************************************************
		
		@Temporal(TemporalType.TIMESTAMP)
		private Date created_time;
		
		// Convert Date to Unix timestamp in seconds
		public long getCreated_time() {
			if(this.source_type == MyConstants.FeatureStrings.INSTAGRAM.toString())
				return created_time.getTime();
			else
				return created_time.getTime()/1000;
		}

		@Constraints.MaxLength(255)
		public String descr_url = "";
		
		// Retrieve the icon URL 
		public String getIcon_url() {
			if(source_type.equals(MyConstants.FeatureStrings.MAPPA.toString()))
				return MyConstants.NEW_FEATURE_SERVER_NAME_PORT + "/resources/images/mappa.png";
			else if(source_type.equals( MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString()))
				return MyConstants.NEW_FEATURE_SERVER_NAME_PORT + "/resources/images/mapped_instagram.png";
			else if(source_type.equals( MyConstants.FeatureStrings.INSTAGRAM.toString()))
				return MyConstants.NEW_FEATURE_SERVER_NAME_PORT + "/resources/images/instagram.png";
			else return "";
		}
		
		// Retrieve the description URL
		public String getDescription_url() {
			return MyConstants.NEW_FEATURE_SERVER_NAME_PORT + "/content/" + origin_id;
		}
		
		public String getName() {
			return description.length() < 22 ? description : description.substring(0, 22);
		}
		
		@Constraints.MaxLength(255)
		public String description = "";
		
//		@Constraints.MaxLength(255)
		public String mapper_description = "";

		@Constraints.MaxLength(255)
		public String icon_url = "";

		@Constraints.MaxLength(30)
		public String source_type = MyConstants.FeatureStrings.MAPPA.toString();
	}

	public Feature() {
		images = this.new Images();
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
	
	@JSON(include=true)
	public String getId() {
		if(this.properties.source_type.equals(MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString()) || this.properties.source_type.equals(MyConstants.FeatureStrings.MAPPA.toString())) {
			return String.valueOf(this._id);
		}
		else
			return this.origin_id;
	}

	@JSON(include=false)
	public String getOrigin_id() {
		return origin_id;
	}
	
	public void setOrigin_id() {
		this.origin_id = String.valueOf(_id);
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
		featureSession = Session.find.where().eq("facebook_group_id", featureNode.get("properties").path("sessions").path(0).path("id").asText()).findUnique();

// *************  Session should always be supplied in the JSON. This case should be removed when sessions are enabled
		if(featureSession == null) {
			Session newSession = new Session();

			newSession.setFacebook_group_id( featureNode.get("properties").path("sessions").path(0).path("id").asText() );
			newSession.setTitle( featureNode.get("properties").path("sessions").path(0).path("name").asText() );
		//	newSession.setDescription( featureNode.get("properties").path("session").path(0).path("description").asText() );
		//	newSession.setPrivacy(featureNode.get("properties").path("session").path(0).path("description").asText() );
			newSession.save();

			featureSession = newSession;
		}

		// Set regular parameters
		properties.description = featureNode.get("properties").get("description").asText();

		String source = featureNode.get("properties").get("source_type").asText();
		if (source.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPA.toString()))
			properties.source_type = MyConstants.FeatureStrings.MAPPA.toString();
		else if (source.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString()))
			properties.source_type = MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString();
		//this.name = featureNode.get("properties").path("name").getTextValue();

		Set<String> foundTags = new HashSet<String>();
		// Set source dependent parameters
		if(properties.source_type.toString().equals(MyConstants.FeatureStrings.MAPPA.toString()))
		{
			foundTags = TwitterParser.searchHashTags(properties.description);
		}
		else if(properties.source_type.toString().equals(MyConstants.FeatureStrings.INSTAGRAM.toString()))
		{
			;
		}
		else if(properties.source_type.toString().equals(MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString()))
		{
			images.standard_resolution = featureNode.get("properties").get("images").path("standard_resolution").asText();
			images.thumbnail = featureNode.get("properties").get("images").path("thumbnail").getTextValue();
			// 'name' not included in regular 'Overlay' feature??  '.path' call is used to return a 'missing node' instead of null if node not found
			properties.mapper_description = featureNode.get("properties").path("mapper_description").getTextValue();
			properties.icon_url = MyConstants.NEW_FEATURE_SERVER_NAME_PORT + "/resources/images/mapped_instagram.png";
			foundTags = TwitterParser.searchHashTags(properties.mapper_description);

			// ******** Image URLs should be added here. Are they included in the MAPPED_INSTAGRAM JSON request?
		}
		
		// Set the Tag references, if any tags exist
		// Look through the description for tags
		Iterator<JsonNode> tagsIteratorFromNode = featureNode.get("properties").path("tags").iterator();
		while(tagsIteratorFromNode.hasNext())
		{
			foundTags.add(tagsIteratorFromNode.next().getTextValue());
		}
		
		// Remove existing tags for this feature
		removeTags();
		
		// Add unique / new and non-existing tags to the database
		if(foundTags != null) {
			Iterator<String> tagsIteratorAllTags = foundTags.iterator();
			while(tagsIteratorAllTags.hasNext())
			{
				addTag(tagsIteratorAllTags.next());
			}
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
		if(jInstagramMedia.getCaption() != null)
			properties.description = jInstagramMedia.getCaption().getText();
		properties.descr_url = MyConstants.NEW_FEATURE_SERVER_NAME_PORT + "/instagram/" + jInstagramMedia.getId();
		properties.source_type = MyConstants.FeatureStrings.INSTAGRAM.toString();
		images.thumbnail = jInstagramMedia.getImages().getThumbnail().getImageUrl();
		images.standard_resolution = jInstagramMedia.getImages().getStandardResolution().getImageUrl();
		long tt = Long.parseLong(jInstagramMedia.getCreatedTime());
		properties.created_time.setTime(tt);
		this.origin_id = jInstagramMedia.getId();

		// Set the Tag references, if any tags exist
		Iterator<String> tagsIterator = jInstagramMedia.getTags().iterator();
		while(tagsIterator.hasNext())
		{
			addTag(tagsIterator.next());
		}
	}
	
	public Images retrieveImages() {
		return images;
	}

	public void removeTags()
	{
		// Find tags with references to this feature and others, remove
		Iterator<Tag> it = featureTags.iterator();
		while(it.hasNext())
		{
			it.next().tagFeatures.remove(this);
			it.remove();
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
		properties.images.standard_resolution = "";
		properties.images.thumbnail = "";
		imageStandardResolutionFile.delete();
		imageThumbnailFile.delete();
	}
	
	public static Model.Finder<Long, Feature> find = new Model.Finder<Long, Feature>(Long.class, Feature.class);
	
	public String toString()
	{
		return properties.description;
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
		// Temporary variables inside 'properties' need to be initialised before serialisation
		properties.setUser(this.featureUser);
		properties.setMapper(this.featureMapper);
		properties.setSession(this.featureSession);
		properties.setTags(this.featureTags);
		properties.setImages(this.images);
		if(this.properties.source_type.equals(MyConstants.FeatureStrings.MAPPA.toString()) || this.properties.source_type.equals(MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString())) {
			properties.origin_id = String.valueOf(this._id);
		}
		else
			properties.origin_id = this.origin_id;
		
		JSONSerializer serializer = new JSONSerializer();
        return serializer
        		.exclude("*.class")
        		.include("geometry.coordinates")
        		.exclude("geometry.lat")
        		.exclude("geometry.lng")
        		.exclude("featureUser")
        		.exclude("featureMapper")
        		.exclude("featureSession")
        		.exclude("featureTags")
        		.exclude("imageStandardResolutionFile")
        		.exclude("imageThumbnailFile")
        		.serialize( this );

	}
}
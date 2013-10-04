package models;


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
import models.geometry.Point;
import parsers.TwitterParser;
import play.data.validation.*;

import play.db.ebean.Model;

/**
 * @author Richard Nesnass - AUS
 */

@Entity
@Table(name="s_feature")
public class Feature extends Model // implements Comparator<Feature>
{
	private static final long serialVersionUID = 6285870362122377542L;
	
	@Id
	@GeneratedValue
	private long _id;

	@Constraints.MaxLength(30)
	public String type = "Feature";

	@ManyToOne
	public MUser featureUser;
	
	// This needs to be removed - mock mapper is don in Properties class. But we need to persist an "origin" section instead for mapped items . See email 11th June 2013.
//	@ManyToOne
//	public MUser featureMapper;

	@ManyToOne
	public Session featureSession;
	
	@ManyToMany
	public Set<Tag> featureTags = new HashSet<Tag>();
	
	@Transient
	private Set<Tag> instaTags = new HashSet<Tag>();
	
	@Embedded
	public Point geometry;
	
	@OneToOne(cascade=CascadeType.ALL)
	public S3File imageStandardResolutionFile;

	@OneToOne(cascade=CascadeType.ALL)
	public S3File imageThumbnailFile;

	@Embedded
	public Properties properties;

	@Embedded
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
	
	@Embedded
	private Origin origin;
	
	@Embeddable
	public class Origin {
		
		public String id;
		public String full_name;
		public String username;
		private double lngOrigin;
		private double latOrigin;
		
		@JSON(include=false)
		public double getLng() {
			return lngOrigin;
		}
		@JSON(include=false)
		public void setLng(double lng) {
			this.lngOrigin = lng;
		}
		@JSON(include=false)
		public double getLat() {
			return latOrigin;
		}
		@JSON(include=false)
		public void setLat(double lat) {
			this.latOrigin = lat;
		}

		
		
		@JSON(include=true)
		public double[] getLocation() {
			double[] location = new double[2];
			if(latOrigin == 0 && lngOrigin == 0)
				return null;
			location[0] = lngOrigin;
			location[1] = latOrigin;
			return location;
		}
		
		public void setLocation(double ln, double la) {
			this.lngOrigin = ln;
			this.latOrigin = la;
		}
	}
	
	@Embeddable
	public class Properties
	{	
		
/****************************************************
 * 		These non-persisted variables are mapped to the ones above for JSON printing ^
 */
		@Transient
		private String idProperties;
		
		@JSON(include=true)
		public String getId() {
			if(source_type.equals(MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString()) || source_type.equals(MyConstants.FeatureStrings.MAPPA.toString())) {
				return String.valueOf(this.idProperties);
			}
			else
				return origin.id;
		}
		public void setId(String id) {
			idProperties = id;
		}
		
		@Transient
		private Images images;
		public Images getImages() { return images; }
		public void setImages(Images images) { this.images = images; }

		@Transient
		private Origin user;
		public Origin getUser() { return user; }
		public void setUser(Origin u) { user = u; }
		
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
			return MyConstants.NEW_FEATURE_SERVER_NAME_PORT + "/content/" + user.id;
		}
		
		public String getName() {
			return description.length() < 22 ? description : description.substring(0, 22);
		}
		
//		@Constraints.MaxLength(255)
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
		origin = this.new Origin();
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
			return this.origin.id;
	}
	
	public Date getDate() {
		return properties.created_time;
	}

/*	@JSON(include=false)
	public String getOrigin_id() {
		return origin.id;
	}
*/
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

// *************  Session should always be supplied in the JSON
		if(featureSession == null) {
			Session newSession = new Session();

			newSession.setFacebook_group_id( featureNode.get("properties").path("sessions").path(0).path("id").asText() );
			newSession.setTitle( featureNode.get("properties").path("sessions").path(0).path("name").asText() );
		//	newSession.setDescription( featureNode.get("properties").path("session").path(0).path("description").asText() );
		//	newSession.setPrivacy(featureNode.get("properties").path("session").path(0).path("description").asText() );
			newSession.save();

			featureSession = newSession;
		}

		
		String source = featureNode.get("properties").get("source_type").asText();
		if (source.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPA.toString()))
			properties.source_type = MyConstants.FeatureStrings.MAPPA.toString();
		else if (source.equalsIgnoreCase(MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString()))
			properties.source_type = MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString();
		//this.name = featureNode.get("properties").path("name").getTextValue();

		Set<String> foundTags = new HashSet<String>();
		foundTags = TwitterParser.searchHashTags(properties.description);
		// Set source dependent parameters
		if(properties.source_type.equals(MyConstants.FeatureStrings.MAPPA.toString()))
		{
			;
		}
		else if(properties.source_type.equals(MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString()))
		{
			foundTags.addAll(TwitterParser.searchHashTags(properties.mapper_description));
		}
		else if(properties.source_type.equals(MyConstants.FeatureStrings.INSTAGRAM.toString()))
		{
			;
		}
		origin.full_name = featureNode.get("properties").get("user").get("full_name").asText();
		origin.setLocation(featureNode.get("properties").get("user").get("location").get(0).asDouble(), featureNode.get("properties").get("user").get("location").get(1).asDouble());
		origin.id = featureNode.get("properties").get("user").get("id").asText();
		origin.username = featureNode.get("properties").get("user").get("username").asText();
		
		// Set the Tag references, if any tags exist
		// Look through the description for tags
		Iterator<JsonNode> tagsIteratorFromNode = featureNode.get("properties").path("tags").iterator();
		while(tagsIteratorFromNode.hasNext())
		{
			foundTags.add(tagsIteratorFromNode.next().getTextValue());
		}
		
		updateTags(foundTags);
		
		/*
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
		*/
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
		
		// Instagram created time
		// long tt = Long.parseLong(jInstagramMedia.getCreatedTime());
		properties.created_time = new Date();
		this.origin.id = jInstagramMedia.getId();
		
		this.origin.full_name = jInstagramMedia.getUser().getFullName();
		this.origin.setLocation(jInstagramMedia.getLocation().getLongitude(), jInstagramMedia.getLocation().getLatitude());
		this.origin.username = jInstagramMedia.getUser().getUserName();

		// New set to contain tags
		Set<String> foundTags = new HashSet<String>();
		// Set the Tag references, if any tags exist
		Iterator<String> tagsIterator = jInstagramMedia.getTags().iterator();
		while(tagsIterator.hasNext())
		{
			foundTags.add(tagsIterator.next());
		}
		updateTags(foundTags);
	}
	
	public Images retrieveImages() {
		return images;
	}
	
	/*  This method avoids PSQL complaints from eBean, but creates a lot of DB accesses! */
	/*  Reconsider the need for many-many tag relationship.. */
	
	// this.properties.source_type.equals(MyConstants.FeatureStrings.MAPPA.toString())
	
	
	public void updateTags(Set<String> tags) {
		
		// In the case of a pure Instagram POI, which is not persisted, we cannot persist their tags either.
		if(this.properties.source_type.equals(MyConstants.FeatureStrings.INSTAGRAM.toString())) {
			for (String tag : tags) {
					Tag newTag = new Tag(tag);
					instaTags.add(newTag);
		    }
		}
		else {
			for(Tag tag : featureTags) {
				tag.tagFeatures.remove(this);
				tag.save();
			}
			featureTags.clear();
			
			if(tags == null) {
				this.save();
				return;
			}
			
		  /*  for(Tag tag : featureTags) {
		        removeTag(tag.retrieveId());
		    }
		  */
		    for (String tag : tags) {
		    	Tag newTag = Tag.find.where().eq("tag", tag).findUnique();
				if(newTag == null) {
					newTag = new Tag(tag);
				}
				newTag.tagFeatures.add(this);
				newTag.save();
				featureTags.add(newTag);
		    }
		    this.save();
		   // this.saveManyToManyAssociations("featureTags");
		}
	}
	
	public void removeTag(Long tagId) {
	    featureTags.remove(Tag.find.ref(tagId));
	    this.saveManyToManyAssociations("featureTags");
	}
	
	/*
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
	*/
	/*
	public void addTag(String theTag)
	{
		Tag newTag = Tag.find.where().eq("tag", theTag).findUnique();
		if(newTag == null)
			newTag = new Tag(theTag);

		featureTags.add(newTag);
		newTag.tagFeatures.add(this);
		//newTag.save();
	}
	*/
	public void deleteImages()
	{
		imageStandardResolutionFile.delete();
		imageThumbnailFile.delete();
		images.standard_resolution = "";
		images.thumbnail = "";
		imageStandardResolutionFile.delete();
		imageThumbnailFile.delete();
		properties.setImages(images);
	}
	
	
	// Comparing Features using distance
	public class DistanceComparator implements Comparator<Feature> {
		public int compare(Feature arg0, Feature arg1){
			final double distanceDelta = getHaversineDistance(arg0) - getHaversineDistance(arg1); 
	        return distanceDelta < 0 ? -1 : 1; 
		}
	}
	
	// Comparing Features using timestamp - reverse order - newest to oldest
	public class TimeStampComparator implements Comparator<Feature> {
		public int compare(Feature arg0, Feature arg1){
			long t1 = arg0.getDate().getTime();
		    long t2 = arg1.getDate().getTime();
		    return t1 == t2 ? 0 : t1 < t2 ? 1 : -1;
			//return arg0.getDate().compareTo(arg1.getDate());
		}
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
		return GeoCalculations.haversine(geometry.getLat(), geometry.getLng(), other.geometry.getLat(), other.geometry.getLng());
	}
	
	public static Model.Finder<Long, Feature> find = new Model.Finder<Long, Feature>(Long.class, Feature.class);
	
	public String toString()
	{
		return properties.description;
	}
	
	// Created to map the json output matching the implementation currently running on client (client cannot be changed at this time)
	public String toJson()
	{
		// Temporary variables inside 'properties' need to be initialised before serialisation
		if(this.properties.source_type.equals(MyConstants.FeatureStrings.MAPPA.toString()) || this.properties.source_type.equals(MyConstants.FeatureStrings.MAPPED_INSTAGRAM.toString())) {
			properties.setId(String.valueOf(this._id));
			properties.setTags(this.featureTags);
		}
		else {
			properties.setId(this.origin.id);
			properties.setTags(this.instaTags);
		}
		properties.setUser(this.origin);
		properties.setMapper(this.featureUser);
		properties.setSession(this.featureSession);
		properties.setImages(this.images);

		
		JSONSerializer serializer = new JSONSerializer();
        return serializer
        		.exclude("*.class")
        		.include("geometry.coordinates")
        		.exclude("geometry.lat")
        		.exclude("geometry.lng")
        		.exclude("featureUser")
        		.exclude("featureSession")
        		.exclude("featureTags")
        		.exclude("imageStandardResolutionFile")
        		.exclude("imageThumbnailFile")
        		.serialize( this );

	}
}
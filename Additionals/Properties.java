package models;

import helpers.MyConstants;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import models.geometry.Point;
import play.data.validation.Constraints;
import play.db.ebean.Model;

@Embeddable
public class Properties extends Model {

	private static final long serialVersionUID = 2697740214710134721L;

	@Id
	@GeneratedValue
	private long id;
	
	@Transient
	protected Feature feature;

	@Transient
	private MUser user;

	@Transient
	private MUser mapper;

	@Transient
	private Session session;

	@Transient
	private Set<Tag> tags = new HashSet<Tag>();
	
	@Embedded()
	public Point geometry;
	
	@OneToOne(cascade=CascadeType.ALL)
	public S3File imageStandardResolutionFile;

	@OneToOne(cascade=CascadeType.ALL)
	public S3File imageThumbnailFile;

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
	
	public Properties(Feature f)
	{
		this.feature = f;
	}
}

package models;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

// import play.Logger;
import play.db.ebean.Model;
import plugins.S3Plugin;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

import external.MyConstants;


@Entity
@Table(name="s_s3file")
public class S3File extends Model {

	private static final long serialVersionUID = 1L;

	@Id
    public UUID id;

    private String bucket;

    public String type;

    @Transient
    public File file;

    public URL getUrl() throws MalformedURLException {
        return new URL(MyConstants.AMAZON_SERVER_NAME_PORT + bucket + "/" + getActualFileName());
    }
    
    public String getUrlAsString() {
    	return MyConstants.AMAZON_SERVER_NAME_PORT + bucket + "/" + getActualFileName();
    }

    private String getActualFileName() {
        return id + "/" + type;
    }

    @Override
    public void save() {
        if (S3Plugin.amazonS3 == null) {
            throw new RuntimeException("Could not save");
        }
        else {
            this.bucket = S3Plugin.s3Bucket;
            
            super.save(); // assigns an id

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, getActualFileName(), file);
            putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead); // public for all
            S3Plugin.amazonS3.putObject(putObjectRequest); // upload file
        }
    }

    @Override
    public void delete() {
        if (S3Plugin.amazonS3 == null) {
            throw new RuntimeException("Could not delete");
        }
        else {
            S3Plugin.amazonS3.deleteObject(bucket, getActualFileName());
            super.delete();
        }
    }

}
package helpers;

import org.jinstagram.auth.model.Token;

public class MyConstants
{
	public enum FeatureStrings
	{
	    MAPPA("mappa"),
	    MAPPED_INSTAGRAM("mapped_instagram"),
	    INSTAGRAM("instagram");
	    private final String text;

	    private FeatureStrings(final String text)
	    {
	        this.text = text;
	    }
	    @Override
	    public String toString()
	    {
	        return text;
	    }
	}
	
	public enum S3Strings
	{
	    SIZE_ORIGINAL("size_original"),
	    SIZE_THUMBNAIL("size_thumbnail");
	    private final String text;

	    private S3Strings(final String text)
	    {
	        this.text = text;
	    }
	    @Override
	    public String toString()
	    {
	        return text;
	    }
	}
	
	public enum QueryStrings
	{
	    RADIUS("radius"),
	    BOUNDING_BOX("bounding_box"),
	    RECENT("recent");
	    private final String text;

	    private QueryStrings(final String text)
	    {
	        this.text = text;
	    }
	    @Override
	    public String toString()
	    {
	        return text;
	    }
	}
	
	public final static String OLD_FEATURE_SERVER_NAME_PORT = "http://intermedia-prod03.uio.no:9010";
	public final static String NEW_FEATURE_SERVER_NAME_PORT = "http://mappa.uio.im";
	public final static String AMAZON_SERVER_NAME_PORT = "https://s3.amazonaws.com/";
	
	public final static String INSTAGRAM_CLIENT_ID = "452b5fe7bad74ccdb9f0aa7860939aaf";
	public final static String INSTAGRAM_SECRET_ID = "529ba305dafb480aa6c83bea34b12bae";
	public final static String INSTAGRAM_CALLBACK = "http://mappa.uio.im/instagramAuthorisation/";
	public final static String INSTAGRAM_ACCESS_TOKEN = "391112889.452b5fe.e4348987b6454d57a6dfe41c278c2285";
	
	public final static String KML_MAPPA_ICON = "http://maps.gstatic.com/mapfiles/ridefinder-images/mm_20_orange.png";
	
	
	public final static int DEFAULT_INSTAGRAM_DISTANCE = 5000;
	public final static int MAX_FEATURES_TO_GET = 18;
	public final static int RADIUS_MULTIPLIER = 5;  // Used to obtain more or less results, for testing purposes
}

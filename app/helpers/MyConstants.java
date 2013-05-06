package helpers;

public class MyConstants
{
	public enum FeatureStrings
	{
	    OVERLAY("overlay"),
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
	
	public final static String FEATURE_SERVER_NAME_PORT = "http://intermedia-prod03.uio.no:9010";
	public final static String AMAZON_SERVER_NAME_PORT = "https://s3.amazonaws.com/";
	public final static String INSTAGRAM_CLIENT_ID = "a80dd450be84452a91527609a4eae97b";   //sample image: 447745155284458290_181517165
	
	public final static int DEFAULT_INSTAGRAM_DISTANCE = 1000;
	public final static int MAX_FEATURES_TO_GET_IN_BOUNDING_BOX = 18;
	public final static int MOST_RECENT_FEATURES_TO_GET = 3;
	public final static int RADIUS_MULTIPLIER = 1;  // Used to obtain more or less results, for testing purposes
}

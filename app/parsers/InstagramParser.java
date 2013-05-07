package parsers;

import helpers.MyConstants;
import models.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.jinstagram.Instagram;
import org.jinstagram.entity.media.MediaInfoFeed;
import org.jinstagram.entity.users.feed.MediaFeed;
import org.jinstagram.entity.users.feed.MediaFeedData;
import org.jinstagram.exceptions.InstagramException;

import play.Logger;

public class InstagramParser
{
	/*	InstagramService service =  new InstagramAuthService()
    	.apiKey("your_client_id")
    	.apiSecret("your_client_secret")
    	.callback("your_callback_url")     
    	.build();
	 */	
	
	
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
	
	// ********************************************************************************
	//  *******    New methods using jInstagram library by Sachin Handiekar     *******
	//  *******        https://github.com/sachin-handiekar/jInstagram           *******
	//  *******               Calls to Instagram are synchronous                *******
	
	// Set up the query
	public static List<Feature> getQuery(QueryStrings queryType, double latitude, double longitude, int radius)
	{
		Instagram instagram = new Instagram(MyConstants.INSTAGRAM_CLIENT_ID);
		MediaFeed feed = null;
/*		try {
			switch (queryType)
			{
			case RADIUS:
				feed = instagram.searchMedia(latitude, longitude, null, null, radius);
				break;
			case BOUNDING_BOX:
				feed = instagram.searchMedia(latitude, longitude, null, null, radius);
				break;
			case RECENT:
				// 'recent' search is not likely to return different results from radius unless we adjust distance also?
				feed = instagram.searchMedia(latitude, longitude, new Date(), null, radius);
				break;
			}
		
		} catch (InstagramException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Logger.info("InstagramException: " + e.getMessage() + " " + e.getCause());
		}
*/
			return getFeaturesFromFeed(feed);
	}
	
	// Process the feed retrieved
	public static List<Feature> getFeaturesFromFeed(MediaFeed feed)
	{
		List<Feature> featureList = new ArrayList<Feature>();
		if(feed != null)
		{
			List<MediaFeedData> feeds = feed.getData();
			Iterator<MediaFeedData> feedIterator = feeds.iterator();
			while(feedIterator.hasNext())
			{
				Feature f = new Feature();
				f.assignProperties(feedIterator.next());
				featureList.add(f);
			}
		}
		return featureList;
	}

	public static Feature getInstaByMediaId(String id) 
	{
		Instagram instagram = new Instagram(MyConstants.INSTAGRAM_CLIENT_ID);
		MediaInfoFeed feed = null;
		try {
			feed = instagram.getMediaInfo(id);
		}
		catch (InstagramException e)
		{
		// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(feed != null)
		{
			Feature f = new Feature();
			MediaFeedData feeddata = feed.getData();
			f.assignProperties(feeddata);
			return f;
		}
		return null;
	}
	// ********************************************************************************

}

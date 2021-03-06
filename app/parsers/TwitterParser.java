package parsers;

import helpers.MyConstants;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Muhammad Fahied
 */

public class TwitterParser
{
	public static String parse(String tweetText, String source)
	{

		// Search for URLs
		//	     if (tweetText && tweetText.contains('http:')) 
		//	     {
		//	         int indexOfHttp = tweetText.indexOf('http:');
		//	         int endPoint = (tweetText.indexOf(' ', indexOfHttp) != -1) ? 
		//	        		 tweetText.indexOf(' ', indexOfHttp) : tweetText.length();
		//	         String url = tweetText.substring(indexOfHttp, endPoint);
		//	         String targetUrlHtml=  "<a href='${url}' target='_blank'>${url}</a>";
		//	         tweetText = tweetText.replace(url,targetUrlHtml );
		//	     }

		// TODO: implement instagram hooks for both users and hashtag
		// TODO: implement facebook hook for the current logged in user

	//	String patternStr = "(?:\\s|\\A)[##]+([A-Za-z0-9-_]+)";
		String patternStr = "(?:\\s|\\A)[##]+([\\p{L}0-9-_]+)";
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(tweetText);
		String result = "";

		// Search for Hashtags
		while (matcher.find()) {
			result = matcher.group();
			result = result.replace(" ", "");
			String search = result.replace("#", "");

			String searchHTML;
			if (source.equals("Instagram")) {
				searchHTML = "<a href='instagram://tag?name=" + search + "'>" + result + "</a>";
			}
			else
			{
				searchHTML = "<a href=" +  MyConstants.NEW_FEATURE_SERVER_NAME_PORT + "/search/" + search + " >" + result + "</a>";
			}
			tweetText = tweetText.replace(result,searchHTML);
		}

		// Search for Users "\\p{L}+"
//		patternStr = "(?:\\s|\\A)[@]+([A-Za-z0-9-_.]+)";
		patternStr = "(?:\\s|\\A)[@]+([\\p{L}0-9-_.]+)";
		pattern = Pattern.compile(patternStr);
		matcher = pattern.matcher(tweetText);
		while (matcher.find()) {
			result = matcher.group();
			result = result.replace(" ", "");
			String rawName = Charset.forName("UTF-8").encode(result.replace("@", "")).toString();
			
			String userHTML;
			if (source.equals("Instagram")) {
				userHTML = "<a href='instagram:/user?username" + rawName + "'>" + result + "</a>";
			}
			else {
				userHTML = "<a href=" + MyConstants.NEW_FEATURE_SERVER_NAME_PORT + "/user/"+ rawName +">"  + result + "</a>";

			}
			tweetText = tweetText.replace(result,userHTML);
		}
		return tweetText;
	}


	public static Set<String> searchHashTags(String tweetText)
	{
	//	String patternStr = "(?:\\s|\\A)[##]+([A-Za-z0-9-_]+)";
		String patternStr = "(?:\\s|\\A)[##]+([\\p{L}0-9-_]+)";
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(tweetText);

		String  result = "";
		Set<String> hashTags = new HashSet<String>();

		// Search for Hashtags
		while (matcher.find()) {
			result = matcher.group();
			result = result.replace(" ", "");
			String search = result.replace("#", "");
			hashTags.add(search);
		}

		return hashTags;

	}


}

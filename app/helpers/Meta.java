package helpers;

import flexjson.JSONSerializer;

// Class used to provide additional key / values in the JSON response
public class Meta {

	public String code = "200";
	public String error_message = "OK";
	
	public String toJson()
	{
		JSONSerializer serializer = new JSONSerializer();
        return serializer
        		.exclude("*.class")
        		.serialize(this);
	}
	
}

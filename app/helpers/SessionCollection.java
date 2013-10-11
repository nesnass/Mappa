package helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import models.Session;

/**
 * @author Richard Nesnass
 */

public class SessionCollection implements Iterable<Session> {
	public  String type = "SessionCollection";
	public ArrayList<Session> sessions;
	public Meta meta = new Meta();
	
	public SessionCollection() {
		this.sessions = new ArrayList<Session>();
	}
	
	public SessionCollection(Collection<Session> sessions) {
		this();
		for(Session s : sessions)
		{
			this.sessions.add(s);
		}
	}
	
	public void add(Collection<Session> sc) {
		for(Session s : sc)
		{
			this.sessions.add(s);
		}
	}
	
	public void add(Session s) {
		this.sessions.add(s);
	}
	
	public List<Session> getSessions() {
		return sessions;
	}
	
	public String toJson()
	{ 
		Iterator<Session> it = sessions.iterator();
		Session s = null;
		String jsonString = "{\"type\" : \"SessionCollection\", \"meta\" : " + meta.toJson() + ", \"sessions\":[";
		while(it.hasNext())
		{
			s = it.next();
			jsonString += s.toJson();
			if(it.hasNext())
				jsonString += ",";
		}
		jsonString += "]}";
		return jsonString;
	}

	@Override
	public Iterator<Session> iterator() {
		return sessions.iterator();
	}
	
}
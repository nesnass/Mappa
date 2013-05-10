package helpers;

// Adapted from JavaScript:  http://www.movable-type.co.uk/scripts/latlong.html
public class GeoCalculations {
    public static final double R = 6371; // In kilometers
    
    // Returns distance from one point to the other
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
        return R * c; // distance in kilometers
    }
    
    // Returns in degrees - lat2 and lon2 away from lat1 and lon1 given distance and bearing
    // supply: radius in meters, all else in degrees
    public static double[] destinationCoordsFromDistance(double lat, double lon, double bearing, double distance) {  // distance supplied in meters
    	distance = distance/(R*1000);
    	bearing = Math.toRadians(bearing);
    	double[] destination = new double[2];
    	lat = Math.toRadians(lat);
    	lon = Math.toRadians(lon);
    	destination[0] = Math.asin( Math.sin(lat)*Math.cos(distance) + Math.cos(lat)*Math.sin(distance)*Math.cos(bearing) );
    	destination[1] = lon + Math.atan2(Math.sin(bearing)*Math.sin(distance)*Math.cos(lat), Math.cos(distance)-Math.sin(lat)*Math.sin(destination[0]) );
    	destination[0] = Math.toDegrees(destination[0]);
    	destination[1] = Math.toDegrees(destination[1]); 
    	return destination;
    }
    
    // Returns in degrees - midpoint coordinates given start and end coordinates
    public static double[] midpointCoordsFromStartEndCoords(double lat1, double lon1, double lat2, double lon2)
    {
    	double[] destination = new double[2];
    	double dLon = Math.toRadians(lon2-lon1);
    	lat1 = Math.toRadians(lat1);
    	lon1 = Math.toRadians(lon1);
    	lat2 = Math.toRadians(lat2);
    	lon2 = Math.toRadians(lon2);
    	double Bx = Math.cos(lat2) * Math.cos(dLon);
    	double By = Math.cos(lat2) * Math.sin(dLon);
    	destination[0] = Math.toDegrees( Math.atan2(Math.sin(lat1)+Math.sin(lat2), Math.sqrt( (Math.cos(lat1)+Bx)*(Math.cos(lat1)+Bx) + By*By ) ) ); 
    	destination[1] = Math.toDegrees( lon1 + Math.atan2(By, Math.cos(lat1) + Bx) );
    	return destination;
    }
}

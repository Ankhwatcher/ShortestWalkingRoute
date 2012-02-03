package ie.appz.shortestwalkingroute;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;
/* Rory Glynn DT081/4
 * Implements location information gathering from http://developer.android.com/guide/topics/location/obtaining-user-location.html
 * And displays it to screen.
 * */
public class ShortestWalkingRouteActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
     // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener networkListener = new LocationListener() {
            public void onLocationChanged(Location location)
            {
              // Called when a new location is found by the network location provider.
              makeUseOfNetwork(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
          };
          LocationListener gpsListener = new LocationListener() {
              public void onLocationChanged(Location location)
              {
                // Called when a new location is found by the network location provider.
                makeUseOfGPS(location);
              }

              public void onStatusChanged(String provider, int status, Bundle extras) {}

              public void onProviderEnabled(String provider) {}

              public void onProviderDisabled(String provider) {}
            };

        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
        //The two integers in this request are the time and distance intervals of notifications respectively.
    }

	protected void makeUseOfNetwork(Location location)
	{
		TextView networkDisplay =new TextView(this); 
		networkDisplay=(TextView)findViewById(R.id.network_display);
		
		String latitude = new Double(location.getLatitude()).toString();
		String longitude = new Double(location.getLongitude()).toString();
		networkDisplay.setText(latitude +","+ longitude);
		/*networkDisplay.setText(location.toString());*/
		
	}

	protected void makeUseOfGPS(Location location)
	{
		//Grab the gps_display TextView
		TextView gpsDisplay =new TextView(this); 
		gpsDisplay=(TextView)findViewById(R.id.gps_display);
		
		//Convert the location's longitude and latitude properties into strings
		String latitude = new Double(location.getLatitude()).toString();
		String longitude = new Double(location.getLongitude()).toString();
		
		//Display the longitude and latitude on screen
		gpsDisplay.setText(latitude +","+ longitude);
		
	}
}
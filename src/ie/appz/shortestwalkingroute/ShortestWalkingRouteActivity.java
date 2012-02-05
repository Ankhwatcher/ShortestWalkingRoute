package ie.appz.shortestwalkingroute;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.format.Time;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;
/* Rory Glynn DT081/4
 * Implements location information gathering from http://developer.android.com/guide/topics/location/obtaining-user-location.html
 * And displays it to screen.
 * */
public class ShortestWalkingRouteActivity extends Activity
{
	String networkLocation;
	Long startMillis;
	LocationListener gpsListener, networkListener;
	LocationManager locationManager;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
     // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        
        //Start Count
        Time startTime = new Time();
        startTime.setToNow();
        startMillis = startTime.toMillis(false);
        
        // Define a listener that responds to location updates
        networkListener = new LocationListener() {
            public void onLocationChanged(Location location)
            {
              // Called when a new location is found by the network location provider.
              makeUseOfNetwork(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
          };
          gpsListener = new LocationListener() {
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
		/*TextView networkDisplay =new TextView(this); 
		networkDisplay=(TextView)findViewById(R.id.network_display);*/
		//Convert the location's longitude and latitude properties into strings
		String latitude = new Double(location.getLatitude()).toString();
		String longitude = new Double(location.getLongitude()).toString();
		
		//Combine the values into a single String to be displayed when the GPS location next updates
		networkLocation = latitude +","+ longitude;
		/*networkDisplay.setText(latitude +","+ longitude);
		networkDisplay.setText(location.toString());*/
		
	}

	protected void makeUseOfGPS(Location location)
	{
		Time currentTime = new Time();
        currentTime.setToNow();
        long currentMillis = currentTime.toMillis(false);
        long runningMillis = currentMillis - startMillis;
        
		//Grab the main_table
		TableLayout mainTable = (TableLayout)findViewById(R.id.main_table);
		//Create a new TableRow
		TableRow dynamicRow = new TableRow(this);
        dynamicRow.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
        //dynamicRow.setBackgroundResource(R.color.solid_black);
		//create the TextViews to contain the new values
        TextView networkDisplay = new TextView(this);
		networkDisplay.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
		networkDisplay.setTextColor(0xFF0000FF);
		TextView cellDivider = new TextView(this);
		cellDivider.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
		cellDivider.setTextColor(0xFF000000);
		TextView gpsDisplay =new TextView(this);
		gpsDisplay.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
		gpsDisplay.setTextColor(0xFFFF0000);
		
		//Convert the location's longitude and latitude properties into strings
		String latitude = new Double(location.getLatitude()).toString();
		String longitude = new Double(location.getLongitude()).toString();
		
		//Display the longitude and latitude on screen
		gpsDisplay.setText(latitude +","+ longitude);
		cellDivider.setText(R.string.pipe_divider);
		networkDisplay.setText(networkLocation);
		
		//Add both TextViews to the dynamicRow
		dynamicRow.addView(networkDisplay, 0);
		dynamicRow.addView(cellDivider,1);
		dynamicRow.addView(gpsDisplay, 2);
		
		/* Add row to TableLayout. */
        mainTable.addView(dynamicRow,new TableLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
        
        
        if(runningMillis > 300000)
        {
        	// Kill location Listeners
        	locationManager.removeUpdates(networkListener);
        	locationManager.removeUpdates(gpsListener);
        }
	}
}
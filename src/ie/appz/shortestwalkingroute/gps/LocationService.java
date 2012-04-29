package ie.appz.shortestwalkingroute.gps;

import ie.appz.shortestwalkingroute.sqlite.FixOpenHelper;
import ie.appz.shortestwalkingroute.sqlite.FixProvider;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.*;

public class LocationService extends Service {

	int mRouteNo;
	FixOpenHelper mFixOpenHelper;
	Location lastLocation;
	public static float oldAccuracy = 12;
	LocationManager mLocationManager;
	LocationListener networkListener, gpsListener;

	@Override
	public void onCreate() {
		super.onCreate();
		mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		networkListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				if (lastLocation == null || (location.getTime() - lastLocation.getTime()) > 10000) {
					addRow(location);
				}
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
				
			}
		};
		gpsListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				addRow(location);
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}
		};
		mFixOpenHelper = new FixOpenHelper(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			mRouteNo = intent.getIntExtra("HIGHESTROUTE", 1);
			SharedPreferences settings = getSharedPreferences(ie.appz.shortestwalkingroute.CaptureRouteActivity.PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt("HIGHESTROUTE", mRouteNo);
			editor.commit();
		}
		else
		{
			SharedPreferences settings = getSharedPreferences(ie.appz.shortestwalkingroute.CaptureRouteActivity.PREFS_NAME, 0);
			mRouteNo = settings.getInt("HIGHESTROUTE", 1);
		}
		/*
		 * Register the listener with the Location Manager to receive location
		 * updates
		 */
		Log.d(LocationService.class.getName(), "Starting capture of GPS data for route " + mRouteNo);
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 5, networkListener);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, gpsListener);
		/*
		 * The two integers in this request are the time (ms) and distance (m)
		 * intervals of notifications respectively.
		 */
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	protected void addRow(Location location) {
		/*
		 * This will reject any GPS fix with very poor accuracy
		 */
		if (location.getAccuracy() < 100 && location.getAccuracy() < 2 * oldAccuracy) {
			Log.d(LocationService.class.getName(), "Adding fix to " + FixOpenHelper.TABLE_NAME + " in route number "
					+ mRouteNo + ". Fix provided by " + location.getProvider());
			mFixOpenHelper.addFix(mRouteNo, location);
			lastLocation = location;
			oldAccuracy = (oldAccuracy + location.getAccuracy()) / 2;
			Uri baseUri = Uri.withAppendedPath(FixProvider.CONTENT_URI, FixProvider.ROUTE + "/" + mRouteNo);

			ContentResolver contentResolver = this.getContentResolver();
			contentResolver.notifyChange(baseUri, null);
		} else
			Log.d(LocationService.class.getName(), "Rejected fix for " + FixOpenHelper.TABLE_NAME + " in route number "
					+ mRouteNo + " because accuracy is " + location.getAccuracy()+ ". Fix provided by " + location.getProvider());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mLocationManager.removeUpdates(networkListener);
		mLocationManager.removeUpdates(gpsListener);

		if (mFixOpenHelper != null) {
			mFixOpenHelper.close();
		}
		
	
	}
	
}

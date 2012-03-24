package ie.appz.shortestwalkingroute;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.format.Time;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

/* Rory Glynn DT081/4
 * Implements location information gathering from http://developer.android.com/guide/topics/location/obtaining-user-location.html
 * And displays it to screen.
 * */
public class CaptureRouteActivity extends Activity {

	public static final String PREFS_NAME = "ROUTE_PREFS";
	private int highestRoute = 0;
	private static final int CAPTURING_ROUTE = 1;

	LocationListener gpsListener, networkListener;
	LocationManager locationManager;
	Location lastLocation = null;
	FixOpenHelper fixHelper = new FixOpenHelper(this);
	ProgressBar captureProgress;
	Button captureButton = null;
	Context getThis = this;

	private OnClickListener startCapture = new OnClickListener() {

		public void onClick(View v) {
			captureButton.setText(R.string.stop);
			captureButton.setOnClickListener(stopCapture);

			locationManager = (LocationManager) CaptureRouteActivity.this.getSystemService(Context.LOCATION_SERVICE);

			/*
			 * Start Count Time startTime = new Time(); startTime.setToNow();
			 * startMillis = startTime.toMillis(false);
			 */

			// Define a listener that responds to location updates
			networkListener = new LocationListener() {
				public void onLocationChanged(Location location) {
					// Called when a new location is found by the network
					// location
					// provider.
					makeUseOfNetwork(location);
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
					// Called when a new location is found by the network
					// location
					// provider.
					makeUseOfGPS(location);
				}

				public void onStatusChanged(String provider, int status, Bundle extras) {
				}

				public void onProviderEnabled(String provider) {
				}

				public void onProviderDisabled(String provider) {
				}
			};

			/*
			 * Register the listener with the Location Manager to receive
			 * location updates
			 */
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 2, networkListener);
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 2, gpsListener);
			/*
			 * The two integers in this request are the time (ms) and distance
			 * (m) intervals of notifications respectively.
			 */

			highestRoute = fixHelper.highestRoute();
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			if (!settings.getBoolean("capturingRoute", false)) {
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean("capturingRoute", true);
				editor.commit();
				highestRoute++;
				Context context = getApplicationContext();
				CharSequence text = "Now Capturing Route Number: " + highestRoute;
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(context, text, duration);
				toast.show();

			}
			// Show progress spinner
			captureProgress.setVisibility(0);

			CharSequence contentText = "Capturing Route: " + highestRoute;
			Notification(contentText);

		}

	};

	private OnClickListener stopCapture = new OnClickListener() {

		@Override
		public void onClick(View v) {
			captureButton.setText(R.string.start);
			captureButton.setOnClickListener(startCapture);

			// Hide spinner so user knows that location is no longer capturing
			captureProgress.setVisibility(4);
			// Kill location Listeners
			locationManager.removeUpdates(networkListener);
			locationManager.removeUpdates(gpsListener);

			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			// Set capturingRoute to false
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("capturingRoute", false);

			// Commit the edits!
			editor.commit();
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			notificationManager.cancel(CAPTURING_ROUTE);
		}

	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.captureroute);
		captureProgress = (ProgressBar) findViewById(R.id.captureProgress);

		captureButton = (Button) findViewById(R.id.captureButton);

		captureButton.setOnClickListener(startCapture);
		getThis = this;

	}

	protected void makeUseOfNetwork(Location location) {
		Time currentTime = new Time();
		currentTime.setToNow();
		long currentMillis = currentTime.toMillis(false);

		if (lastLocation == null || (currentMillis - lastLocation.getTime()) > 2000) {
			addRow(location);
		}
	}

	protected void makeUseOfGPS(Location location) {

		addRow(location);

	}

	protected void addRow(Location location) {
		/*
		 * Time currentTime = new Time(); currentTime.setToNow(); long
		 * currentMillis = currentTime.toMillis(false); long runningMillis =
		 * currentMillis - startMillis;
		 */

		// Convert the location's longitude and latitude properties into strings
		String latitude = new Double(location.getLatitude()).toString();
		String longitude = new Double(location.getLongitude()).toString();

		// Grab the main_table
		TableLayout mainTable = (TableLayout) findViewById(R.id.capture_table);
		// Create a new TableRow
		TableRow dynamicRow = new TableRow(this);
		dynamicRow.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

		// Create Divider to be used between Cells
		TextView cellDivider = new TextView(this);
		cellDivider.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		cellDivider.setTextColor(0xFF000000);
		TextView cellDivider2 = new TextView(this);
		cellDivider2.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		cellDivider2.setTextColor(0xFF000000);
		// create the TextViews to contain the new values
		TextView firstCell = new TextView(this);
		firstCell.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		firstCell.setTextColor(0xFF0000FF);
		TextView secondCell = new TextView(this);
		secondCell.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		secondCell.setTextColor(0xFFFF0000);
		TextView thirdCell = new TextView(this);
		thirdCell.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		thirdCell.setTextColor(0xFF00FF00);

		String accuracy = new Float(location.getAccuracy()).toString();
		String speed = new Float(location.getSpeed()).toString();

		cellDivider.setText(R.string.pipe_divider);
		cellDivider2.setText(R.string.pipe_divider);

		firstCell.setText(latitude + "," + longitude);
		secondCell.setText(accuracy);
		thirdCell.setText(speed);

		// Add both TextViews to the dynamicRow
		dynamicRow.addView(firstCell, 0);
		dynamicRow.addView(cellDivider, 1);
		dynamicRow.addView(secondCell, 2);
		dynamicRow.addView(cellDivider2, 3);
		dynamicRow.addView(thirdCell, 4);

		/* Add row to TableLayout. */
		mainTable
				.addView(dynamicRow, new TableLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

		fixHelper.addFix(highestRoute, location);

	}

	private void Notification(CharSequence contentText) {
		Intent notificationIntent = new Intent(this, CaptureRouteActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		Builder nCompatBuilder = new NotificationCompat.Builder(this);
		nCompatBuilder.setAutoCancel(false);
		nCompatBuilder.setOngoing(true);
		nCompatBuilder.setContentTitle(getString(R.string.app_name));
		nCompatBuilder.setContentText(contentText);
		nCompatBuilder.setContentIntent(contentIntent);

		nCompatBuilder.setSmallIcon(R.drawable.ic_menu_capture);

		Notification notification = nCompatBuilder.getNotification();

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(CAPTURING_ROUTE, notification);
	}

	@Override
	protected void onStop() {
		super.onStop();
		/*
		 * Setting this shared preference will prevent the application from
		 * continuing capturing the route when it is restarted.
		 * 
		 * SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		 * SharedPreferences.Editor editor = settings.edit();
		 * editor.putBoolean("capturingRoute", false); editor.commit();
		 */

		if (fixHelper != null) {
			fixHelper.close();
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		/*
		 * Setting this shared preference will prevent the application from
		 * continuing capturing the route when it is restarted.
		 */
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("capturingRoute", false);
		editor.commit();
	}
}

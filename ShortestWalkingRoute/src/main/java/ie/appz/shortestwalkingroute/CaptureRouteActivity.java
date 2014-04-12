package ie.appz.shortestwalkingroute;

import ie.appz.shortestwalkingroute.gps.LocationService;
import ie.appz.shortestwalkingroute.sqlite.FixOpenHelper;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * @author Rory Glynn
 */
public class CaptureRouteActivity extends FragmentActivity {
	public static final String PREFS_NAME = "ROUTE_PREFS";
	private int highestRoute = 0;
	private static final int CAPTURING_ROUTE = 1;
	private static NotificationManager notificationManager;
	private CaptureRouteFragment captureRouteFragment;
	Location lastLocation = null;
	FixOpenHelper fixHelper;
	Context context;

	/* Layout Items */
	private ProgressBar captureProgress;
	private Button captureButton = null;

	public class targetSingleChoiceListener implements
			DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {

		}
	}

	private class DialogButtonClickHandler implements
			DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int clicked) {
			switch (clicked) {
			case DialogInterface.BUTTON_POSITIVE:

				break;
			}
		}
	};

	protected Dialog onCreateDialog(int id) {

		// int highestRoute = fixHelper.highestRoute();

		// List<CharSequence> optionList = new ArrayList<CharSequence>();
		Cursor targetCursor = fixHelper.getTargetNames();
		/*
		 * if (targetCursor.moveToFirst()) { do {
		 * optionList.add(targetCursor.getString(0)); } while
		 * (targetCursor.moveToNext()); targetCursor.close();
		 */

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select Target Location");

		builder.setSingleChoiceItems(targetCursor, -1,
				FixOpenHelper.TARGET_NAME, new targetSingleChoiceListener());
		builder.setPositiveButton("OK", new DialogButtonClickHandler())
				.create();
		builder.setNegativeButton("New Target", new DialogButtonClickHandler())
				.create();
		Dialog dialog = builder.create();
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		WindowManager.LayoutParams WMLP = dialog.getWindow().getAttributes();
		dialog.getWindow().setAttributes(WMLP);
		WMLP.gravity = Gravity.TOP | Gravity.RIGHT;
		return dialog;
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/* Supress the default Title Bar because we will be using our own */
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.captureroute_list);
	}

	public void onResume() {
		super.onResume();
		fixHelper = new FixOpenHelper(this);
		captureButton = (Button) findViewById(R.id.capturelButton);
		captureProgress = (ProgressBar) findViewById(R.id.capturelProgress);

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		if (settings.getBoolean(getString(R.string.capturingroute), false)) {
			startCapture.onClick(captureButton);
		} else {
			captureButton.setOnClickListener(startCapture);
		}

		captureRouteFragment = (CaptureRouteFragment) getSupportFragmentManager()
				.findFragmentById(R.id.captureRouteFragment);
		captureRouteFragment.getListView().setScrollbarFadingEnabled(false);
		captureRouteFragment.getListView().setTranscriptMode(
				ListView.TRANSCRIPT_MODE_NORMAL);

	}

	private OnClickListener startCapture = new OnClickListener() {

		public void onClick(View v) {

			captureButton.setText(R.string.stop);
			captureButton.setOnClickListener(stopCapture);
			highestRoute = fixHelper.highestRoute();
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			if (!settings.getBoolean(getString(R.string.capturingroute), false)) {
				highestRoute++;

				captureRouteFragment.onQueryChange(highestRoute);

				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean(getString(R.string.capturingroute), true);
				editor.commit();

				Intent i = new Intent(CaptureRouteActivity.this,
						ie.appz.shortestwalkingroute.gps.LocationService.class);
				i.putExtra("HIGHESTROUTE", highestRoute);
				startService(i);

				CharSequence text = "Now Capturing Route Number: "
						+ highestRoute;
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(CaptureRouteActivity.this, text,
						duration);
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
			context = getApplicationContext();
			stopService(new Intent(context, LocationService.class));

			captureButton.setText(R.string.start);
			captureButton.setOnClickListener(startCapture);

			// Hide spinner so user knows that location is no longer
			// capturing
			captureProgress.setVisibility(4);

			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			// Set capturingRoute to false
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean(getString(R.string.capturingroute), false);
			// Commit the edits!
			editor.commit();

			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			notificationManager.cancel(CAPTURING_ROUTE);

			Cursor c = fixHelper.routeFixesTime(highestRoute);
			if (c.getCount() == 1) {
				fixHelper.rejectRoute(highestRoute);
			} else {
				String selectedRoutes = settings.getString(
						DisplayRoutesActivity.SELECTED_ROUTES, "");
				selectedRoutes = selectedRoutes + (char) (highestRoute - 1);
				editor.putString(DisplayRoutesActivity.SELECTED_ROUTES,
						selectedRoutes);
				editor.commit();
			}
			c.close();
		}

	};

	private void Notification(CharSequence contentText) {
		Intent notificationIntent = new Intent(this, CaptureRouteActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder nCompatBuilder = new NotificationCompat.Builder(
				this);
		nCompatBuilder.setAutoCancel(false);
		nCompatBuilder.setOngoing(true);
		nCompatBuilder.setContentTitle(getString(R.string.app_name));
		nCompatBuilder.setContentText(contentText);
		nCompatBuilder.setContentIntent(contentIntent);

		nCompatBuilder.setSmallIcon(R.drawable.ic_menu_capture);

		@SuppressWarnings("deprecation")
		Notification notification = nCompatBuilder.getNotification();

		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(CAPTURING_ROUTE, notification);
	}

	@Override
	protected void onPause() {
		super.onPause();

	}

	@Override
	public void onBackPressed() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		if (settings.getBoolean(getString(R.string.capturingroute), false)) {
			stopCapture.onClick(captureButton);
		}
		super.onBackPressed();
		return;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(CAPTURING_ROUTE);

		if (fixHelper != null) {
			fixHelper.close();
		}

	}

}

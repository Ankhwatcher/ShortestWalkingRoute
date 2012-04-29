package ie.appz.shortestwalkingroute;

import ie.appz.shortestwalkingroute.sqlite.FixOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.location.Location;
import android.os.Bundle;
import android.text.format.Time;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class DisplayRoutesActivity extends MapActivity {

	public static final String PREFS_NAME = "ROUTE_PREFS";
	public static final String MAP_SATELLITE = "mapSatellite";

	private static final String SELECTED_ROUTES = "selectedRoutes";
	FixOpenHelper fixHelper = new FixOpenHelper(this);

	public void switchSource(View switchSource) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		Boolean mapSatellite = !settings.getBoolean(MAP_SATELLITE, false);

		MapView mapView = (MapView) findViewById(R.id.route_map);
		mapView.setSatellite(mapSatellite);

		ImageButton sourceSwitch = (ImageButton) findViewById(R.id.sourceSwitch);
		if (mapSatellite) {
			sourceSwitch.setImageResource(R.drawable.ic_menu_display_satellite);
		} else {
			sourceSwitch.setImageResource(R.drawable.ic_menu_display);
		}

		editor.putBoolean(MAP_SATELLITE, mapSatellite);
		editor.commit();

	}

	protected OnClickListener changeRoute = new OnClickListener() {

		public void onClick(View v) {
			showDialog(0);
		}
	};

	public class DialogButtonClickHandler implements DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int clicked) {
			switch (clicked) {
			case DialogInterface.BUTTON_POSITIVE:

				break;
			}
		}
	};

	/*
	 * I want to hold all of the routes chosen for display in SharedPreferences.
	 * Ideally I would use an array of integers, but as SharedPreferences does
	 * not support arrays I will use a String value instead.
	 */
	public class routeMultiChoiceListener implements DialogInterface.OnMultiChoiceClickListener {
		public void onClick(DialogInterface dialog, int which, boolean isChecked) {
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			String selectedRoutes = settings.getString(SELECTED_ROUTES, "");
			String selectedRoutesN = "";
			MapView currentMap = (MapView) findViewById(R.id.route_map);
			int routeColor;
			if (isChecked) {
				/*
				 * If an item is to be added, I will assume it was not there
				 * already and simply add it to the String before any greater
				 * value.
				 */
				Boolean done = false;
				for (int i = 0; i < selectedRoutes.length(); i++) {
					if (selectedRoutes.charAt(i) > (char) which && !done) {
						selectedRoutesN = selectedRoutesN + (char) which;
						selectedRoutesN = selectedRoutesN + selectedRoutes.charAt(i);

						routeColor = randomColorGenerator(i);
						drawRoute(currentMap, which, routeColor, isChecked);
						done = true;
					} else {
						selectedRoutesN = selectedRoutesN + selectedRoutes.charAt(i);
					}

				}
				/*
				 * This will add the selection if it the greatest value so far.
				 */
				if (!done) {
					selectedRoutesN = selectedRoutes + (char) which;
					routeColor = randomColorGenerator(0);
					drawRoute(currentMap, which, routeColor, isChecked);
				}
			} else {
				/*
				 * If an item is removed I will filter through the String for
				 * the value and remove it. The map will be cleared and the
				 * routes that are wanted will be re-added. This does
				 * unfortunately cause all the colors to change whenever a route
				 * is removed.
				 */
				currentMap.getOverlays().clear();
				for (int i = 0; i < selectedRoutes.length(); i++) {

					if ((selectedRoutes.charAt(i) - (char) which) != 0) {
						selectedRoutesN = selectedRoutesN + selectedRoutes.charAt(i);
						// Re-Add route
						routeColor = randomColorGenerator(i);
						drawRoute(currentMap, selectedRoutes.charAt(i), routeColor, isChecked);
					}
					currentMap.invalidate();
				}

			}
			editor.putString(SELECTED_ROUTES, selectedRoutesN);
			editor.commit();

		}
	}

	protected Dialog onCreateDialog(int id) {

		int highestRoute = fixHelper.highestRoute();

		List<CharSequence> optionList = new ArrayList<CharSequence>();
		for (int i = 1; i < (highestRoute + 1); i++) {
			optionList.add("Route " + i);
		}

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String selectedRoutes = settings.getString(SELECTED_ROUTES, "");
		int j = 0;

		CharSequence[] options = optionList.toArray(new CharSequence[optionList.size()]);
		boolean[] selections = new boolean[options.length];
		if (selectedRoutes.length() >= 1) {
			for (int i = 0; i < selections.length; i++) {

				if (i == (int) selectedRoutes.charAt(j)) {
					selections[i] = true;
					j++;
					if (j == selectedRoutes.length()) {
						break;
					}
				}
			}
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.select_routes_to_display);
		builder.setMultiChoiceItems(options, selections, new routeMultiChoiceListener());
		builder.setPositiveButton("OK", new DialogButtonClickHandler()).create();
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
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.displayroutes);

		Button routeButton = (Button) findViewById(R.id.routeButton);
		routeButton.setOnClickListener(changeRoute);

		MapView mapView = (MapView) findViewById(R.id.route_map);
		mapView.setBuiltInZoomControls(true);
		mapView.setSatellite(false);

	}

	@Override
	public void onResume() {
		super.onResume();

		MapView mapView = (MapView) findViewById(R.id.route_map);
		ImageButton sourceSwitch = (ImageButton) findViewById(R.id.sourceSwitch);

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String selectedRoutes = settings.getString(SELECTED_ROUTES, "");
		Boolean mapSatellite = settings.getBoolean(MAP_SATELLITE, false);

		if (mapSatellite) {
			sourceSwitch.setImageResource(R.drawable.ic_menu_display_satellite);
			mapView.setSatellite(mapSatellite);
		}

		for (int i = 0; i < selectedRoutes.length(); i++) {

			int routeColor = randomColorGenerator(i);
			drawRoute(mapView, selectedRoutes.charAt(i), routeColor, false);
		}
		mapView.invalidate();

	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	protected void drawRoute(MapView mapView, int routeNo, int routeColor, Boolean isChecked) {
		routeNo++;

		Location fixLocation = new Location("Cursor"), lastLocation = new Location("Cursor");
		float totalDistance = 0, spreadDistance = 0;
		Boolean firstRun = true;
		GeoPoint fixGeo = null;
		ShapeDrawable shapedraw = new ShapeDrawable(new OvalShape());
		shapedraw.getPaint().setColor(routeColor);
		shapedraw.setIntrinsicHeight(18);
		shapedraw.setIntrinsicWidth(18);
		RouteItemizedOverlay tagOverlay = new RouteItemizedOverlay(shapedraw, this);

		ArrayList<GeoPoint> routePoints = new ArrayList<GeoPoint>();
		// int itemSpreader = 0;
		long startTime, fixTime, diffTime;

		Time totalTime = new Time();
		Time aTime = new Time();
		totalTime.set(fixHelper.totalRouteTime(routeNo));
		float averageSpeed = fixHelper.averageSpeed(routeNo);

		Cursor cursor = fixHelper.routeFixesTime(routeNo);

		if (cursor.moveToFirst()) {
			startTime = cursor.getLong(3);
			do {
				fixLocation.reset();
				fixLocation.setLatitude(cursor.getDouble(0));
				fixLocation.setLongitude(cursor.getDouble(1));
				fixLocation.setAccuracy(cursor.getFloat(2));
				float dt = fixLocation.distanceTo(lastLocation);

				if (firstRun || dt > lastLocation.getAccuracy() * 3) {
					if (!firstRun) {
						totalDistance = totalDistance + dt;
						spreadDistance = spreadDistance + dt;
					}

					fixGeo = new GeoPoint((int) (fixLocation.getLatitude() * 1E6),
							(int) (fixLocation.getLongitude() * 1E6));
					routePoints.add(fixGeo);

					fixTime = cursor.getLong(3);
					diffTime = fixTime - startTime;
					/*
					 * itemSpreader++; if ((itemSpreader % 10) == 0) {
					 */
					if (spreadDistance > 300) {
						spreadDistance = 0;
						fixTime = cursor.getLong(3);
						diffTime = fixTime - startTime;
						aTime = new Time();
						aTime.set(diffTime);

						tagOverlay.addOverlay(new OverlayItem(fixGeo, "Route " + routeNo, "Total Time "
								+ String.format("%02d", totalTime.toMillis(true) / (1000 * 60 * 60)) + ":"
								+ totalTime.format("%M:%S") + "\nAverage Speed "
								+ Math.floor((averageSpeed * 60 * 60) / 1000 * 100) / 100 + " km/h" + "\nPoint Time "
								+ String.format("%02d", aTime.toMillis(true) / (1000 * 60 * 60)) + ":"
								+ aTime.format("%M:%S") + "\nDistance Travelled " + distanceString(totalDistance)));
					}
					lastLocation.reset();
					lastLocation.set(fixLocation);
					firstRun = false;
				}

			} while (cursor.moveToNext());
			cursor.close();

			tagOverlay.addOverlay(new OverlayItem(fixGeo, "Route " + routeNo + "\nTotal Time "
					+ String.format("%02d", totalTime.toMillis(true) / (1000 * 60 * 60)) + ":"
					+ totalTime.format("%M:%S"), "Final Point" + "\nAverage Speed "
					+ Math.floor((averageSpeed * 60 * 60) / 1000 * 100) / 100 + " km/h" + "\nTotal Distance "
					+ distanceString(totalDistance)));
			mapView.getOverlays().add(new RouteOverlay(routePoints, routeColor));

			mapView.getOverlays().add(tagOverlay);
			if (isChecked) {
				mapView.invalidate();
			}
			MapController mapController = mapView.getController();
			mapController.animateTo(fixGeo);
			mapController.setZoom(16);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

		if (fixHelper != null) {
			fixHelper.close();
		}
	}

	private String distanceString(float totalDistance) {
		String distanceString;
		if (totalDistance > 1000) {
			distanceString = Math.floor((totalDistance / 1000) * 100) / 100 + " km";
		} else {
			distanceString = (int) Math.floor(totalDistance) + " m";
		}
		return distanceString;
	}

	public int randomColorGenerator(int run) {
		/* "run" is used when you are generating more than one color in a loop. */
		Time seedTime = new Time();
		seedTime.setToNow();
		Random randomColor = new Random(seedTime.toMillis(true) + run * 25);
		int returnColor = 0xFF000000 + randomColor.nextInt(0xFFFFFF);
		return returnColor;
	}

}

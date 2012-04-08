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
import android.os.Bundle;
import android.text.format.Time;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class DisplayRoutesActivity extends MapActivity {

	public static final String PREFS_NAME = "ROUTE_PREFS";

	private static final String SELECTED_ROUTES = "selectedRoutes";
	FixOpenHelper fixHelper = new FixOpenHelper(this);

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

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String selectedRoutes = settings.getString(SELECTED_ROUTES, "");

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

		GeoPoint fixGeo = null;
		ShapeDrawable shapedraw = new ShapeDrawable(new OvalShape());
		shapedraw.getPaint().setColor(routeColor);
		shapedraw.setIntrinsicHeight(18);
		shapedraw.setIntrinsicWidth(18);
		RouteItemizedOverlay tagOverlay = new RouteItemizedOverlay(shapedraw, this);

		ArrayList<GeoPoint> routePoints = new ArrayList<GeoPoint>();
		int itemSpreader = 0;
		long startTime, fixTime, diffTime;

		Time totalTime = new Time();
		Time aTime = new Time();
		totalTime.set(fixHelper.totalRouteTime(routeNo));

		Cursor cursor = fixHelper.routeFixesTime(routeNo);

		if (cursor.moveToFirst()) {
			startTime = cursor.getLong(3);
			do {

				fixGeo = new GeoPoint((int) (cursor.getDouble(0) * 1E6), (int) (cursor.getDouble(1) * 1E6));

				routePoints.add(fixGeo);
				itemSpreader++;
				fixTime = cursor.getLong(3);
				diffTime = fixTime - startTime;
				if ((itemSpreader % 25) == 0) {
					fixTime = cursor.getLong(3);
					diffTime = fixTime - startTime;
					aTime = new Time();
					aTime.set(diffTime);
					tagOverlay.addOverlay(new OverlayItem(fixGeo, "Route " + routeNo + "\nTotal Time "
							+ String.format("%02d", totalTime.toMillis(true) / (1000 * 60 * 60)) + ":"
							+ totalTime.format("%M:%S"), "Point Time "
							+ String.format("%02d", aTime.toMillis(true) / (1000 * 60 * 60)) + ":"
							+ aTime.format("%M:%S") + "\nAccuracy (m) " + cursor.getString(2)));
				}

			} while (cursor.moveToNext());
			cursor.close();
			tagOverlay.addOverlay(new OverlayItem(fixGeo, "Route " + routeNo + "\nTotal Time "
					+ String.format("%02d", totalTime.toMillis(true) / (1000 * 60 * 60)) + ":"
					+ totalTime.format("%M:%S"), "Final Point"));
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

	public int randomColorGenerator(int run) {
		/* "run" is used when you are generating more than one color in a loop. */
		Time seedTime = new Time();
		seedTime.setToNow();
		Random randomColor = new Random(seedTime.toMillis(true) + run * 25);
		int returnColor = 0xFF000000 + randomColor.nextInt(0xFFFFFF);
		return returnColor;
	}

}

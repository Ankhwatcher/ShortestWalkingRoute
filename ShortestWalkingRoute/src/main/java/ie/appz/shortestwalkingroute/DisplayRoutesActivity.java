package ie.appz.shortestwalkingroute;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ie.appz.shortestwalkingroute.sqlite.FixOpenHelper;
import ie.appz.shortestwalkingroute.sqlite.FixProvider;

/**
 * @author Rory
 */
public class DisplayRoutesActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String MAP_SATELLITE = "mapSatellite";

    public static final String SELECTED_ROUTES = "selectedRoutes";
    protected OnClickListener changeRoute = new OnClickListener() {

        public void onClick(View v) {
            int highestRoute = fixHelper.getHighestRouteNo();

            List<CharSequence> optionList = new ArrayList<>();
            for (int i = 1; i < (highestRoute + 1); i++) {
                optionList.add("Route " + i);
            }

            SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
            String selectedRoutes = settings.getString(SELECTED_ROUTES, "");
            int j = 0;

            CharSequence[] options = optionList.toArray(new CharSequence[optionList
                    .size()]);
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
            Dialog dialog = new AlertDialog.Builder(DisplayRoutesActivity.this)
                    .setTitle(R.string.select_routes_to_display)
                    .setMultiChoiceItems(options, selections, new routeMultiChoiceListener())
                    .setPositiveButton("OK", new DialogButtonClickHandler())
                    .create();
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//            WindowManager.LayoutParams WMLP = dialog.getWindow().getAttributes();
//            dialog.getWindow().setAttributes(WMLP);
//            WMLP.gravity = Gravity.TOP | Gravity.END;
            dialog.show();
        }
    };
    /**
     */
    FixOpenHelper fixHelper = new FixOpenHelper(this);
    private GoogleMap mGoogleMap;
    private SparseArray<List<Fix>> mRoutes;
    private ArrayList<Integer> mRouteColors;

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_routes);

        Button routeButton = (Button) findViewById(R.id.routeButton);
        routeButton.setOnClickListener(changeRoute);

        Time seedTime = new Time();
        seedTime.setToNow();
        Random mRandomColor = new Random(20141231);

        mRouteColors = new ArrayList<>();
        for (int i = 0; i <= fixHelper.getHighestRouteNo(); i++) {
            mRouteColors.add(0xFF000000 + mRandomColor.nextInt(0xFFFFFF));
        }

        mGoogleMap = ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.route_map)).getMap();
        mGoogleMap.setMyLocationEnabled(true);

        ImageButton sourceSwitch = (ImageButton) findViewById(R.id.sourceSwitch);
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        Boolean mapSatellite = settings.getBoolean(MAP_SATELLITE, false);
        if (mapSatellite) {
            sourceSwitch.setImageResource(R.drawable.ic_menu_display);
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        } else {
            sourceSwitch.setImageResource(R.drawable.ic_menu_display_satellite);
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        sourceSwitch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean mapSatellite = mGoogleMap.getMapType() == GoogleMap.MAP_TYPE_HYBRID;

                Log.i(this.getClass().getName(), "Switching Map Mode to " + (mapSatellite ? "Normal" : "Satellite"));

                SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();


                if (mapSatellite) {
                    ((ImageButton) v).setImageResource(R.drawable.ic_menu_display_satellite);
                    mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                } else {
                    ((ImageButton) v).setImageResource(R.drawable.ic_menu_display);
                    mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                }
                editor.putBoolean(MAP_SATELLITE, !mapSatellite);
                editor.apply();
            }
        });

        getSupportLoaderManager().initLoader(10002, null, this);
    }


    protected void drawRoute(GoogleMap googleMap, int routeNo, int routeColor, List<Fix> fixes) {
        Log.d(this.getClass().getName(), "Drawing route " + (routeNo + 1));
        if (fixes.size() < 2)
            return;


        routeNo++;

        Location fixLocation = new Location("Cursor"), lastLocation = new Location("Cursor");
        float totalDistance = 0, spreadDistance = 0;
        Boolean firstRun = true;
        LatLng fixLatLng = null;
        ShapeDrawable shapeDraw = new ShapeDrawable(new OvalShape());
        shapeDraw.getPaint().setColor(routeColor);
        shapeDraw.setIntrinsicHeight(32);
        shapeDraw.setIntrinsicWidth(32);
        Bitmap shapeBitmap = drawableToBitmap(shapeDraw);

        ArrayList<LatLng> routePoints = new ArrayList<>();

        Long startTime = null, fixTime, diffTime;

        Time totalTime = new Time();
        Time aTime;
        totalTime.set(fixHelper.totalRouteTime(routeNo));
        float averageSpeed = fixHelper.averageSpeed(routeNo);


        for (Fix fix : fixes) {

            if (startTime == null) {
                startTime = fix.getTime();
            }
            fixLocation.reset();
            fixLocation.setLatitude(fix.getLatitude());
            fixLocation.setLongitude(fix.getLongitude());
            fixLocation.setAccuracy(fix.getAccuracy());
            float dt = fixLocation.distanceTo(lastLocation);

            if (firstRun || dt > lastLocation.getAccuracy() * 3) {
                if (!firstRun) {
                    totalDistance = totalDistance + dt;
                    spreadDistance = spreadDistance + dt;
                }

                fixLatLng = new LatLng(fixLocation.getLatitude(), fixLocation.getLongitude());
                routePoints.add(fixLatLng);

                if (spreadDistance > 300 || firstRun) {
                    spreadDistance = 0;
                    fixTime = fix.getTime();
                    diffTime = fixTime - startTime;
                    aTime = new Time();
                    aTime.set(diffTime);

                    googleMap.addMarker(new MarkerOptions()
                            .position(fixLatLng)
                            .title("Route " + routeNo
                                    + " - Total Time " + String.format("%02d", totalTime.toMillis(true) / (1000 * 60 * 60))
                                    + ":" + totalTime.format("%M:%S")
                                    + " Average Speed "
                                    + Math.floor((averageSpeed * 60 * 60) / 1000 * 100)
                                    / 100 + "kph")
                            .snippet(
                                    "Point Time "
                                            + String.format(
                                            "%02d",
                                            aTime.toMillis(true)
                                                    / (1000 * 60 * 60)
                                    )
                                            + ":" + aTime.format("%M:%S")
                                            + " Distance Travelled "
                                            + distanceString(totalDistance)
                            )
                            .icon(BitmapDescriptorFactory.fromBitmap(shapeBitmap))
                            .anchor(0.5f, 0.5f));

                }
                lastLocation.reset();
                lastLocation.set(fixLocation);
                firstRun = false;
            }

        }

        googleMap.addMarker(new MarkerOptions()
                .position(fixLatLng)
                .title("Route "
                        + routeNo
                        + " - Final Point"
                        + " - Total Time "
                        + String.format("%02d", totalTime.toMillis(true)
                        / (1000 * 60 * 60)) + ":"
                        + totalTime.format("%M:%S"))
                .snippet(
                        " Average Speed "
                                + Math.floor((averageSpeed * 60 * 60) / 1000 * 100)
                                / 100 + " km/h" + "\nTotal Distance "
                                + distanceString(totalDistance)
                )
                .icon(BitmapDescriptorFactory.fromBitmap(shapeBitmap)));

        googleMap.addPolyline(new PolylineOptions().addAll(routePoints)
                .color(routeColor).width(12f));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(fixLatLng, 16));

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


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        Uri baseUri = Uri.withAppendedPath(FixProvider.CONTENT_URI, FixProvider.ROUTE);

        String[] projection = {FixOpenHelper.COLUMN_ID, FixOpenHelper.LATITUDE, FixOpenHelper.LONGITUDE, FixOpenHelper.ACCURACY, FixOpenHelper.TIME, FixOpenHelper.ROUTE_NUMBER};

        return new CursorLoader(this, baseUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mGoogleMap.clear();
        mRoutes = new SparseArray<>();

        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String selectedRoutes = settings.getString(SELECTED_ROUTES, "");

        if (data.moveToFirst())
            do {
                List<Fix> fixArrayList = mRoutes.get(data.getInt(data.getColumnIndex(FixOpenHelper.ROUTE_NUMBER)) - 1, new ArrayList<Fix>());
                fixArrayList.add(new Fix(data.getDouble(data.getColumnIndex(FixOpenHelper.LATITUDE)),
                        data.getDouble(data.getColumnIndex(FixOpenHelper.LONGITUDE)),
                        data.getFloat(data.getColumnIndex(FixOpenHelper.ACCURACY)),
                        data.getLong(data.getColumnIndex(FixOpenHelper.TIME))));
                mRoutes.put(data.getInt(data.getColumnIndex(FixOpenHelper.ROUTE_NUMBER)) - 1, fixArrayList);
            } while (data.moveToNext());

        for (int i = 0; i < selectedRoutes.length(); i++) {


            List<Fix> fixes = mRoutes.get(selectedRoutes.charAt(i), new ArrayList<Fix>());

            drawRoute(mGoogleMap, selectedRoutes.charAt(i), mRouteColors.get(selectedRoutes.charAt(i)), fixes);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        mGoogleMap.clear();
    }

    class Fix {
        private double latitude;
        private double longitude;
        private float accuracy;
        private long time;

        Fix(double latitude, double longitude, float accuracy, long time) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracy = accuracy;
            this.time = time;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public float getAccuracy() {
            return accuracy;
        }

        public long getTime() {
            return time;
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
    }

    /*
     * I want to hold all of the routes chosen for display in SharedPreferences.
     * Ideally I would use an array of integers, but as SharedPreferences does
     * not support arrays I will use a String value instead.
     */
    public class routeMultiChoiceListener implements DialogInterface.OnMultiChoiceClickListener {
        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
            SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            String selectedRoutes = settings.getString(SELECTED_ROUTES, "");
            String selectedRoutesN = "";


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


                        drawRoute(mGoogleMap, which, mRouteColors.get(which), mRoutes.get(which, new ArrayList<Fix>()));
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

                    drawRoute(mGoogleMap, which, mRouteColors.get(which), mRoutes.get(which, new ArrayList<Fix>()));
                }
            } else {
                /*
                 * If an item is removed I will filter through the String for
				 * the value and remove it. The map will be cleared and the
				 * routes that are wanted will be re-added.
				 */
                mGoogleMap.clear();

                for (int i = 0; i < selectedRoutes.length(); i++) {

                    if (selectedRoutes.charAt(i) != (char) which) {
                        selectedRoutesN = selectedRoutesN + selectedRoutes.charAt(i);
                        // Re-Add route
                        drawRoute(mGoogleMap, selectedRoutes.charAt(i), mRouteColors.get(selectedRoutes.charAt(i)), mRoutes.get(selectedRoutes.charAt(i), new ArrayList<Fix>()));
                    }
                }
            }
            editor.putString(SELECTED_ROUTES, selectedRoutesN);
            editor.apply();
        }
    }
}

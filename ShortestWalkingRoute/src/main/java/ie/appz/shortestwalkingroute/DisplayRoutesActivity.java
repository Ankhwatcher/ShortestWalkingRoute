package ie.appz.shortestwalkingroute;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
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

import java.io.IOException;
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
    private OnClickListener changeRoute = new OnClickListener() {

        public void onClick(View v) {
            List<CharSequence> optionList = new ArrayList<>();

            for (int i = 0; i < mRoutes.size(); i++) {
                optionList.add(getString(R.string.route_x, mRoutes.keyAt(i) + 1));
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
            dialog.show();
        }
    };
    private final FixOpenHelper fixHelper = new FixOpenHelper(this);
    private GoogleMap mGoogleMap;
    private SparseArray<List<Fix>> mRoutes;
    private ArrayList<Integer> mRouteColors;
    private ShareActionProvider mShareActionProvider;

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

    private static String distanceString(float totalDistance) {
        String distanceString;
        if (totalDistance > 1000) {
            distanceString = Math.floor((totalDistance / 1000) * 100) / 100 + " km";
        } else {
            distanceString = (int) Math.floor(totalDistance) + " m";
        }
        return distanceString;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.display_routes, menu);

        // Get the menu item.
        MenuItem menuItem = menu.findItem(R.id.action_share);
        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        setShareIntent(-1);

        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (fixHelper != null) {
            fixHelper.close();
        }
    }

    private void setShareIntent(int routeNo) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        if (routeNo == -1) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
        } else {
            /* It would be nice to be able to share an image of the full route, but for the time being lets just share some info about the route. */
            List<Fix> fixes = mRoutes.get(routeNo);
            Fix startFix = fixes.get(0);
            Fix endFix = fixes.get(fixes.size() - 1);
            Geocoder geocoder = new Geocoder(this);
            String startAddress = null, endAddress = null;
            try {
                startAddress = geocoder.getFromLocation(startFix.latitude, startFix.longitude, 1).get(0).getAddressLine(0);
                endAddress = geocoder.getFromLocation(endFix.latitude, endFix.longitude, 1).get(0).getAddressLine(0);
            } catch (IOException ignored) {
            }
            startAddress = startAddress == null ? startFix.getLatitude() + "," + startFix.getLongitude() : startAddress;
            endAddress = endAddress == null ? endFix.getLatitude() + "," + endFix.getLongitude() : endAddress;
            String totalTime = DateUtils.formatElapsedTime(fixHelper.totalRouteTime(routeNo) / 1000);

            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text_route, startAddress, endAddress, totalTime, routeNo + 1, fixHelper.averageSpeed(routeNo)));
        }

        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
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
                List<Fix> fixArrayList = mRoutes.get(data.getInt(data.getColumnIndex(FixOpenHelper.ROUTE_NUMBER)), new ArrayList<Fix>());
                fixArrayList.add(new Fix(data.getDouble(data.getColumnIndex(FixOpenHelper.LATITUDE)),
                        data.getDouble(data.getColumnIndex(FixOpenHelper.LONGITUDE)),
                        data.getFloat(data.getColumnIndex(FixOpenHelper.ACCURACY)),
                        data.getLong(data.getColumnIndex(FixOpenHelper.TIME))));
                mRoutes.put(data.getInt(data.getColumnIndex(FixOpenHelper.ROUTE_NUMBER)), fixArrayList);
            } while (data.moveToNext());

        if (selectedRoutes.length() == 1) {
            setShareIntent(selectedRoutes.charAt(0));
        }

        for (int i = 0; i < selectedRoutes.length(); i++) {

            List<Fix> fixes = mRoutes.get(selectedRoutes.charAt(i), new ArrayList<Fix>());

            drawRoute(mGoogleMap, selectedRoutes.charAt(i), mRouteColors.get(selectedRoutes.charAt(i)), fixes);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        mGoogleMap.clear();
    }

    protected void drawRoute(GoogleMap googleMap, int routeNo, int routeColor, List<Fix> fixes) {
        if (fixes.size() < 2)
            return;
        //Increment the route number for display (because computers count from 0 but people count from 1
        int displayRouteNo = routeNo + 1;
        Location fixLocation = new Location("Cursor"), lastLocation = new Location("Cursor");
        float totalDistance = 0, spreadDistance = 0;
        Boolean firstPoint = true;
        LatLng fixLatLng = null;
        ShapeDrawable shapeDraw = new ShapeDrawable(new OvalShape());
        shapeDraw.getPaint().setColor(routeColor);
        shapeDraw.setIntrinsicHeight(32);
        shapeDraw.setIntrinsicWidth(32);
        Bitmap shapeBitmap = drawableToBitmap(shapeDraw);

        ArrayList<LatLng> routePoints = new ArrayList<>();

        Long startTime = null, fixTime, diffTime;


        String totalTime = DateUtils.formatElapsedTime(fixHelper.totalRouteTime(routeNo) / 1000);

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

            if (firstPoint || dt > lastLocation.getAccuracy() * 3) {
                if (!firstPoint) {
                    totalDistance = totalDistance + dt;
                    spreadDistance = spreadDistance + dt;
                }

                fixLatLng = new LatLng(fixLocation.getLatitude(), fixLocation.getLongitude());
                routePoints.add(fixLatLng);

                if (spreadDistance > 300 || firstPoint) {
                    spreadDistance = 0;
                    fixTime = fix.getTime();
                    diffTime = fixTime - startTime;
                    String aTime = DateUtils.formatElapsedTime(diffTime / 1000);

                    googleMap.addMarker(new MarkerOptions()
                            .position(fixLatLng)
                            .title(getString(R.string.route_point_title, displayRouteNo, totalTime,
                                    Math.floor((averageSpeed * 60 * 60) / 1000 * 100) / 100))
                            .snippet(getString(R.string.route_point_snippet, aTime, distanceString(totalDistance)))
                            .icon(BitmapDescriptorFactory.fromBitmap(shapeBitmap))
                            .anchor(0.5f, 0.5f));

                }
                lastLocation.reset();
                lastLocation.set(fixLocation);
                firstPoint = false;
            }

        }
        //Add a marker for the final point
        googleMap.addMarker(new MarkerOptions()
                .position(fixLatLng)
                .title(getString(R.string.route_point_title_final, displayRouteNo, totalTime))
                .snippet(getString(R.string.route_point_snippet_final,
                        Math.floor((averageSpeed * 60 * 60) / 1000 * 100) / 100, distanceString(totalDistance)))
                .icon(BitmapDescriptorFactory.fromBitmap(shapeBitmap)));

        googleMap.addPolyline(new PolylineOptions().addAll(routePoints)
                .color(routeColor).width(12f));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(fixLatLng, 16));

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

            if (selectedRoutesN.length() == 1) {
                setShareIntent(selectedRoutesN.charAt(0));
            }
        }
    }
}

package ie.appz.shortestwalkingroute;

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
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import ie.appz.shortestwalkingroute.gps.LocationService;
import ie.appz.shortestwalkingroute.sqlite.FixOpenHelper;

/**
 * @author Rory Glynn
 */
public class CaptureRouteActivity extends ActionBarActivity implements ActionBar.OnNavigationListener {
    public static final String PREFS_NAME = "ROUTE_PREFS";
    public static final String NOTIFY_RANGE = "notify_range";
    private static final int CAPTURING_ROUTE = 1;
    private static NotificationManager notificationManager;
    Location lastLocation = null;
    FixOpenHelper fixHelper;
    Context context;
    private int highestRoute = MODE_PRIVATE;
    private CaptureRouteFragment captureRouteFragment;
    private boolean captureStopped = true;
    private MenuItem startCaptureMenuItem;
    private int itemPosition = 0;
    private int[] distanceArray = {0, 100, 200, 300, 400};

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putInt(NOTIFY_RANGE, distanceArray[itemPosition]).commit();
        return false;
    }

    protected Dialog onCreateDialog(int id) {

        // int highestRoute = fixHelper.highestRoute();

        // List<CharSequence> optionList = new ArrayList<CharSequence>();
        Cursor targetCursor = fixHelper.getTargetNames();
        /*
         * if (targetCursor.moveToFirst()) { do {
		 * optionList.add(targetCursor.getString(MODE_PRIVATE)); } while
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
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.captureroute_list);
        setProgressBarIndeterminateVisibility(false);
        ArrayAdapter<CharSequence> mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.notify_distances, android.R.layout.simple_spinner_dropdown_item);
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        getSupportActionBar().setListNavigationCallbacks(mSpinnerAdapter, this);
        int notifyRange = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(CaptureRouteActivity.NOTIFY_RANGE, 0);
        for (int i = 0; i < distanceArray.length; i++) {
            if (notifyRange == distanceArray[i])
                getSupportActionBar().setSelectedNavigationItem(i);
        }

    }

    ;

    public void onResume() {
        super.onResume();
        fixHelper = new FixOpenHelper(this);


        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        if (settings.getBoolean(getString(R.string.capturingroute), false)) {
            startCapture();
        }
        captureRouteFragment = (CaptureRouteFragment) getSupportFragmentManager().findFragmentById(R.id.captureRouteFragment);
        captureRouteFragment.getListView().setScrollbarFadingEnabled(false);
        captureRouteFragment.getListView().setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);

    }

    private void startCapture() {
        captureStopped = false;

        startCaptureMenuItem.setIcon(android.R.drawable.ic_media_pause);
        startCaptureMenuItem.setTitle(getString(R.string.start));
        highestRoute = fixHelper.highestRoute();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
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
        setProgressBarIndeterminateVisibility(true);

        CharSequence contentText = "Capturing Route: " + highestRoute;
        Notification(contentText);

    }

    private void stopCapture() {
        captureStopped = true;
        startCaptureMenuItem.setIcon(android.R.drawable.ic_media_play);
        startCaptureMenuItem.setTitle(getString(R.string.stop));
        stopService(new Intent(CaptureRouteActivity.this, LocationService.class));
        setProgressBarIndeterminateVisibility(false);
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(getString(R.string.capturingroute), false).commit();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(CAPTURING_ROUTE);

        Cursor c = fixHelper.routeFixesTime(highestRoute);
        if (c.getCount() == 1) {
            fixHelper.rejectRoute(highestRoute);
        } else {
            String selectedRoutes = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(DisplayRoutesActivity.SELECTED_ROUTES, "");
            selectedRoutes = selectedRoutes + (char) (highestRoute - 1);
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(DisplayRoutesActivity.SELECTED_ROUTES, selectedRoutes).commit();
        }
        c.close();
    }

    private void Notification(CharSequence contentText) {
        Intent notificationIntent = new Intent(this, CaptureRouteActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(this, MODE_PRIVATE,
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
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.capture_route, menu);
        startCaptureMenuItem = menu.findItem(R.id.action_start);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_start: {
                if (captureStopped) {
                    startCapture();
                } else {
                    stopCapture();
                }
            }
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (settings.getBoolean(getString(R.string.capturingroute), false)) {
            stopCapture();
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
    }

}

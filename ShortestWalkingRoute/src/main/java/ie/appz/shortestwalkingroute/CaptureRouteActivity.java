package ie.appz.shortestwalkingroute;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import ie.appz.shortestwalkingroute.gps.LocationService;
import ie.appz.shortestwalkingroute.sqlite.FixOpenHelper;

/**
 * @author Rory Glynn
 */
public class CaptureRouteActivity extends ActionBarActivity implements ActionBar.OnNavigationListener {
    public static final String NOTIFY_RANGE = "notify_range";
    private static final int CAPTURING_ROUTE = 1;
    FixOpenHelper fixHelper;
    private int highestRoute = MODE_PRIVATE;
    private boolean captureStopped = true;
    private MenuItem startCaptureMenuItem;
    private int[] distanceArray = {0, 100, 200, 300, 400};

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).edit().putInt(NOTIFY_RANGE, distanceArray[itemPosition]).commit();
        return false;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_route);
        setSupportProgressBarIndeterminateVisibility(false);
        ArrayAdapter<CharSequence> mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.notify_distances, android.R.layout.simple_spinner_dropdown_item);
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        getSupportActionBar().setListNavigationCallbacks(mSpinnerAdapter, this);
        int notifyRange = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).getInt(CaptureRouteActivity.NOTIFY_RANGE, 0);
        for (int i = 0; i < distanceArray.length; i++) {
            if (notifyRange == distanceArray[i])
                getSupportActionBar().setSelectedNavigationItem(i);
        }


    }


    public void onResume() {
        super.onResume();
        fixHelper = new FixOpenHelper(this);

        if (isMyServiceRunning(LocationService.class)) {
            startCapture();
        }
    }

    private void startCapture() {
        captureStopped = false;
        if (startCaptureMenuItem != null) {
            startCaptureMenuItem.setIcon(R.drawable.ic_action_stop);
            startCaptureMenuItem.setTitle(getString(R.string.start));
        }
        highestRoute = fixHelper.getHighestRouteNo();
        if (!isMyServiceRunning(LocationService.class)) {
            highestRoute++;


            Intent i = new Intent(CaptureRouteActivity.this, ie.appz.shortestwalkingroute.gps.LocationService.class);
            i.putExtra("HIGHESTROUTE", highestRoute);
            startService(i);

            int displayRouteNumber = highestRoute + 1;
            Toast.makeText(CaptureRouteActivity.this, getString(R.string.toast_capturing_x, displayRouteNumber), Toast.LENGTH_SHORT).show();

        }
        getSupportFragmentManager().beginTransaction().replace(R.id.container, CaptureRouteFragment.getInstance(highestRoute)).commit();
        // Show progress spinner
        setSupportProgressBarIndeterminateVisibility(true);
    }

    private void stopCapture() {
        startCaptureMenuItem.setIcon(R.drawable.ic_action_play);
        startCaptureMenuItem.setTitle(getString(R.string.stop));
        stopService(new Intent(CaptureRouteActivity.this, LocationService.class));
        setSupportProgressBarIndeterminateVisibility(false);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.capture_route, menu);

        startCaptureMenuItem = menu.findItem(R.id.action_start);
        if (isMyServiceRunning(LocationService.class)) {
            startCaptureMenuItem.setTitle(R.string.stop);
            startCaptureMenuItem.setIcon(R.drawable.ic_action_stop);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_start: {
                if (!isMyServiceRunning(LocationService.class)) {
                    startCapture();
                } else {
                    stopCapture();
                }
                break;
            }
        }


        return super.onOptionsItemSelected(item);
    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


        if (fixHelper != null) {
            fixHelper.close();
        }
    }
}

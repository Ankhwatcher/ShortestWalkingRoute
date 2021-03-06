package ie.appz.shortestwalkingroute.gps;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Locale;

import ie.appz.shortestwalkingroute.BuildConfig;
import ie.appz.shortestwalkingroute.CaptureRouteActivity;
import ie.appz.shortestwalkingroute.Constants;
import ie.appz.shortestwalkingroute.DisplayRoutesActivity;
import ie.appz.shortestwalkingroute.R;
import ie.appz.shortestwalkingroute.sqlite.FixOpenHelper;
import ie.appz.shortestwalkingroute.sqlite.FixProvider;

/**
 * @author Rory
 */
public class LocationService extends Service implements TextToSpeech.OnInitListener {

    public static float oldAccuracy = 12;
    FixOpenHelper mFixOpenHelper;
    Location lastLocation;
    LocationManager mLocationManager;
    LocationListener networkListener;
    LocationListener gpsListener;
    private int mRouteNo;
    private float totalDistance = 0, spreadDistance = 0;
    private TextToSpeech mTextToSpeech;
    private boolean textToSpeech_Initialized = false;
    private NotificationManager mNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mTextToSpeech = new TextToSpeech(this, this);
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        networkListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                if (lastLocation == null || (location.getTime() - lastLocation.getTime()) > 10000) {
                    addRow(location);
                }
            }

            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {
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
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        if (intent != null) {
            mRouteNo = intent.getIntExtra("HIGHESTROUTE", 0);
            editor.putInt("HIGHESTROUTE", mRouteNo);
            editor.apply();
        } else {
            mRouteNo = settings.getInt("HIGHESTROUTE", 0);
        }
        /*
         * Register the listener with the Location Manager to receive location updates
		 */
        if (BuildConfig.DEBUG) {
            Log.d(LocationService.class.getName(), "Starting capture of location data for route " + mRouteNo);
        }

        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 5, networkListener);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, gpsListener);

        startForeground(2004, getNotification());
        /*
         * The two integers in this request are the time (ms) and distance (m)
		 * intervals of updates respectively.
		 */
        return START_STICKY;
    }

    private Notification getNotification() {
        int displayRouteNo = mRouteNo + 1;

        Intent notificationIntent = new Intent(this, CaptureRouteActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(this, MODE_PRIVATE, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder nCompatBuilder = new NotificationCompat.Builder(this);
        nCompatBuilder.setAutoCancel(false);
        nCompatBuilder.setOngoing(true);
        nCompatBuilder.setContentTitle(getString(R.string.app_name));
        nCompatBuilder.setContentText(getString(R.string.notification_content, displayRouteNo));
        nCompatBuilder.setContentIntent(contentIntent);
        nCompatBuilder.setSmallIcon(R.drawable.ic_menu_capture);

        return nCompatBuilder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected void addRow(Location location) {
        /*
         * This will reject any GPS fix with very poor accuracy
		 */
        if (location.getAccuracy() < 100 && location.getAccuracy() < 2 * oldAccuracy) {
            if (BuildConfig.DEBUG) {
                Log.d(LocationService.class.getName(), "Adding fix to " + FixOpenHelper.FIX_TABLE_NAME + " in route number " + mRouteNo + ". Fix provided by " + location.getProvider());
            }
            if (lastLocation == null) {
                lastLocation = location;
            }

            Uri uri = Uri.withAppendedPath(FixProvider.CONTENT_URI, FixProvider.ROUTE + "/" + mRouteNo);

            ContentValues contentValues = new ContentValues();
            contentValues.put(FixOpenHelper.ROUTE_NUMBER, mRouteNo);
            contentValues.put(FixOpenHelper.LATITUDE, location.getLatitude());
            contentValues.put(FixOpenHelper.LONGITUDE, location.getLongitude());
            contentValues.put(FixOpenHelper.ACCURACY, location.getAccuracy());
            contentValues.put(FixOpenHelper.SPEED, location.getSpeed());
            contentValues.put(FixOpenHelper.SOURCE, location.getProvider());
            contentValues.put(FixOpenHelper.TIME, location.getTime());

            ContentResolver contentResolver = this.getContentResolver();
            contentResolver.insert(uri, contentValues);


            int notifyRange = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).getInt(CaptureRouteActivity.NOTIFY_RANGE, 0);
            if (notifyRange != 0 && lastLocation != null) {
                float dt = location.distanceTo(lastLocation);
                if (dt > lastLocation.getAccuracy() * 3) {
                    totalDistance += dt;
                    spreadDistance += dt;

                    if (spreadDistance >= notifyRange) {
                        spreadDistance = 0;
                        String popupText = "Distance travelled " + formatDistanceString(totalDistance);
                        if (textToSpeech_Initialized) {
                            mTextToSpeech.speak(popupText, TextToSpeech.QUEUE_ADD, null);
                        }
                        NotificationCompat.Builder notificationCompatBuilder = new NotificationCompat.Builder(this)
                                .setAutoCancel(true)
                                .setOngoing(false)
                                .setContentTitle(this.getString(R.string.app_name))
                                .setContentText(popupText)
                                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, CaptureRouteActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_launcher))
                                .setTicker(popupText)
                                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
                        notificationCompatBuilder.build();
                        Notification notification = notificationCompatBuilder.build();
                        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.notify(302, notification);
                    }
                    lastLocation = location;
                }
            }

            oldAccuracy = (oldAccuracy + location.getAccuracy()) / 2;

        } else {
            if (BuildConfig.DEBUG) {
                Log.d(LocationService.class.getName(),
                        "Rejected fix for " + FixOpenHelper.FIX_TABLE_NAME
                                + " in route number " + mRouteNo
                                + " because accuracy is " + location.getAccuracy()
                                + ". Fix provided by " + location.getProvider()
                );
            }
            oldAccuracy++;
        }
    }


    String formatDistanceString(float totalDistance) {
        String distanceString;
        if (totalDistance > 1000) {
            distanceString = Math.floor((totalDistance / 1000) * 100) / 100 + " Kilometers";
        } else {
            distanceString = (int) Math.floor(totalDistance) + " meters";
        }
        return distanceString;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocationManager.removeUpdates(networkListener);
        mLocationManager.removeUpdates(gpsListener);


        SharedPreferences.Editor editor = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).edit();
        //A route with only one geopoint is not a route at all, reject it.
        Cursor c = mFixOpenHelper.routeFixesTime(mRouteNo);
        if (c.getCount() == 1) {
            Uri uri = Uri.withAppendedPath(FixProvider.CONTENT_URI, FixProvider.ROUTE + "/" + mRouteNo);
            ContentResolver contentResolver = this.getContentResolver();
            contentResolver.delete(uri, null, null);

            String selectedRoutes = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).getString(DisplayRoutesActivity.SELECTED_ROUTES, "");
            selectedRoutes = selectedRoutes.replace(String.valueOf((char) (mRouteNo)), "");
            editor.putString(DisplayRoutesActivity.SELECTED_ROUTES, selectedRoutes);
        } else {
            String selectedRoutes = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).getString(DisplayRoutesActivity.SELECTED_ROUTES, "");
            selectedRoutes = selectedRoutes + (char) (mRouteNo);
            editor.putString(DisplayRoutesActivity.SELECTED_ROUTES, selectedRoutes);
        }
        c.close();
        editor.apply();


        if (mNotificationManager != null) {
            mNotificationManager.cancelAll();
        }

        if (mFixOpenHelper != null) {
            mFixOpenHelper.close();
        }
        if (mTextToSpeech != null) {
            mTextToSpeech.stop();
            mTextToSpeech.shutdown();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result;
            if (mTextToSpeech.isLanguageAvailable(Locale.getDefault()) >= 0) {
                result = mTextToSpeech.setLanguage(Locale.getDefault());
            } else {
                Log.w(this.getClass().getName(),
                        "Default Language not available, falling back to US English.");
                result = mTextToSpeech.setLanguage(Locale.US);
            }
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(this.getClass().getName(),
                        "TextToSpeech: This Language is not supported");
            } else {
                textToSpeech_Initialized = true;
            }
        } else {
            Log.e(this.getClass().getName(),
                    "TextToSpeech: Initilization Failed!");
        }
    }

}

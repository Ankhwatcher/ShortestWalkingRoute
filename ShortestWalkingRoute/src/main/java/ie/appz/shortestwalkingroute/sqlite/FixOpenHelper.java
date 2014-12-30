package ie.appz.shortestwalkingroute.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

public class FixOpenHelper extends SQLiteOpenHelper {
    public static final String FIX_TABLE_NAME = "fix_table";
    public static final String COLUMN_ID = "_id";
    public static final String ROUTE_NUMBER = "route_number";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String ACCURACY = "accuracy";
    public static final String SPEED = "speed";
    public static final String SOURCE = "source";
    public static final String TIME = "time";
    /* CREATE_FIX_TABLE SQL statement */
    private static final String CREATE_FIX_TABLE = "create table " + FIX_TABLE_NAME + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + ROUTE_NUMBER + " integer, "
            + LATITUDE + " real,"
            + LONGITUDE + " real,"
            + ACCURACY + " real,"
            + SPEED + " real,"
            + TIME + " integer, "
            + SOURCE + " text not null" + ");";
    public static final String ROUTE_LENGTH = "route_length";
    public static final String ROUTE_TIME = "route_time";
    public static final String ROUTE_TABLE_NAME = "route_table";
    public static final String TARGET = "target";
    /* CREATE_ROUTE_TABLE SQL statement */
    private static final String CREATE_ROUTE_TABLE = "create table " + ROUTE_TABLE_NAME + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + ROUTE_NUMBER + " integer, "
            + TARGET + " integer,"
            + ROUTE_LENGTH + " real,"
            + ROUTE_TIME + " integer" + ");";
    public static final String TARGET_TABLE_NAME = "target_table";

    private static final String DATABASE_NAME = "fixtable.db";
    private static final int DATABASE_VERSION = 3;

    public FixOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_FIX_TABLE);
        db.execSQL(CREATE_ROUTE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(FixOpenHelper.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + FIX_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ROUTE_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TARGET_TABLE_NAME);
        onCreate(db);
    }

    /**
     * ****** fix_table Functions ********
     */

    public void addFix(int routeNo, Location location) {

        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("insert into " + FIX_TABLE_NAME + "(" + ROUTE_NUMBER + ", "
                + LATITUDE + ", " + LONGITUDE + ", " + ACCURACY + ", " + SPEED
                + ", " + SOURCE + ", " + TIME + ") " + "values(" + routeNo
                + ", " + location.getLatitude() + ", "
                + location.getLongitude() + ", " + location.getAccuracy()
                + ", " + location.getSpeed() + "," + " '"
                + location.getProvider() + "', " + location.getTime() + ");");

    }

    public int getHighestRouteNo() {
        SQLiteDatabase db = getReadableDatabase();
        int highRoute = 0;
        highRoute = (int) DatabaseUtils.longForQuery(db, "SELECT MAX("
                + ROUTE_NUMBER + ") FROM " + FIX_TABLE_NAME, null);
        db.close();
        return highRoute;

    }

    public long totalRouteTime(int routeNo) {
        long diffTime = 0;
        SQLiteDatabase db = getReadableDatabase();
        if (db != null) {
            diffTime = DatabaseUtils.longForQuery(db, "SELECT (MAX(" + TIME
                    + ") - MIN(" + TIME + ")) FROM " + FIX_TABLE_NAME + " WHERE "
                    + ROUTE_NUMBER + " = " + routeNo, null);

            db.close();
        }
        return diffTime;
    }

    public float averageSpeed(int routeNo) {

        SQLiteDatabase db = getReadableDatabase();
        float avgSpeed = 0;
        String astring = DatabaseUtils.stringForQuery(db, "SELECT AVG(" + SPEED
                + ") FROM " + FIX_TABLE_NAME + " WHERE " + ROUTE_NUMBER + " = "
                + routeNo, null);
        if (astring != null)
            avgSpeed = Float.valueOf(astring);
        return avgSpeed;
    }

    public float minimumAccuracy(int routeNo) {
        float minAcc = 0;
        SQLiteDatabase db = getReadableDatabase();
        try {
            Cursor results = db.rawQuery("SELECT MIN(" + ACCURACY + ") FROM "
                    + FIX_TABLE_NAME, null);
            if (results.moveToFirst()) {
                minAcc = results.getFloat(0);
            }
            results.close();
        } catch (Exception e) {
            Log.e(FixOpenHelper.class.getName(),
                    "Unable to get minimum Accuracy for Route " + routeNo + ".",
                    e);
        }
        return minAcc;
    }

    public Cursor routeFixesTime(int routeNo) {
        SQLiteDatabase db = getReadableDatabase();

        return db.query(FIX_TABLE_NAME, new String[]{LATITUDE, LONGITUDE, ACCURACY, TIME}, ROUTE_NUMBER + " = " + routeNo, null, null, null, COLUMN_ID);

    }

    public Cursor routeFixesSpeed(int routeNo) {
        SQLiteDatabase db = getReadableDatabase();

        return db.query(FIX_TABLE_NAME, new String[]{LATITUDE, LONGITUDE, ACCURACY, SPEED, COLUMN_ID}, ROUTE_NUMBER + " = " + routeNo,
                null, null, null, COLUMN_ID);
    }

    public void rejectRoute(int routeNo) {
        SQLiteDatabase db = getWritableDatabase();
        Log.i(FixOpenHelper.class.getName(), "Deleting route number " + routeNo);
        db.delete(FIX_TABLE_NAME, ROUTE_NUMBER + "=" + routeNo, null);

        db.close();
    }

    /********* route_table Functions *********/


}
package ie.appz.shortestwalkingroute.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

public class FixOpenHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "fixtable.db";
	private static final int DATABASE_VERSION = 3;
	public static final String TABLE_NAME = "fix_table";
	public static final String COLUMN_ID = "_id";
	public static final String ROUTE_NUMBER = "route_number";
	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";
	public static final String ACCURACY = "accuracy";
	public static final String SPEED = "speed";
	public static final String SOURCE = "source";
	public static final String TIME = "time";
	public static final String TARGET = "target";
	public static final String URL = "url";

	// Database creation SQL statement
	private static final String DATABASE_CREATE = "create table " + TABLE_NAME + "(" + COLUMN_ID
			+ " integer primary key autoincrement, " + ROUTE_NUMBER + " integer, " + TARGET + " integer," + LATITUDE
			+ " real," + LONGITUDE + " real," + ACCURACY + " real," + SPEED + " real," + TIME + " integer, " + SOURCE
			+ " text not null" + ");";

	public FixOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(FixOpenHelper.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(db);
	}

	public void addFix(int routeNo, Location location) {

		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("insert into " + TABLE_NAME + "(" + ROUTE_NUMBER + ", " + LATITUDE + ", " + LONGITUDE + ", "
				+ ACCURACY + ", " + SPEED + ", " + SOURCE + ", " + TIME + ") " + "values(" + routeNo + ", "
				+ location.getLatitude() + ", " + location.getLongitude() + ", " + location.getAccuracy() + ", "
				+ location.getSpeed() + "," + " '" + location.getProvider() + "', " + location.getTime() + ");");

	}

	public int highestRoute() {
		SQLiteDatabase db = getReadableDatabase();
		int highRoute = 0;
		try {

			Cursor results = db.rawQuery("SELECT MAX(" + ROUTE_NUMBER + ") FROM " + TABLE_NAME, null);
			if (results.moveToFirst()) {
				highRoute = results.getInt(0);
			}
			results.close();
		} catch (Exception e) {
			Log.e(FixOpenHelper.class.getName(), "Unable to get highestRoute.", e);
		}

		return highRoute;

	}

	public long totalRouteTime(int routeNo) {
		long diffTime = 0;
		SQLiteDatabase db = getReadableDatabase();
		try {
			Cursor results = db.rawQuery("SELECT MAX(" + TIME + ") AS \"MAXTIME\", MIN(" + TIME
					+ ") AS \"MINTIME\" FROM " + TABLE_NAME + " WHERE " + ROUTE_NUMBER + " = " + routeNo, null);
			if (results.moveToFirst()) {
				diffTime = results.getLong(0) - results.getLong(1);
			}
			results.close();
		} catch (Exception e) {
			Log.e(FixOpenHelper.class.getName(), "Unable to get diffTime for Route " + routeNo + ".", e);
		}
		return diffTime;
	}

	public float minimumAccuracy(int routeNo) {
		float minAcc = 0;
		SQLiteDatabase db = getReadableDatabase();
		try {
			Cursor results = db.rawQuery("SELECT MIN(" + ACCURACY + ") FROM " + TABLE_NAME, null);
			if (results.moveToFirst()) {
				minAcc = results.getFloat(0);
			}
			results.close();
		} catch (Exception e) {
			Log.e(FixOpenHelper.class.getName(), "Unable to get minimum Accuracy for Route " + routeNo + ".", e);
		}
		return minAcc;
	}

	public Cursor routeFixesTime(int routeNo) {
		SQLiteDatabase db = getReadableDatabase();

		return db.query(TABLE_NAME, new String[] { LATITUDE, LONGITUDE, ACCURACY, TIME }, ROUTE_NUMBER + " = "
				+ routeNo, null, null, null, COLUMN_ID);

	}
	
	public Cursor routeFixesSpeed(int routeNo) {
		SQLiteDatabase db = getReadableDatabase();

		return db.query(TABLE_NAME, new String[] { LATITUDE, LONGITUDE, ACCURACY, SPEED, COLUMN_ID }, ROUTE_NUMBER + " = "
				+ routeNo, null, null, null, COLUMN_ID);

	}

	public void rejectRoute(int routeNo) {
		SQLiteDatabase db = getReadableDatabase();
		db.delete(TABLE_NAME, ROUTE_NUMBER + "=" + routeNo, null);
	}
	
}
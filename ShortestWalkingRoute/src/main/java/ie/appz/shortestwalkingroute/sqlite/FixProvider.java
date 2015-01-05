package ie.appz.shortestwalkingroute.sqlite;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 * @author Rory
 */
public class FixProvider extends ContentProvider {
    public static final String PROVIDER_NAME = "ie.appz.shortestwalkingroute.sqlite.FixProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME);
    public static final String ROUTE = "ROUTE";
    private static final int ALL = 1;
    private static final int ONEROUTE = 2;
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, ROUTE, ALL);
        uriMatcher.addURI(PROVIDER_NAME, ROUTE + "/#", ONEROUTE);
    }

    private SQLiteDatabase fixDB;
    private FixOpenHelper fixOpenHelper;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        fixDB = fixOpenHelper.getWritableDatabase();
        int rowsAffected;
        switch (uriType) {
            case ALL:
                rowsAffected = fixDB.delete(FixOpenHelper.FIX_TABLE_NAME, selection, selectionArgs);
                break;
            case ONEROUTE:
                String routeNo = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsAffected = fixDB.delete(FixOpenHelper.FIX_TABLE_NAME, FixOpenHelper.ROUTE_NUMBER + "=" + routeNo, null);
                } else {
                    rowsAffected = fixDB.delete(FixOpenHelper.FIX_TABLE_NAME, selection + " and " + FixOpenHelper.ROUTE_NUMBER
                            + "=" + routeNo, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown or Invalid URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsAffected;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            // ---get all routes---
            case ALL:
                return "vnd.android.cursor.dir/vnd.appz.shortestwalkingroute.route ";
            // ---get a particular routes---
            case ONEROUTE:
                return "vnd.android.cursor.item/vnd.appz.shortestwalkingroute.route ";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // ---add a new fix---
        long rowID = fixDB.insert(FixOpenHelper.FIX_TABLE_NAME, "", values);

        // ---if added successfully---
        if (rowID > 0) {
            getContext().getContentResolver().notifyChange(uri, null);

            return ContentUris.withAppendedId(CONTENT_URI, rowID);
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public boolean onCreate() {

        fixOpenHelper = new FixOpenHelper(getContext());
        fixDB = fixOpenHelper.getWritableDatabase();
        return fixDB != null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
        sqlBuilder.setTables(FixOpenHelper.FIX_TABLE_NAME);
        if (uriMatcher.match(uri) == ONEROUTE) {
            sqlBuilder.appendWhere(FixOpenHelper.ROUTE_NUMBER + " == " + uri.getPathSegments().get(1));
        }
        Cursor c = sqlBuilder.query(fixDB, projection, selection, selectionArgs, null, null, null);
        // ---register to watch a content URI for changes---
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        return 0;
    }
}
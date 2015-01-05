package ie.appz.shortestwalkingroute;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.ListView;

import ie.appz.shortestwalkingroute.sqlite.FixOpenHelper;
import ie.appz.shortestwalkingroute.sqlite.FixProvider;

/**
 * @author Rory
 */
public class CaptureRouteFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String REQUESTED_ROUTE = "REQUESTED_ROUTE";
    private static final int FIX_LIST_LOADER = 0x01;
    private FixOpenHelper fixOpenHelper;
    private SimpleCursorAdapter adapter;

    public CaptureRouteFragment() {
    }

    public static CaptureRouteFragment getInstance(int requestedRoute) {
        CaptureRouteFragment captureRouteFragment = new CaptureRouteFragment();

        Bundle arguments = new Bundle();
        arguments.putInt(REQUESTED_ROUTE, requestedRoute);
        captureRouteFragment.setArguments(arguments);

        return captureRouteFragment;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fixOpenHelper = new FixOpenHelper(getActivity().getApplicationContext());

        String[] uiBindFrom = {FixOpenHelper.LATITUDE, FixOpenHelper.LONGITUDE, FixOpenHelper.ACCURACY, FixOpenHelper.SPEED};
        int[] uiBindTo = {R.id.latitudeText, R.id.longitudeText, R.id.accuracyText, R.id.speedText};

        adapter = new SimpleCursorAdapter(getActivity(), R.layout.row_capture_route, null, uiBindFrom, uiBindTo, 0);

        setListAdapter(adapter);

        getLoaderManager().initLoader(FIX_LIST_LOADER, getArguments(), this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setScrollbarFadingEnabled(false);
        getListView().setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        int routeNo = args.getInt(REQUESTED_ROUTE, 1);

        Uri uri = Uri.withAppendedPath(FixProvider.CONTENT_URI, FixProvider.ROUTE + "/" + routeNo);

        String[] projection = {FixOpenHelper.COLUMN_ID, FixOpenHelper.LATITUDE, FixOpenHelper.LONGITUDE, FixOpenHelper.ACCURACY, FixOpenHelper.SPEED};

        return new CursorLoader(getActivity(), uri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        adapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fixOpenHelper != null) {
            fixOpenHelper.close();
        }
    }
}
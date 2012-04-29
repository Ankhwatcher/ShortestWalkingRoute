package ie.appz.shortestwalkingroute;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class RouteItemizedOverlay extends ItemizedOverlay<OverlayItem> {
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	Context clientContext;

	public RouteItemizedOverlay(Drawable routeIcon) {
		super(boundCenter(routeIcon));
		populate();
	}

	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

	public RouteItemizedOverlay(Drawable routeIcon, Context context) {
		super(boundCenter(routeIcon));
		this.clientContext = context;
		populate();
	}

	@Override
	protected boolean onTap(int index) {
		OverlayItem item = mOverlays.get(index);

		AlertDialog.Builder dialog = new AlertDialog.Builder(this.clientContext);
		dialog.setTitle(item.getTitle());
		dialog.setMessage(item.getSnippet());
		dialog.show();

		return true;
	}

	public void draw(android.graphics.Canvas canvas, MapView mapView, boolean shadow) {

		super.draw(canvas, mapView, false);

	}

}

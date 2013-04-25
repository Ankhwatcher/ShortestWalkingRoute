package ie.appz.shortestwalkingroute;

import java.util.ArrayList;
import java.util.Iterator;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class RouteOverlay extends Overlay {

	private int routeColor;
	private ArrayList<GeoPoint> routePoints;

	public RouteOverlay(ArrayList<GeoPoint> routePoints, int routeColor) {
		this.routePoints = routePoints;
		this.routeColor = routeColor;
	}

	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		// Projection projection = mapView.getProjection();
		super.draw(canvas, mapView, shadow);
		if (shadow == false) {

			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setDither(true);
			paint.setStyle(Paint.Style.FILL_AND_STROKE);
			paint.setStrokeJoin(Paint.Join.ROUND);
			paint.setColor(routeColor);
			paint.setStrokeWidth(6);

			Point mapPoint = new Point();
			Path path = new Path();

			// Path must be set to start location.
			GeoPoint point = routePoints.get(0);
			mapView.getProjection().toPixels(point, mapPoint);
			path.moveTo(mapPoint.x, mapPoint.y);

			Point pixelPoint = new Point();
			Iterator<GeoPoint> routePointIterator = routePoints.iterator();
			while (routePointIterator.hasNext()) {

				GeoPoint routePoint = routePointIterator.next();
				mapView.getProjection().toPixels(routePoint, pixelPoint);
				path.lineTo(pixelPoint.x, pixelPoint.y);
				path.moveTo(pixelPoint.x, pixelPoint.y);
			}

			// draw track
			canvas.drawPath(path, paint);
		}

	}

}

package ie.appz.shortestwalkingroute;

import android.os.Bundle;

import com.google.android.maps.MapActivity;

public class DisplayRoutesActivity extends MapActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.displayroutes);
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
}

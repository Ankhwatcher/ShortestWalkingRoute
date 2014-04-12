package ie.appz.shortestwalkingroute;

import ie.appz.shortestwalkingroute.sqlite.FixOpenHelper;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TableRow;

/**
 * @author  Rory
 */
public class HomeActivity extends Activity {
	/**
	 */
	private FixOpenHelper fixHelper = new FixOpenHelper(this);
	Context HAContext = this;
	private OnClickListener fragmentLaunch0 = new OnClickListener() {

		public void onClick(View v) {
			Intent intent = new Intent(HAContext, CaptureRouteActivity.class);
			startActivity(intent);
		}
	};
	private OnClickListener fragmentLaunch1 = new OnClickListener() {

		public void onClick(View v) {
			Intent intent = new Intent(HAContext, DisplayRoutesActivity.class);
			startActivity(intent);
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

	}

	@Override
	protected void onResume() {

		setContentView(R.layout.home);
		super.onResume();

		/* SetOnClickListener For Capture Route */
		TableRow fragmentRow0 = (TableRow) findViewById(R.id.fragmentRow0);
		fragmentRow0.setOnClickListener(fragmentLaunch0);

		/*
		 * SetOnClickListener and visibility for Display Routes only if at least
		 * one route is recorded
		 */

		if (fixHelper.highestRoute() >= 1) {
			TableRow fragmentRow1 = (TableRow) findViewById(R.id.fragmentRow1);
			fragmentRow1.setVisibility(0);
			fragmentRow1.setOnClickListener(fragmentLaunch1);
		}

		SharedPreferences settings = getSharedPreferences(CaptureRouteActivity.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("capturingRoute", false);
		editor.commit();
	}

	@Override
	public void onStop() {
		super.onStop();
		fixHelper.close();
	}
}

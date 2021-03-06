package ie.appz.shortestwalkingroute;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TableRow;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import ie.appz.shortestwalkingroute.sqlite.FixOpenHelper;

/**
 * @author Rory
 */
public class HomeActivity extends ActionBarActivity {
    /**
     */

    Context HAContext = this;
    private OnClickListener fragmentLaunch0 = new OnClickListener() {

        public void onClick(View v) {
            Intent intent = new Intent(HAContext, CaptureRouteActivity.class);
            startActivity(intent);
        }
    };
    private OnClickListener fragmentLaunch1 = new OnClickListener() {

        public void onClick(View v) {
            switch (GooglePlayServicesUtil.isGooglePlayServicesAvailable(HomeActivity.this)) {
                case ConnectionResult.SUCCESS:
                    Intent intent = new Intent(HAContext, DisplayRoutesActivity.class);
                    startActivity(intent);
                    break;

                case ConnectionResult.SERVICE_MISSING:
                case ConnectionResult.SERVICE_INVALID:
                    Toast.makeText(HomeActivity.this, getString(R.string.error_services_install), Toast.LENGTH_LONG).show();
                    break;

                case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                    Toast.makeText(HomeActivity.this, getString(R.string.error_services_update), Toast.LENGTH_LONG).show();
                    break;

                case ConnectionResult.SERVICE_DISABLED:
                    Toast.makeText(HomeActivity.this, getString(R.string.error_services_enable), Toast.LENGTH_LONG).show();
                    break;

                default:
                    Toast.makeText(HomeActivity.this, getString(R.string.error_services_general), Toast.LENGTH_LONG).show();
            }


        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    protected void onResume() {

        setContentView(R.layout.activity_home);
        super.onResume();

		/* SetOnClickListener For Capture Route */
        TableRow fragmentRow0 = (TableRow) findViewById(R.id.fragmentRow0);
        fragmentRow0.setOnClickListener(fragmentLaunch0);

		/*
         * SetOnClickListener and visibility for Display Routes only if at least
		 * one route is recorded
		 */
        FixOpenHelper fixHelper = new FixOpenHelper(this);
        if (fixHelper.getHighestRouteNo() != -1) {
            TableRow fragmentRow1 = (TableRow) findViewById(R.id.fragmentRow1);
            fragmentRow1.setVisibility(View.VISIBLE);
            fragmentRow1.setOnClickListener(fragmentLaunch1);
        }
        fixHelper.close();


    }


}

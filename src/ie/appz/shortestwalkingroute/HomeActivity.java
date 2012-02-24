package ie.appz.shortestwalkingroute;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TableRow;



public class HomeActivity extends Activity
{
	Context HAContext = this;
	private OnClickListener fragmentLaunch0 = new OnClickListener()
	{
	    //@Override
		public void onClick(View v)
	    {
	    	Intent intent = new Intent(HAContext,CaptureRouteActivity.class);
	    	startActivity(intent);
		}
	};
	private OnClickListener fragmentLaunch1 = new OnClickListener()
	{
	    //@Override
		public void onClick(View v)
	    {
	    	Intent intent = new Intent(HAContext,DisplayRoutesActivity.class);
	    	startActivity(intent);
		}
	};
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        
        /*SetOnClickListener For Capture Route*/
        TableRow fragmentRow0 =  (TableRow) findViewById(R.id.fragmentRow0);
		fragmentRow0.setOnClickListener(fragmentLaunch0);
		
		/*SetOnClickListener and visibility for Display Routes only if at least one route is recorded*/
		FixOpenHelper fixHelper = new FixOpenHelper(this);
		if (fixHelper.highestRoute() >= 1)
		{
			TableRow fragmentRow1 =  (TableRow) findViewById(R.id.fragmentRow1);
			fragmentRow1.setVisibility(0);
			fragmentRow1.setOnClickListener(fragmentLaunch1);
		}
    }
}


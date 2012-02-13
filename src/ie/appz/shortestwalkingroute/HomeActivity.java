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
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        TableRow fragmentRow0 =  (TableRow) findViewById(R.id.fragmentRow0);
        
		fragmentRow0.setOnClickListener(fragmentLaunch0);
    }
}


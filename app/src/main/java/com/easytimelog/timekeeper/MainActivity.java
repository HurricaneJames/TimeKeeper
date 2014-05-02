package com.easytimelog.timekeeper;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

import com.easytimelog.timekeeper.devtools.DatabaseUtils;
import com.easytimelog.timekeeper.views.TimeRecordFragment;


public class MainActivity extends Activity implements TimeRecordFragment.OnTimeRecordSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO - remove this dev/debug with an actual UI method of adding records to the databse
        DatabaseUtils.tempSeedDatabase(getApplicationContext(), 10);

        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new TimeRecordFragment())
                    .commit();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // TODO - remove this dev/debug with an actual UI method of removing records from the db
        DatabaseUtils.wipeDatabase(getApplicationContext());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTimeRecordSelected(String id) {
        Log.d("MainActivity", "onTimeRecordSelected [" + id + "]");
    }

}

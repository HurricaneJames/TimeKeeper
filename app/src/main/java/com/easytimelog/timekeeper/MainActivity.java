package com.easytimelog.timekeeper;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

import com.easytimelog.timekeeper.data.TimeKeeperContract;
import com.easytimelog.timekeeper.devtools.DatabaseUtils;
import com.easytimelog.timekeeper.views.ProjectDetailActivity;
import com.easytimelog.timekeeper.views.ProjectDetailsFragment;
import com.easytimelog.timekeeper.views.ProjectsFragment;
import com.easytimelog.timekeeper.views.TimeRecordFragment;


public class MainActivity extends Activity implements ProjectDetailsFragment.OnTimeRecordSelectedListener,
                                                      ProjectsFragment.OnProjectSelectedListener {

    private boolean mDualPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO - remove this dev/debug with an actual UI method of removing records from the db
        DatabaseUtils.wipeDatabase(getApplicationContext());

        // TODO - remove this dev/debug with an actual UI method of adding records to the databse
        DatabaseUtils.tempSeedDatabase(getApplicationContext(), 5, 25);

//        Cursor cursor = getContentResolver().query(ContentUris.withAppendedId(TimeKeeperContract.Projects.CONTENT_URI, 1), TimeKeeperContract.Projects.PROJECTION_ALL, null, null, null);
//        if(cursor.moveToFirst()) {
//            String startedAt;
//            long duration;
//            boolean running;
//            {
//                duration = cursor.getLong(cursor.getColumnIndex(TimeKeeperContract.Projects.DURATION));
//                startedAt = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.Projects.STARTED_AT));
//                running = cursor.getInt(cursor.getColumnIndex(TimeKeeperContract.Projects.RUNNING)) != 0;
//
//                Log.d("##### ##### ##### DURATION", Long.toString(duration));
//                Log.d("##### ##### ##### startedAt", "-" + startedAt);
//                Log.d("##### ##### ##### running", Boolean.toString(running));
//            }while(cursor.moveToNext());
//        }

        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {

            getFragmentManager().beginTransaction()
                    .add(R.id.container, new ProjectsFragment())
                    .commit();
        }

        mDualPane = findViewById(R.id.time_records_container) != null;
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

    @Override
    public void onProjectSelected(int id) {
        Log.d("MainActivity", "onProjectSelected [" + id + "]");
//        Log.d("MainActivity", "Dual Pane Display: " + mDualPane);
        if(mDualPane) {
            ProjectDetailsFragment recordsFragment = (ProjectDetailsFragment ) getFragmentManager().findFragmentById(R.id.time_records_container);
//Log.d("MainActivity", "Project Details Fragment Shown Id: " + recordsFragment.getShownProjectId());
//Log.d("MainActivity", "Update to: " + id);
            if(recordsFragment == null || recordsFragment.getShownProjectId() != id) {
                recordsFragment = ProjectDetailsFragment.newInstance(id);
            }

            getFragmentManager().beginTransaction()
                    .replace(R.id.time_records_container, recordsFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        }else {
            Intent intent = new Intent();
            intent.setClass(this, ProjectDetailActivity.class);
            intent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, id);
            startActivity(intent);
        }
    }
}

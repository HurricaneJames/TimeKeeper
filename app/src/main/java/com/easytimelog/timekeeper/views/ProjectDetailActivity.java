package com.easytimelog.timekeeper.views;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.easytimelog.timekeeper.R;

public class ProjectDetailActivity extends Activity implements ProjectDetailsFragment.OnTimeRecordSelectedListener {
    public static final String EXTRA_PROJECT_ID = "projectId";
    public static final String EXTRA_DUAL_PANE = "dualView";
    private boolean mDualPane = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(mDualPane && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            finish();
            return;
        }

        setContentView(R.layout.activity_project_detail);
        if(savedInstanceState == null) {
            String projectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);
            mDualPane = getIntent().getBooleanExtra(EXTRA_DUAL_PANE, false);
            getFragmentManager().beginTransaction()
                    .add(R.id.container, ProjectDetailsFragment.newInstance(projectId))
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.project_detail, menu);
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
        Log.d("ProjectDetailActivity", "TODO - Implement something for onTimeRecordsSelected");
    }

}

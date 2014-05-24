package com.easytimelog.timekeeper.views;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.easytimelog.timekeeper.R;

public class ProjectDetailActivity extends Activity implements ProjectDetailsFragment.OnTimeRecordSelectedListener {
    public static final String EXTRA_PROJECT_ID = "projectId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);
        if(savedInstanceState == null) {
            int projectId = getIntent().getIntExtra(EXTRA_PROJECT_ID, 0);
            getFragmentManager().beginTransaction()
                    .add(R.id.container, ProjectDetailsFragment.newInstance(projectId))
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.project_detail, menu);
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

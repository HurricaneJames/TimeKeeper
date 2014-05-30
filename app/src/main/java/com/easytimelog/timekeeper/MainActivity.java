package com.easytimelog.timekeeper;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.easytimelog.timekeeper.data.TimeKeeperContract;
import com.easytimelog.timekeeper.devtools.DatabaseUtils;
import com.easytimelog.timekeeper.util.DatabaseHelper;
import com.easytimelog.timekeeper.views.OnNoteChangeListener;
import com.easytimelog.timekeeper.views.OnNoteRequestedListener;
import com.easytimelog.timekeeper.views.ProjectDetailActivity;
import com.easytimelog.timekeeper.views.ProjectDetailsFragment;
import com.easytimelog.timekeeper.views.ProjectsFragment;
import com.easytimelog.timekeeper.views.TextNoteFragment;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity implements ProjectDetailsFragment.OnTimeRecordSelectedListener,
                                                      ProjectsFragment.OnProjectSelectedListener,
                                                      OnNoteChangeListener,
                                                      OnNoteRequestedListener {

    private boolean mDualPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {

            getFragmentManager().beginTransaction()
                    .add(R.id.container, new ProjectsFragment())
                    .commit();
        }

        mDualPane = findViewById(R.id.details_container) != null;
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

        if(id == R.id.action_seedDB) {
            // TODO - remove this dev/debug with an actual UI method of adding records to the databse
            DatabaseUtils.tempSeedDatabase(getApplicationContext(), 5, 25, 3);
        }

        if(id == R.id.action_wipeDB) {
            // TODO - remove this dev/debug with an actual UI method of removing records from the db
            DatabaseUtils.wipeDatabase(getApplicationContext());
            System.exit(0);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTimeRecordSelected(String id) {
        Log.d("MainActivity", "onTimeRecordSelected [" + id + "]");
    }

    @Override
    public void onProjectSelected(String id) {
        Log.d("MainActivity", "onProjectSelected [" + id + "]");
        if(mDualPane) {
            Fragment detailsContainerFragment = getFragmentManager().findFragmentById(R.id.details_container);
            if(detailsContainerFragment == null || (detailsContainerFragment instanceof ProjectDetailsFragment && ((ProjectDetailsFragment)detailsContainerFragment).getShownProjectId() != id)) {
                detailsContainerFragment = ProjectDetailsFragment.newInstance(id);
            }
            getFragmentManager().beginTransaction()
                    .replace(R.id.details_container, detailsContainerFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        }else {
            Intent intent = new Intent();
            intent.setClass(this, ProjectDetailActivity.class);
            intent.putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, id);
            startActivity(intent);
        }
    }

    @Override
    public void onNewNoteRequested(String projectId, String noteType) {
        Log.d("MainActivity::NewNoteRequested", "Project: " + projectId + "        Type: " + noteType);
        // todo - save the current view before opening the new pane so that the history (back button) will reopen as expected (instead of closing the app)
        Bundle values = new Bundle();
        values.putString(OpenNoteViewTask.ARG_PROJECT_ID, projectId);
        values.putString(OpenNoteViewTask.ARG_NOTE_TYPE, noteType);
        new OpenNoteViewTask().execute(values);
    }


    @Override
    public void onNoteUpdateRequested(String noteId) { /* todo - implement in version 2 */ }

    @Override
    public void onNoteChanged(int result) {
        Log.d("MainActivity", "OnNoteChanged: " + result);
        // remove the fragment
        Fragment noteFragment = getFragmentManager().findFragmentById(R.id.details_container);
        getFragmentManager().beginTransaction()
                .remove(noteFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();

        // todo - if a time record was created specifically for this note, stop the clock
    }

    private class OpenNoteViewTask extends AsyncTask<Bundle, Void, Bundle> {
        public static final String ARG_PROJECT_ID = "projectId";
        public static final String ARG_NOTE_TYPE = "noteType";
        private static final String ARG_TIME_RECORD_ID = "timeRecordId";

        @Override
        protected Bundle doInBackground(Bundle... params) {
            Bundle values = params[0];
            values.putString(ARG_TIME_RECORD_ID, getRunningTimeRecordFor(values.getString(ARG_PROJECT_ID)));
            return values;
        }

        @Override
        protected void onPostExecute(Bundle values) {
            super.onPostExecute(values);

            if(mDualPane) {
                TextNoteFragment noteFragment = TextNoteFragment.newInstance(values.getString(ARG_TIME_RECORD_ID), null);
                getFragmentManager().beginTransaction()
                        .replace(R.id.details_container, noteFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
            } else {
                // todo - open new activity view for note
            }

        }

        private String getRunningTimeRecordFor(String projectId) {
            // todo - check to see if one is already running
            Cursor timeRecordIdsCursor = getContentResolver().query(TimeKeeperContract.TimeRecords.CONTENT_URI,
                    new String[] { TimeKeeperContract.TimeRecords._ID },
                    TimeKeeperContract.TimeRecords.whereProjectId(projectId) +
                    " and " + TimeKeeperContract.TimeRecords.END_AT + " is NULL",
                    null, null);
            Set<String> timeRecordIds = DatabaseHelper.getIdsFromCursor(timeRecordIdsCursor, timeRecordIdsCursor.getColumnIndex(TimeKeeperContract.TimeRecords._ID));
            if(timeRecordIds.size() > 0) {
                return (String)timeRecordIds.toArray()[0];
            }else {
                return DatabaseHelper.addTimeRecord(getApplicationContext(), new Date(), null, projectId);
            }
        }
    }
}

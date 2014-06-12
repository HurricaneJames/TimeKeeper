package com.easytimelog.timekeeper;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.easytimelog.timekeeper.data.TimeKeeperContract;
import com.easytimelog.timekeeper.devtools.DatabaseUtils;
import com.easytimelog.timekeeper.util.DatabaseHelper;
import com.easytimelog.timekeeper.util.FileHelper;
import com.easytimelog.timekeeper.views.AudioCaptureFragment;
import com.easytimelog.timekeeper.views.NoteTakerActivity;
import com.easytimelog.timekeeper.views.OnNoteChangeListener;
import com.easytimelog.timekeeper.views.OnNoteRequestedListener;
import com.easytimelog.timekeeper.views.ProjectDetailActivity;
import com.easytimelog.timekeeper.views.ProjectDetailsFragment;
import com.easytimelog.timekeeper.views.ProjectsFragment;
import com.easytimelog.timekeeper.views.TextNoteFragment;

import java.util.Date;
import java.util.Set;

public class MainActivity extends Activity implements ProjectDetailsFragment.OnTimeRecordSelectedListener,
                                                      ProjectsFragment.OnProjectSelectedListener,
                                                      OnNoteChangeListener,
                                                      OnNoteRequestedListener {

    public static final int ADD_TEXT_NOTE = 0;
    public static final int ADD_LIST_NOTE = 1;
    public static final int ADD_CAMERA_NOTE = 2;
    public static final int ADD_AUDIO_NOTE = 3;

    private ContentValues mExternalNoteActivityParams;
    private static final String EXTERNAL_PARAMS = "external_params";
    private static final String EXTERNAL_PROJECT_ID = "project_id";
    private static final String EXTERNAL_TIME_RECORD_ID = "time_record_id";
    private static final String EXTERNAL_NOTE_ID = "note_id";
    private static final String EXTERNAL_LINK = "link";
    private static final String EXTERNAL_SCRIBBLE = "scribble";
    private static final String EXTERNAL_NOTE_TYPE = "note_type";

    private static final String SELECTED_PROJECT = "selected_project";
    private static final String NO_PROJECT_SELECTED = "";
    private String mSelectedProject = NO_PROJECT_SELECTED;

    private boolean mDualPane;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
Log.d("MainActivity", "onCreate Called ***** ***** *****");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDualPane = findViewById(R.id.details_container) != null;
        ProjectsFragment projectListFragment = ProjectsFragment.newInstance(mDualPane);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, projectListFragment)
                    .commit();
        }else {
            mExternalNoteActivityParams = savedInstanceState.getParcelable(EXTERNAL_PARAMS);
            String selectedProject = savedInstanceState.getString(SELECTED_PROJECT);
            if(selectedProject != null) { onProjectSelected(selectedProject); }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if(mExternalNoteActivityParams != null) { outState.putParcelable(EXTERNAL_PARAMS, mExternalNoteActivityParams); }
        if(!NO_PROJECT_SELECTED.equals(mSelectedProject)) { outState.putString(SELECTED_PROJECT, mSelectedProject); }
        super.onSaveInstanceState(outState);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("MainActivity", "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        ContentValues externalParams = popExternalNoteParams();
        ContentValues noteValues;
        // todo - add toast letting user know that item was (not?) added
        if(resultCode == RESULT_OK) {
            switch(requestCode) {
                case ADD_TEXT_NOTE:
                case ADD_AUDIO_NOTE:
                    // NoteTakerActivity does all the work, just skip
                    // todo - update TextNoteFragment to return the content values instead of adding to the db there? (maybe not?)
                    // noteValues = (ContentValues)data.getParcelableExtra(NoteTakerActivity.NOTE_VALUES);
                    break;
                case ADD_CAMERA_NOTE:
                    noteValues = new ContentValues();
                    noteValues.put(TimeKeeperContract.Notes.TIME_RECORD_ID, externalParams.getAsString(EXTERNAL_TIME_RECORD_ID));
                    noteValues.put(TimeKeeperContract.Notes.LINK, externalParams.getAsString(EXTERNAL_LINK));
                    noteValues.put(TimeKeeperContract.Notes.NOTE_TYPE, externalParams.getAsString(EXTERNAL_NOTE_TYPE));
                    DatabaseHelper.addNote(this, noteValues);
            }
        }
    }

    public ContentValues getCameraNoteValues(Uri noteLinkUri) {
        ContentValues values = new ContentValues();
//        values.put(TimeKeeperContract.Notes.TIME_RECORD_ID, );
        return null;
    }

    @Override
    public void onTimeRecordSelected(String id) {
        Log.d("MainActivity", "onTimeRecordSelected [" + id + "]");
    }

    @Override
    public void onProjectSelected(String id) {
        Log.d("MainActivity", "onProjectSelected [" + id + "]");
        mSelectedProject = id;
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
            intent.putExtra(ProjectDetailActivity.EXTRA_DUAL_PANE, mDualPane);
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
    public void onNoteChanged(int result, ContentValues values) {
        // remove the fragment
        String projectId = values.getAsString(TimeKeeperContract.TimeRecords.PROJECT_ID);
        Log.d("MainActivity", "OnNoteChanged: " + projectId);
        if(projectId != null) {
            Fragment projectDetailsFragment = ProjectDetailsFragment.newInstance(values.getAsString(TimeKeeperContract.TimeRecords.PROJECT_ID));
            getFragmentManager().beginTransaction()
                    .replace(R.id.details_container, projectDetailsFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        }else {
            Fragment noteFragment = getFragmentManager().findFragmentById(R.id.details_container);
            getFragmentManager().beginTransaction()
                    .remove(noteFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        }

        // todo - if a time record was created specifically for this note, stop the clock
    }

    public void pushExternalNoteParams(ContentValues values) {
        mExternalNoteActivityParams = values;
    }
    public ContentValues popExternalNoteParams() {
        ContentValues t = mExternalNoteActivityParams;
        mExternalNoteActivityParams = null;
        return t;
    }

    public void openNoteView(String projectId, String timeRecordId, String noteType) {
        if(noteType.equals(TimeKeeperContract.Notes.TEXT_NOTE)) {
            openTextNoteView(projectId, timeRecordId, noteType);
        }else if(noteType.equals(TimeKeeperContract.Notes.LIST_NOTE)) {
        }else if(noteType.equals(TimeKeeperContract.Notes.CAMERA_NOTE)) {
            openCameraNoteView(projectId, timeRecordId, noteType);
        }else if(noteType.equals(TimeKeeperContract.Notes.AUDIO_NOTE)) {
            openAudioNoteView(projectId, timeRecordId, noteType);
        }
    }

    public Intent getIntentFor(String projectId, String timeRecordId, String noteType) {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), NoteTakerActivity.class);
        intent.putExtra(NoteTakerActivity.EXTRA_PROJECT_ID, projectId);
        intent.putExtra(NoteTakerActivity.EXTRA_TIME_RECORD_ID, timeRecordId);
        intent.putExtra(NoteTakerActivity.EXTRA_NOTE_TYPE, noteType);
        return intent;
    }

    public void displayNoteFragment(Fragment noteFragment) {
        getFragmentManager().beginTransaction()
                .replace(R.id.details_container, noteFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    public void openTextNoteView(String projectId, String timeRecordId, String noteType) {
        if(mDualPane) {
            displayNoteFragment(TextNoteFragment.newInstance(projectId, timeRecordId, null));
        } else {
            startActivityForResult(getIntentFor(projectId, timeRecordId, noteType), ADD_TEXT_NOTE);
        }
    }

    public void openAudioNoteView(String projectId, String timeRecordId, String noteType) {
        if(mDualPane) {
            displayNoteFragment(AudioCaptureFragment.newInstance(projectId, timeRecordId, FileHelper.getOutputFileUri(this, "TimeKeeper", FileHelper.AUDIO_TYPE)));
        }else {
            startActivityForResult(getIntentFor(projectId, timeRecordId, noteType), ADD_AUDIO_NOTE);
        }
    }

    public void openCameraNoteView(String projectId, String timeRecordId, String noteType) {
        // todo - add ability to save the images inside the app as a setting (default: stored in general photos directory for easy access)
        Uri capturedFileUri = FileHelper.getOutputFileUri(this, "TimeKeeper", FileHelper.CAMERA_TYPE, Environment.DIRECTORY_PICTURES);

        ContentValues values = new ContentValues();
        values.put(EXTERNAL_PROJECT_ID, projectId);
        values.put(EXTERNAL_TIME_RECORD_ID, timeRecordId);
        values.put(EXTERNAL_NOTE_TYPE, noteType);
        values.put(EXTERNAL_LINK, capturedFileUri.toString());
        pushExternalNoteParams(values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedFileUri);
        startActivityForResult(intent, ADD_CAMERA_NOTE);
    }

    private class OpenNoteViewTask extends AsyncTask<Bundle, Void, Bundle> {
        public static final String ARG_PROJECT_ID = "projectId";
        public static final String ARG_NOTE_TYPE = "noteType";
        private static final String ARG_TIME_RECORD_ID = "timeRecordId";

        @Override
        protected void onPostExecute(Bundle values) {
            super.onPostExecute(values);
            openNoteView(values.getString(ARG_PROJECT_ID), values.getString(ARG_TIME_RECORD_ID), values.getString(ARG_NOTE_TYPE));
        }

        @Override
        protected Bundle doInBackground(Bundle... params) {
            Bundle values = params[0];
            values.putString(ARG_TIME_RECORD_ID, getRunningTimeRecordFor(values.getString(ARG_PROJECT_ID)));
            return values;
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

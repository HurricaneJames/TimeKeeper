package com.easytimelog.timekeeper.views;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.easytimelog.timekeeper.R;
import com.easytimelog.timekeeper.data.TimeKeeperContract;
import com.easytimelog.timekeeper.util.DatabaseHelper;
import com.easytimelog.timekeeper.util.FileHelper;

public class NoteTakerActivity extends Activity implements OnNoteChangeListener {
    public static final String EXTRA_PROJECT_ID = "project_id";
    public static final String EXTRA_TIME_RECORD_ID = "time_record_id";
    public static final String EXTRA_NOTE_TYPE = "note_type";
    public static final String NOTE_VALUES = "note_values";

    private String mProjectId;
    private String mTimeRecordId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_taker);
        if(savedInstanceState == null) {

            mProjectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);
            mTimeRecordId = getIntent().getStringExtra(EXTRA_TIME_RECORD_ID);
            String noteType = getIntent().getStringExtra(EXTRA_NOTE_TYPE);

            getFragmentManager().beginTransaction()
                    .add(R.id.container, getFragmentForNoteType(noteType))
                    .commit();
        }
    }

    private Fragment getFragmentForNoteType(String noteType) {
        Fragment noteFragment = null;
        if(TimeKeeperContract.Notes.TEXT_NOTE.equals(noteType)) {
            noteFragment = TextNoteFragment.newInstance(mProjectId, mTimeRecordId, null);
        }else if(TimeKeeperContract.Notes.AUDIO_NOTE.equals(noteType)) {
            noteFragment = AudioCaptureFragment.newInstance(mProjectId, mTimeRecordId, FileHelper.getOutputFileUri(this, "TimeKeeper", FileHelper.AUDIO_TYPE));
        }
        return noteFragment;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.note_taker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNoteChanged(int result, ContentValues values) {
        returnToCallingActivity(result == OnNoteChangeListener.RESULT_ACCEPT, values);
    }

    private void returnToCallingActivity(boolean approve, ContentValues values) {
        Intent intent = new Intent();
        intent.putExtra(NOTE_VALUES, values);
        setResult(approve ? Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d("NoteTakerActivity", "On Back Pressed");
        // trigger a cancel result
        returnToCallingActivity(false, new ContentValues());
    }

}

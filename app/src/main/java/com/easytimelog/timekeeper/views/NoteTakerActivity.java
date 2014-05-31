package com.easytimelog.timekeeper.views;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.easytimelog.timekeeper.R;

public class NoteTakerActivity extends Activity implements OnNoteChangeListener {
    public static final String EXTRA_PROJECT_ID = "project_id";
    public static final String EXTRA_TIME_RECORD_ID = "time_record_id";
    public static final String EXTRA_NOTE_TYPE = "note_type";
    public static final String NOTE_VALUES = "note_values";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_taker);
        if(savedInstanceState == null) {

            String projectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);
            String timeRecordId = getIntent().getStringExtra(EXTRA_TIME_RECORD_ID);
            String noteType = getIntent().getStringExtra(EXTRA_NOTE_TYPE);

            getFragmentManager().beginTransaction()
                    .add(R.id.container, TextNoteFragment.newInstance(projectId, timeRecordId, null))
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.note_taker, menu);
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
    public void onNoteChanged(int result, ContentValues values) {
        returnToCallingActivity(result == OnNoteChangeListener.RESULT_ACCEPT, values);
    }

    private void returnToCallingActivity(boolean approve, ContentValues values) {
        Intent intent = new Intent();
        intent.putExtra(NOTE_VALUES, values);
        setResult(approve ? RESULT_ACCEPT : RESULT_CANCELED, intent);
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

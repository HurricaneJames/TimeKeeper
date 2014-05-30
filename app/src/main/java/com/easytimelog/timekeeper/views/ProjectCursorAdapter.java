package com.easytimelog.timekeeper.views;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.easytimelog.timekeeper.R;
import com.easytimelog.timekeeper.data.TimeKeeperContract;

public class ProjectCursorAdapter extends CursorAdapter implements View.OnClickListener {
    private OnNoteRequestedListener mNoteRequestListener;
    private int mProjectId;

    private LayoutInflater mInflater;
    public ProjectCursorAdapter(OnNoteRequestedListener noteRequestListener, Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mNoteRequestListener = noteRequestListener;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.project_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String  name      = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.Projects.NAME));
        boolean running   = cursor.getInt(cursor.getColumnIndex(TimeKeeperContract.Projects.RUNNING)) != 0;
        String  startedAt = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.Projects.STARTED_AT));
        long    duration  = cursor.getLong(cursor.getColumnIndex(TimeKeeperContract.Projects.DURATION));
        int     runningTimeRecordId = cursor.getInt(cursor.getColumnIndex(TimeKeeperContract.Projects.RUNNING_TIME_RECORD));
        mProjectId = cursor.getInt(cursor.getColumnIndex(TimeKeeperContract.Projects._ID));

        String textCount   = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.Projects.TEXT_NOTE_COUNT));
        String listCount   = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.Projects.LIST_NOTE_COUNT));
        String cameraCount = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.Projects.CAMERA_NOTE_COUNT));
        String audioCount  = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.Projects.AUDIO_NOTE_COUNT));

        TextView nameButton = (TextView)view.findViewById(R.id.project_item_name);
        TimerButton timerButton = (TimerButton)view.findViewById(R.id.project_item_timer);
        ((TextView)view.findViewById(R.id.project_item_text_count)).setText(textCount);
        ((TextView)view.findViewById(R.id.project_item_list_count)).setText(listCount);
        ((TextView)view.findViewById(R.id.project_item_camera_count)).setText(cameraCount);
        ((TextView)view.findViewById(R.id.project_item_audio_count)).setText(audioCount);
        ((ImageButton)view.findViewById(R.id.project_item_new_text_note_button)).setOnClickListener(this);
        ((ImageButton)view.findViewById(R.id.project_item_new_list_note_button)).setOnClickListener(this);
        ((ImageButton)view.findViewById(R.id.project_item_new_camera_note_button)).setOnClickListener(this);
        ((ImageButton)view.findViewById(R.id.project_item_new_audio_note_button)).setOnClickListener(this);


        nameButton.setText(name);
        timerButton.setOnClickListener(new TimerClickListener(context, mProjectId, running, runningTimeRecordId, startedAt));
        setupTimerButton(timerButton, running, startedAt, duration);
    }

    @Override
    public void onClick(View view) {
        String projectId = Long.toString(mProjectId);
        switch(view.getId()) {
            case R.id.project_item_new_text_note_button:
                mNoteRequestListener.onNewNoteRequested(projectId, TimeKeeperContract.Notes.TEXT_NOTE);
                break;
            case R.id.project_item_new_list_note_button:
                mNoteRequestListener.onNewNoteRequested(projectId, TimeKeeperContract.Notes.LIST_NOTE);
                break;
            case R.id.project_item_new_camera_note_button:
                mNoteRequestListener.onNewNoteRequested(projectId, TimeKeeperContract.Notes.CAMERA_NOTE);
                break;
            case R.id.project_item_new_audio_note_button:
                mNoteRequestListener.onNewNoteRequested(projectId, TimeKeeperContract.Notes.AUDIO_NOTE);
                break;
        }
    }

    public static void setupTimerButton(TimerButton timerButton, boolean running, String startedAt, long duration) {
        timerButton.reset();
        timerButton.setBaseDuration(duration);
        updateTimerButton(timerButton, running, startedAt);
    }

    public static void updateTimerButton(TimerButton timerButton, boolean running, String startedAt) {
        if(startedAt != null) { timerButton.setBaseStartTime(startedAt); }
        if(running) {
            timerButton.start();
        }else {
            timerButton.stop();
        }
    }

    private class TimerClickListener implements View.OnClickListener {
        private Context mContext;
        private int mProjectId;
        private boolean mRunning;
        private int mCurrentTimeRecordId;
        private String mStartedAt;

        public TimerClickListener(Context context, int projectId, boolean running, int runningTimeRecordId, String startedAt) {
            // todo - ultimately, this probably needs to be wrapped up in a Project model object
            this.mContext = context;
            mProjectId    = projectId;
            mStartedAt    = startedAt;
            mRunning      = running;
            mCurrentTimeRecordId = runningTimeRecordId;
        }
        @Override
        public void onClick(View view) {
            if(mRunning) {
                mRunning = false;
                mStartedAt = null;
                finalizeTimeRecord(mCurrentTimeRecordId);
            }else {
                mRunning = true;
                mStartedAt = TimeKeeperContract.getStandardDateString(System.currentTimeMillis());
                mCurrentTimeRecordId = addNewTimeRecord(mProjectId);
            }
            updateTimerButton((TimerButton) view, mRunning, mStartedAt);
        }

        private int addNewTimeRecord(int projectId) {
            ContentValues values = new ContentValues();
            values.put(TimeKeeperContract.TimeRecords.START_AT, mStartedAt);
            values.put(TimeKeeperContract.TimeRecords.PROJECT_ID, projectId);
            Uri newRecordUri = mContext.getContentResolver().insert(TimeKeeperContract.TimeRecords.CONTENT_URI, values);
            return Integer.parseInt(newRecordUri.getLastPathSegment());
        }
        private void finalizeTimeRecord(int timeRecordId) {
            ContentValues values = new ContentValues();
            values.put(TimeKeeperContract.TimeRecords.END_AT, System.currentTimeMillis());
            mContext.getContentResolver().update(ContentUris.withAppendedId(TimeKeeperContract.TimeRecords.CONTENT_URI, timeRecordId), values, null, null);
        }
    }
}

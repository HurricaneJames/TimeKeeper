package com.easytimelog.timekeeper.views;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.easytimelog.timekeeper.R;
import com.easytimelog.timekeeper.controllers.AudioController;
import com.easytimelog.timekeeper.data.TimeKeeperContract;

import org.joda.time.DateTime;
import org.joda.time.Period;

import java.io.IOException;

public class TimeRecordCursorAdapter extends CursorTreeAdapter {
    private LayoutInflater mInflater;
    private Context mContext;

    public TimeRecordCursorAdapter(Cursor cursor, Context context, boolean autoRequery) {
        super(cursor, context, autoRequery);
        this.mContext = context.getApplicationContext();
        this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    protected Cursor getChildrenCursor(Cursor groupCursor) {
        // todo - make this asynchronous
        int timeRecordId = groupCursor.getInt(groupCursor.getColumnIndex(TimeKeeperContract.TimeRecords._ID));
        Cursor notes = mContext.getContentResolver().query(
                TimeKeeperContract.Notes.CONTENT_URI,
                TimeKeeperContract.Notes.PROJECTION_ALL,
                TimeKeeperContract.Notes.whereTimeRecordId(timeRecordId),
                null,
                TimeKeeperContract.Notes.CREATED_AT + " ASC");
        return notes;
    }

    @Override
    protected View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
        return mInflater.inflate(R.layout.timerecord_list_item, parent, false);
    }

    @Override
    protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
        TextView noteCount   = (TextView)view.findViewById(R.id.timeRecordListItemViewNoteCount);
        TextView description = (TextView)view.findViewById(R.id.timeRecordListItemViewDescription);
        TextView timer       = (TextView)view.findViewById(R.id.timeRecordListItemViewTime);

        String startAt = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.TimeRecords.START_AT));
        String endAt   = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.TimeRecords.END_AT));
        Period duration = new Period(new DateTime(startAt), new DateTime(endAt));

        String notes = getNoteCount(context, cursor, cursor.getColumnIndex(TimeKeeperContract.TimeRecords.NOTE_COUNT));

        noteCount.setText(notes);
        description.setText(context.getString(R.string.started) + " " + DateFormatter.getHumanFriendlyDate(new DateTime(startAt)));
        timer.setText(DateFormatter.DEFAULT.print(duration));
    }

    private String getNoteCount(Context context, Cursor projectCursor, int noteColumn) {
        int count = projectCursor.getInt(noteColumn);
        return "" + count + " " + ((count == 1) ? context.getString(R.string.note) : context.getString(R.string.notes));
    }

    @Override
    protected View newChildView(Context context, Cursor cursor, boolean isLastChild, ViewGroup parent) {
        return mInflater.inflate(R.layout.note_list_item, parent, false);
    }

    @Override
    protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
        String noteType = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.Notes.NOTE_TYPE));
        String scribble = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.Notes.SCRIBBLE));
        String link     = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.Notes.LINK));
        blankChildView(view);
        if(noteType.equals(TimeKeeperContract.Notes.CAMERA_NOTE)) {
            ImageView imageSummary = (ImageView) view.findViewById(R.id.noteSummaryImage);
            imageSummary.setImageURI(Uri.parse(link));
            imageSummary.setVisibility(View.VISIBLE);
        }else if(TimeKeeperContract.Notes.AUDIO_NOTE.equals(noteType)) {
            setupAudioPlayer(view.findViewById(R.id.notePlaybackControls), link);
        }else {
            // todo - DevTool - remove noteType brackets
            TextView summary = (TextView) view.findViewById(R.id.noteSummary);
            summary.setText("<" + noteType + ">" + scribble + "</" + noteType + ">");
            summary.setVisibility(View.VISIBLE);
        }
    }

    protected void setupAudioPlayer(View audioControls, String link) {
        new AudioControlViewHandler(audioControls, link);
        audioControls.setVisibility(View.VISIBLE);
    }

    protected void blankChildView(View view) {
        view.findViewById(R.id.noteSummary).setVisibility(View.GONE);
        view.findViewById(R.id.noteSummaryImage).setVisibility(View.GONE);
        view.findViewById(R.id.notePlaybackControls).setVisibility(View.GONE);
    }

    private class AudioControlViewHandler implements View.OnClickListener, AudioController.UpdateListener {
        private ImageButton     mPlayButton;
        private SeekBar         mProgressBar;
        private AudioController mController;
        private String mLink;

        public AudioControlViewHandler(View audioControls, String link) {
            mLink = link;
            mPlayButton  = (ImageButton) audioControls.findViewById(R.id.audio_player_play_pause_button);
            mProgressBar = (SeekBar)     audioControls.findViewById(R.id.audio_player_progress_bar);
            mController  = AudioController.getController();

            // todo - implement setImageResource based on return from mController.isPlaying(link)
            mPlayButton.setImageResource(R.drawable.ic_action_play);
            mPlayButton.setOnClickListener(this);

            mProgressBar.setProgress(0);
            mProgressBar.setMax(100);
            // todo - implement setProgress from mController.getProgress
        }

        @Override
        public void onClick(View v) {
Log.d("TimeRecordCursorAdapter", "What on Earth is Calling onClick");
            if(mController.isPlaying(mLink)) {
                mController.pause(mLink);
                mPlayButton.setImageResource(R.drawable.ic_action_play);
                mController.removeUpdateListener(this);
            }else {
                mController.play(mLink);
                mPlayButton.setImageResource(R.drawable.ic_action_pause);
                mController.addUpdateListener(this);
            }
        }

        @Override
        public void onProgress(int currentTime, int duration) {}

        @Override
        public void onChanged(String previouslyPlaying, String nowPlaying) {}

        @Override
        public void onComplete() {}
    }
}

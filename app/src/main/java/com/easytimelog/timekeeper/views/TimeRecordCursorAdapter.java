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
            View noteControls = view.findViewById(R.id.notePlaybackControls);
            setupAudioPlayer(view, link);
        }else {
            // todo - DevTool - remove noteType brackets
            TextView summary = (TextView) view.findViewById(R.id.noteSummary);
            summary.setText("<" + noteType + ">" + scribble + "</" + noteType + ">");
            summary.setVisibility(View.VISIBLE);
        }
    }

    protected void setupAudioPlayer(View audioPlayerView, String link) {
        View audioControls = audioPlayerView.findViewById(R.id.notePlaybackControls);
        ImageButton playButton  = (ImageButton) audioControls.findViewById(R.id.audio_player_play_pause_button);
        SeekBar     progressBar = (SeekBar)     audioControls.findViewById(R.id.audio_player_progress_bar);
        AudioPlayer player = (AudioPlayer) playButton.getTag(R.id.TAG_AUDIO_PLAYER);
        if(player == null) {
            playButton.setTag(R.id.TAG_AUDIO_PLAYER, new AudioPlayer(link, playButton, progressBar));
        }else {
            player.reset(link);
        }
        audioControls.setVisibility(View.VISIBLE);
    }

    protected void blankChildView(View view) {
        view.findViewById(R.id.noteSummary).setVisibility(View.GONE);
        view.findViewById(R.id.noteSummaryImage).setVisibility(View.GONE);
        view.findViewById(R.id.notePlaybackControls).setVisibility(View.GONE);
    }

    private class AudioPlayer implements View.OnClickListener, MediaPlayer.OnCompletionListener {
        private ImageButton mPlayButton;
        private SeekBar mPlaybackBar;
        private MediaPlayer mMediaPlayer;
        private boolean mPlaying = false;

        public AudioPlayer(String link, ImageButton playButton, SeekBar playbackBar) {
            mMediaPlayer = new MediaPlayer();
            mPlayButton = playButton;
            mPlayButton.setOnClickListener(this);
            mPlaybackBar = playbackBar;

            setupPlayer(link);
        }

        // todo - implement
        public AudioPlayer reset(String link) {
            setupPlayer(link);
            return this;
        }

        private void setupPlayer(String link) {
            try {
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(link);
                mMediaPlayer.setOnCompletionListener(this);
                mMediaPlayer.prepare();
            }catch(IllegalArgumentException e) { e.printStackTrace();
            }catch(IllegalStateException e) { e.printStackTrace();
            }catch(IOException e) { e.printStackTrace(); }

            mPlaying = false;
            mPlayButton.setImageResource(R.drawable.ic_action_play);
            mPlaybackBar.setProgress(0);
            mPlaybackBar.setMax(100);
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            mPlaybackBar.setProgress(0);
            mMediaPlayer.seekTo(0);
            mPlayButton.setImageResource(R.drawable.ic_action_play);
        }

        @Override
        public void onClick(View v) {
            if(mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mPlayButton.setImageResource(R.drawable.ic_action_play);
            }else {
                mMediaPlayer.start();
                mPlayButton.setImageResource(R.drawable.ic_action_pause);
            }
        }
    }
}

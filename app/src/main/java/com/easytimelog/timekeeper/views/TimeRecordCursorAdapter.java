package com.easytimelog.timekeeper.views;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.CursorTreeAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.easytimelog.timekeeper.R;
import com.easytimelog.timekeeper.data.TimeKeeperContract;

import org.joda.time.DateTime;
import org.joda.time.Period;

import java.io.File;

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

        TextView summary = (TextView) view.findViewById(R.id.noteSummary);
        ImageView imageSummary = (ImageView) view.findViewById(R.id.noteSummaryImage);

        if(noteType.equals(TimeKeeperContract.Notes.CAMERA_NOTE)) {
            summary.setVisibility(View.GONE);
            imageSummary.setImageURI(Uri.parse(link));
            imageSummary.setVisibility(View.VISIBLE);
        }else if(TimeKeeperContract.Notes.AUDIO_NOTE.equals(noteType)) {
            imageSummary.setVisibility(View.GONE);
            // todo - DevTool - remove noteType brackets
            summary.setText("<" + noteType + ">" + link + "</" + noteType + ">");
            summary.setVisibility(View.VISIBLE);

        }else {
            imageSummary.setVisibility(View.GONE);
            // todo - DevTool - remove noteType brackets
            summary.setText("<" + noteType + ">" + scribble + "</" + noteType + ">");
            summary.setVisibility(View.VISIBLE);
        }
    }
}

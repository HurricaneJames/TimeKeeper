package com.easytimelog.timekeeper.views;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.CursorTreeAdapter;
import android.widget.TextView;

import com.easytimelog.timekeeper.R;
import com.easytimelog.timekeeper.data.TimeKeeperContract;

import org.joda.time.DateTime;
import org.joda.time.Period;

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

        String notes = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.TimeRecords.NOTE_COUNT));

        noteCount.setText(notes);
        description.setText(context.getString(R.string.started) + " " + DateFormatter.getHumanFriendlyDate(new DateTime(startAt)));
        timer.setText(DateFormatter.DEFAULT.print(duration));
    }

    @Override
    protected View newChildView(Context context, Cursor cursor, boolean isLastChild, ViewGroup parent) {
        return mInflater.inflate(R.layout.note_list_item, parent, false);
    }

    @Override
    protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
        String noteType = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.Notes.NOTE_TYPE));
        String scribble = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.Notes.SCRIBBLE));
        TextView summary = (TextView)view.findViewById(R.id.noteSummary);
        summary.setText("<" + noteType + ">" + scribble + "</" + noteType + ">");
    }
}

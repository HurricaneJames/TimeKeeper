package com.easytimelog.timekeeper.data;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;

import com.easytimelog.timekeeper.R;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public class TimeRecordCursorAdapter extends CursorAdapter {
    private static final PeriodFormatter DURATION_FORMATTER;
    static {
        DURATION_FORMATTER = new PeriodFormatterBuilder()
                .printZeroRarelyLast()
                .appendYears()
                .appendSuffix(" year", " years")
                .appendSeparator(" and ")
                .appendMonths()
                .appendSuffix(" month", " months")
                .appendSeparator(", ")
                .appendDays()
                .appendSuffix(" day", " days")
                .appendSeparator(", ")
                .appendHours()
                .appendSuffix(" hour", " hours")
                .appendSeparator(", ")
                .appendMinutes()
                .appendSuffix(" minute", " minutes")
                .appendSeparator(", ")
                .appendSeconds()
                .appendSuffix(" second", " seconds")
                .toFormatter();
    }

    private LayoutInflater mInflater;
    public TimeRecordCursorAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.timerecord_list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Button description = (Button)view.findViewById(R.id.timeRecordListItemViewDescriptionButton);
        Button timer       = (Button)view.findViewById(R.id.timeRecordListItemViewTimeButton);

        String startAt = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.TimeRecords.START_AT));
        String endAt   = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.TimeRecords.END_AT));
        Period duration = new Period(new DateTime(startAt), new DateTime(endAt));

        description.setText(startAt);
        timer.setText(DURATION_FORMATTER.print(duration));
    }
}

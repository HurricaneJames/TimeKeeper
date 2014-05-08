package com.easytimelog.timekeeper.views;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.easytimelog.timekeeper.R;
import com.easytimelog.timekeeper.data.TimeKeeperContract;

import org.joda.time.Duration;

public class ProjectCursorAdapter extends CursorAdapter {
    private LayoutInflater mInflater;
    public ProjectCursorAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.project_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String name = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.Projects.NAME));
        long duration = cursor.getLong(cursor.getColumnIndex(TimeKeeperContract.Projects.DURATION));
//        String startedAt = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.Projects.STARTED_AT));
//        boolean running = cursor.getInt(cursor.getColumnIndex(TimeKeeperContract.Projects.RUNNING)) != 0;

        TextView nameButton = (TextView)view.findViewById(R.id.project_item_name);
        Button timerButton = (Button)view.findViewById(R.id.project_item_timer);

        nameButton.setText(name);
        timerButton.setText(DateFormatter.DEFAULT.print(new Duration(duration).toPeriod()));
    }
}

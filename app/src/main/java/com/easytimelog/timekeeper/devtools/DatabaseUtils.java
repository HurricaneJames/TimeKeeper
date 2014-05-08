package com.easytimelog.timekeeper.devtools;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.easytimelog.timekeeper.data.TimeKeeperContract;
import com.easytimelog.timekeeper.data.TimeKeeperSqlOpenHelper;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.Date;
import java.util.Random;

public class DatabaseUtils {
    public static int nextProjectId = 0;
    public static void wipeDatabase(Context context) {
        context.deleteDatabase("timekeeper.db");
    }

    public static void tempSeedDatabase(Context context, int count) {
        Random random = new Random();
        long projectId = addBlankProject(context);
        // should be 9 hours total
//        DateTime[] fixedDates = {
//                new DateTime(2014, 05, 05,  6, 0), new DateTime(2014, 05, 05,  7, 0),
//                new DateTime(2014, 05, 05, 14, 0), new DateTime(2014, 05, 05, 16, 0),
//                new DateTime(2014, 05, 05, 12, 0), new DateTime(2014, 05, 05, 17, 0),
//                new DateTime(2013, 05, 05,  6, 0), new DateTime(2013, 05, 05,  7, 0),
//                new DateTime(2014, 05, 05, 15, 0), new DateTime(2014, 05, 05, 19, 0)
//        };
//        for(int i=0; i<fixedDates.length; i++) {
//            addTimeRecord(context, fixedDates[i].toDate(), fixedDates[++i].toDate(), projectId);
//        }


        for(int i=0; i<count; i++) {
            Date start = new Date(System.currentTimeMillis() - 1000 * 3600 * random.nextInt(24) - 1000 * 60 * random.nextInt(60) - 1000 * random.nextInt(60));
            Date end   = new Date(start.getTime() + 1000 * 3600 * random.nextInt(2) + 1000* 60 * random.nextInt(60) + 1000* random.nextInt(60));
            addTimeRecord(context, start, end, projectId);
        }

//      should not need to use this anymore
//        updateProjectCache(context, 1);
    }

    public static long addBlankProject(Context context) {
        ContentValues values = new ContentValues();
        values.put(TimeKeeperContract.Projects.NAME, "Blank Project: " + nextProjectId++);

        Uri newProjectUri = context.getContentResolver().insert(TimeKeeperContract.Projects.CONTENT_URI, values);
        String newId = newProjectUri.getLastPathSegment();
        return Long.parseLong(newId);
    }

    public static void addTimeRecord(Context context, Date start, Date end, long projectId) {
        ContentValues values = new ContentValues();
        values.put(TimeKeeperContract.TimeRecords.START_AT, start.getTime());
        if(end != null) { values.put(TimeKeeperContract.TimeRecords.END_AT, end.getTime()); }
        values.put(TimeKeeperContract.TimeRecords.PROJECT_ID, projectId);
        context.getContentResolver().insert(TimeKeeperContract.TimeRecords.CONTENT_URI, values);
    }

//    public static void updateProjectCache(Context context, int projectId) {
//        Cursor timeRecordsCursor = context.getContentResolver().query(TimeKeeperContract.TimeRecords.CONTENT_URI, TimeKeeperContract.TimeRecords.PROJECTION_ALL, TimeKeeperContract.TimeRecords.PROJECT_ID + " = " + projectId, null, TimeKeeperContract.TimeRecords.START_AT + " ASC");
//        if(!timeRecordsCursor.moveToFirst()) { return; }
//
//        DateTime startAt = new DateTime(timeRecordsCursor.getString(timeRecordsCursor.getColumnIndex(TimeKeeperContract.TimeRecords.START_AT)));
//        DateTime endAt   = new DateTime(timeRecordsCursor.getString(timeRecordsCursor.getColumnIndex(TimeKeeperContract.TimeRecords.END_AT)));
//        Duration totalDuration = new Duration(startAt, endAt);
//        DateTime maxEnd = endAt;
//        while(timeRecordsCursor.moveToNext()) {
//            startAt = new DateTime(timeRecordsCursor.getString(timeRecordsCursor.getColumnIndex(TimeKeeperContract.TimeRecords.START_AT)));
//            endAt   = new DateTime(timeRecordsCursor.getString(timeRecordsCursor.getColumnIndex(TimeKeeperContract.TimeRecords.END_AT)));
//            Duration duration = new Duration(startAt, endAt);
//            Duration deltaEnds = new Duration(maxEnd, endAt);
//            if(deltaEnds.compareTo(Duration.ZERO) >= 0) {
//                if (duration.compareTo(deltaEnds) >= 0) {
//                    totalDuration = totalDuration.plus(deltaEnds);
//                } else {
//                    totalDuration = totalDuration.plus(duration);
//                    maxEnd = endAt;
//                }
//            }
//        }
//        ContentValues values = new ContentValues();
//        values.put(TimeKeeperContract.Projects.DURATION, totalDuration.getMillis());
//        context.getContentResolver().update(ContentUris.withAppendedId(TimeKeeperContract.Projects.CONTENT_URI, 1), values, null, null);
//    }
}

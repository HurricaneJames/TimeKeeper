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

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class DatabaseUtils {
    public static String generateSQLPlaceHolders(int count) {
        if(count < 1) { throw new IllegalArgumentException("Cannot generate placeholders for no arguments!"); }
        StringBuilder stringBuilder = new StringBuilder(count*2 - 1);
        stringBuilder.append("?");
        for(int i=1; i<count; i++) {
            stringBuilder.append(",?");
        }
        return stringBuilder.toString();
    }


    public static int nextProjectId = 0;
    public static void wipeDatabase(Context context) {
        context.deleteDatabase("timekeeper.db");
    }

    public static void tempSeedDatabase(Context context, int projectCount, int timeRecordCount, int notesCount) {
        Random random = new Random();
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


        ArrayList<Integer> projectIds = new ArrayList<Integer>(projectCount);
        for(int i=0; i<projectCount; i++) {
            projectIds.add(addBlankProject(context));
        }
        for(int i=0; i<timeRecordCount; i++) {
            Date start = new Date(System.currentTimeMillis() - 1000 * 3600 * random.nextInt(24) - 1000 * 60 * random.nextInt(60) - 1000 * random.nextInt(60));
            Date end   = new Date(start.getTime() + 1000 * 3600 * random.nextInt(2) + 1000* 60 * random.nextInt(60) + 1000* random.nextInt(60));
            long projectId = projectIds.get(random.nextInt(projectIds.size()));
            int timeRecordId = addTimeRecord(context, start, end, projectId);

            int notesForThisRecord = random.nextInt(notesCount);
            for(int j=0; j<notesForThisRecord; j++) {
                addNote(context, "text", "Scribble: " + random.nextInt(1000), null, timeRecordId);
            }
        }

//      should not need to use this anymore
//        updateProjectCache(context, 1);
    }

    public static Integer addBlankProject(Context context) {
        ContentValues values = new ContentValues();
        values.put(TimeKeeperContract.Projects.NAME, "Blank Project: " + nextProjectId++);

        Uri newProjectUri = context.getContentResolver().insert(TimeKeeperContract.Projects.CONTENT_URI, values);
        String newId = newProjectUri.getLastPathSegment();
        return new Integer(newId);
    }

    public static int addTimeRecord(Context context, Date start, Date end, long projectId) {
        ContentValues values = new ContentValues();
        values.put(TimeKeeperContract.TimeRecords.START_AT, start.getTime());
        if(end != null) { values.put(TimeKeeperContract.TimeRecords.END_AT, end.getTime()); }
        values.put(TimeKeeperContract.TimeRecords.PROJECT_ID, projectId);
        Uri insertUri = context.getContentResolver().insert(TimeKeeperContract.TimeRecords.CONTENT_URI, values);
        return Integer.parseInt(insertUri.getLastPathSegment());
    }

    public static int addNote(Context context, String noteType, String scribble, String link, int timeRecordId) {
        ContentValues values = new ContentValues();
        values.put(TimeKeeperContract.Notes.NOTE_TYPE, noteType);
        values.put(TimeKeeperContract.Notes.LINK, link);
        values.put(TimeKeeperContract.Notes.SCRIBBLE, scribble);
        values.put(TimeKeeperContract.Notes.TIME_RECORD_ID, timeRecordId);
        Uri insertUri = context.getContentResolver().insert(TimeKeeperContract.Notes.CONTENT_URI, values);
        return Integer.parseInt(insertUri.getLastPathSegment());
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

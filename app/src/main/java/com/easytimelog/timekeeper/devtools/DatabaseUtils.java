package com.easytimelog.timekeeper.devtools;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.easytimelog.timekeeper.data.TimeKeeperContract;
import com.easytimelog.timekeeper.data.TimeKeeperSqlOpenHelper;

import java.util.Date;
import java.util.Random;

public class DatabaseUtils {
    public static void wipeDatabase(Context context) {
        context.deleteDatabase("timekeeper.db");
//        TimeKeeperSqlOpenHelper openHelper = new TimeKeeperSqlOpenHelper(context);
//        SQLiteDatabase db = openHelper.getWritableDatabase();
    }

    public static void tempSeedDatabase(Context context, int count) {
        Random random = new Random();
        long projectId = addBlankProject(context);
        for(int i=0; i<count; i++) {
            Date start = new Date(System.currentTimeMillis() - 1000 * 3600 * random.nextInt(24) - 1000 * 60 * random.nextInt(60) - 1000 * random.nextInt(60));
            Date end   = new Date(start.getTime() + 1000 * 3600 * random.nextInt(2) + 1000* 60 * random.nextInt(60) + 1000* random.nextInt(60));
            addTimeRecord(context, start, end, projectId);
        }
    }

    public static long addBlankProject(Context context) {
        ContentValues values = new ContentValues();
        values.put(TimeKeeperContract.Projects.NAME, "Unknown Event");

        Uri newProjectUri = context.getContentResolver().insert(TimeKeeperContract.Projects.CONTENT_URI, values);
        String newId = newProjectUri.getLastPathSegment();
        return Long.parseLong(newId);
    }

    public static void addTimeRecord(Context context, Date start, Date end, long projectId) {
        ContentValues values = new ContentValues();
        values.put(TimeKeeperContract.TimeRecords.START_AT, start.getTime());
        values.put(TimeKeeperContract.TimeRecords.END_AT, end.getTime());
        values.put(TimeKeeperContract.TimeRecords.PROJECT_ID, projectId);
        context.getContentResolver().insert(TimeKeeperContract.TimeRecords.CONTENT_URI, values);
    }
}

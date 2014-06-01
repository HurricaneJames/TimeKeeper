package com.easytimelog.timekeeper.util;

import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.easytimelog.timekeeper.data.TimeKeeperContract;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class DatabaseHelper {
    public static Set<String> getIdsFromCursor(Cursor cursor, int idColumn, Set<String> intoSet) {
        if(!cursor.moveToFirst()) { return intoSet; }
        do {
            intoSet.add(cursor.getString(idColumn));
        }while(cursor.moveToNext());
        return intoSet;
    }
    public static Set<String> getIdsFromCursor(Cursor cursor, int idColumn) {
        HashSet<String> ids = new HashSet<String>();
        return getIdsFromCursor(cursor, idColumn, ids);
    }


    public static String addTimeRecord(Context context, Date start, Date end, String projectId) {
        ContentValues values = new ContentValues();
        values.put(TimeKeeperContract.TimeRecords.START_AT, start.getTime());
        if(end != null) { values.put(TimeKeeperContract.TimeRecords.END_AT, end.getTime()); }
        values.put(TimeKeeperContract.TimeRecords.PROJECT_ID, projectId);
        Uri insertUri = context.getContentResolver().insert(TimeKeeperContract.TimeRecords.CONTENT_URI, values);
        return insertUri.getLastPathSegment();
    }

    public static void addNote(Context context, ContentValues noteValues) {
        AsyncQueryHandler queryHandler = new AsyncQueryHandler(context.getApplicationContext().getContentResolver()) {};
        queryHandler.startInsert(0, null, TimeKeeperContract.Notes.CONTENT_URI, noteValues);
    }

    public static void updateNote(Context context, String noteId, ContentValues noteValues) {
        updateNote(context,
                ContentUris.withAppendedId(TimeKeeperContract.Notes.CONTENT_URI, Long.parseLong(noteId)),
                noteValues);
    }

    public static void updateNote(Context context, Uri noteUri, ContentValues noteValues) {
        AsyncQueryHandler queryHandler = new AsyncQueryHandler(context.getApplicationContext().getContentResolver()) {};
        queryHandler.startUpdate(0, null, noteUri, noteValues, null, null);
    }

}

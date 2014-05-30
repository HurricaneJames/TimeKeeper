package com.easytimelog.timekeeper.data;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.easytimelog.timekeeper.util.DatabaseHelper;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TimeKeeperContentProvider extends ContentProvider {
    private static final UriMatcher URI_MATCHER;
    private static final int PROJECT_LIST     = 1;
    private static final int PROJECT_ID       = 2;
    private static final int TIME_RECORD_LIST = 3;
    private static final int TIME_RECORD_ID   = 4;
    private static final int NOTE_LIST        = 5;
    private static final int NOTE_ID          = 6;
    private static final int NOTE_COUNTS      = 7;
    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(TimeKeeperContract.AUTHORITY, "projects",       PROJECT_LIST);
        URI_MATCHER.addURI(TimeKeeperContract.AUTHORITY, "projects/#",     PROJECT_ID);
        URI_MATCHER.addURI(TimeKeeperContract.AUTHORITY, "time_records",   TIME_RECORD_LIST);
        URI_MATCHER.addURI(TimeKeeperContract.AUTHORITY, "time_records/#", TIME_RECORD_ID);
        URI_MATCHER.addURI(TimeKeeperContract.AUTHORITY, "notes",          NOTE_LIST);
        URI_MATCHER.addURI(TimeKeeperContract.AUTHORITY, "notes/#",        NOTE_ID);
        URI_MATCHER.addURI(TimeKeeperContract.AUTHORITY, "notes/counts/#", NOTE_COUNTS);
    }

    private TimeKeeperSqlOpenHelper mOpenHelper;
    private ThreadLocal<Boolean> mBatchProcessing = new ThreadLocal<Boolean>();

    @Override
    public boolean onCreate() {
        mOpenHelper = new TimeKeeperSqlOpenHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        ContentUris.withAppendedId(uri, 1l);
        switch (URI_MATCHER.match(uri)) {
            case PROJECT_LIST:
                return TimeKeeperContract.Projects.CONTENT_TYPE;
            case PROJECT_ID:
                return TimeKeeperContract.Projects.CONTENT_ITEM_TYPE;
            case TIME_RECORD_LIST:
                return TimeKeeperContract.TimeRecords.CONTENT_TYPE;
            case TIME_RECORD_ID:
                return TimeKeeperContract.TimeRecords.CONTENT_ITEM_TYPE;
            case NOTE_LIST:
                return TimeKeeperContract.Notes.CONTENT_TYPE;
            case NOTE_ID:
                return TimeKeeperContract.Notes.CONTENT_ITEM_TYPE;
            case NOTE_COUNTS:
                return TimeKeeperContract.Notes.CONTENT_TYPE + ".count";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String tableName;
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        switch (URI_MATCHER.match(uri)) {
            case PROJECT_LIST:
                builder.setTables(TimeKeeperContract.Projects.TABLE_NAME);
                if(TextUtils.isEmpty(sortOrder)) { sortOrder = TimeKeeperContract.Projects.DEFAULT_SORT_ORDER; }
                break;
            case PROJECT_ID:
                builder.setTables(TimeKeeperContract.Projects.TABLE_NAME);
                builder.appendWhere(TimeKeeperContract.Projects._ID + " = " + uri.getLastPathSegment());
                break;
            case TIME_RECORD_LIST:
                tableName = getTimeRecordTableName(projection);
                builder.setTables(tableName);
                if(TextUtils.isEmpty(sortOrder)) { sortOrder = TimeKeeperContract.TimeRecords.DEFAULT_SORT_ORDER; }
                break;
            case TIME_RECORD_ID:
                tableName = getTimeRecordTableName(projection);
                builder.setTables(tableName);
                builder.appendWhere(TimeKeeperContract.TimeRecords._ID + " = " + uri.getLastPathSegment());
                break;
            case NOTE_LIST:
                builder.setTables(TimeKeeperContract.Notes.TABLE_NAME);
                if(TextUtils.isEmpty(sortOrder)) { sortOrder = TimeKeeperContract.Notes.DEFAULT_SORT_ORDER; }
                break;
            case NOTE_ID:
                builder.setTables(TimeKeeperContract.Notes.TABLE_NAME);
                builder.appendWhere(TimeKeeperContract.Notes._ID + " = " + uri.getLastPathSegment());
                break;
            case NOTE_COUNTS:
                builder.setTables(TimeKeeperContract.Notes.TABLE_NAME);
                String projectId = getProjectIdForTimeRecord(uri.getLastPathSegment());
                String[] countProjection = new String[] { TimeKeeperContract.Notes.NOTE_TYPE, "count(" + TimeKeeperContract.Notes.NOTE_TYPE + ") as " + TimeKeeperContract.Notes.NOTE_TYPE + "_count"};
                String   countSelection = TimeKeeperContract.Notes.TIME_RECORD_ID + " in (select " + TimeKeeperContract.TimeRecords.TABLE_NAME + '.' + TimeKeeperContract.TimeRecords._ID + " from " + TimeKeeperContract.TimeRecords.TABLE_NAME + " where " + TimeKeeperContract.TimeRecords.TABLE_NAME + '.' + TimeKeeperContract.TimeRecords.PROJECT_ID + " = " + projectId + ")";
                Cursor countCursor = builder.query(db, countProjection, countSelection, selectionArgs, TimeKeeperContract.Notes.NOTE_TYPE, null, null);
                countCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return countCursor;
            default:
                throw new IllegalArgumentException("Unsupported URI for query: " + uri);
        }
        Cursor cursor = builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    private String getTimeRecordTableName(String[] projection) {
        // most of the time this will be the case
        if(projection == TimeKeeperContract.TimeRecords.PROJECTION_ALL_WITH_PROJECTS) { return TimeKeeperContract.TimeRecords.TIME_RECORDS_WITH_PROJECTS; }
        // check all the columns to see if we need to join the projects table
        for(String columnName:projection) {
            if(columnName.contains(TimeKeeperContract.Projects.TABLE_NAME + '.')) {
                return TimeKeeperContract.TimeRecords.TIME_RECORDS_WITH_PROJECTS;
            }
        }
        // just need the time_records table
        return TimeKeeperContract.TimeRecords.TABLE_NAME;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        Uri retUri;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        verifyDefaultColumns(contentValues, false);
        switch (URI_MATCHER.match(uri)) {
            case PROJECT_LIST:
                return getUriForId(uri, db.insert(TimeKeeperContract.Projects.TABLE_NAME, null, contentValues));
            case TIME_RECORD_LIST:
                verifyTimeRecordColumns(contentValues);
                retUri = getUriForId(uri, db.insert(TimeKeeperContract.TimeRecords.TABLE_NAME, null, contentValues));
                updateProjectTimeRecordCache(contentValues.getAsString(TimeKeeperContract.TimeRecords.PROJECT_ID));
                return retUri;
            case NOTE_LIST:
                retUri = getUriForId(uri, db.insert(TimeKeeperContract.Notes.TABLE_NAME, null, contentValues));
                updateProjectNoteCacheFromTimeRecord(contentValues.getAsString(TimeKeeperContract.Notes.TIME_RECORD_ID));
                return retUri;
            default:
                throw new IllegalArgumentException("Unsupported URI for insertion: " + uri);
        }
    }

    private Uri getUriForId(Uri uri, long id) {
        if(id > 0) {
            Uri newUri = ContentUris.withAppendedId(uri, id);
            if(!isBatchProcessing()) {
                getContext().getContentResolver().notifyChange(newUri, null);
            }
            return newUri;
        }
        throw new SQLException("Error inserting into uri: " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int deletedCount = 0,
            uriMatch = URI_MATCHER.match(uri),
            id = (uriMatch == PROJECT_ID || uriMatch == TIME_RECORD_ID || uriMatch == NOTE_ID) ? id = Integer.parseInt(uri.getLastPathSegment()) : -1;
        String where = getWhere(id, selection);
        Set<String> projectIds;

        switch(uriMatch) {
            case PROJECT_ID:
            case PROJECT_LIST:
                deletedCount = db.delete(TimeKeeperContract.Projects.TABLE_NAME, where, selectionArgs);
                break;
            case TIME_RECORD_ID:
            case TIME_RECORD_LIST:
                projectIds = getProjectIdsForTimeRecords(uri, selection, selectionArgs);
                deletedCount = db.delete(TimeKeeperContract.TimeRecords.TABLE_NAME, where, selectionArgs);
                if(deletedCount > 0) {
                    updateProjectTimeRecordCache(projectIds);
                    updateProjectNoteCache(projectIds);
                }
                break;
            case NOTE_ID:
            case NOTE_LIST:
                deletedCount = db.delete(TimeKeeperContract.Notes.TABLE_NAME, where, selectionArgs);
                if(deletedCount > 0) {
                    projectIds = getProjectIdsForNotes(uri, selection, selectionArgs);
                    updateProjectNoteCache(projectIds);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI for delete: " + uri);
        }
        if(deletedCount > 0 && !isBatchProcessing()) { getContext().getContentResolver().notifyChange(uri, null); }
        return deletedCount;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int updateCount = 0,
            uriMatch = URI_MATCHER.match(uri),
            id = (uriMatch == PROJECT_ID || uriMatch == TIME_RECORD_ID || uriMatch == NOTE_ID) ? id = Integer.parseInt(uri.getLastPathSegment()) : -1;
        String where = getWhere(id, selection);
        verifyDefaultColumns(values, true);
        switch(uriMatch) {
            case PROJECT_ID:
            case PROJECT_LIST:
                updateCount = db.update(TimeKeeperContract.Projects.TABLE_NAME, values, where, selectionArgs);
                break;
            case TIME_RECORD_ID:
            case TIME_RECORD_LIST:
                verifyTimeRecordColumns(values);
                Set<String> projectIds = getProjectIdsForTimeRecords(uri, selection, selectionArgs);
                updateCount = db.update(TimeKeeperContract.TimeRecords.TABLE_NAME, values, where, selectionArgs);
                if(updateCount > 0) {
                    // check for any updates (in case the change was moving to a different project)
                    getProjectIdsForTimeRecords(uri, selection, selectionArgs, projectIds);
                    updateProjectTimeRecordCache(projectIds);
                }
                break;
            case NOTE_ID:
            case NOTE_LIST:
                updateCount = db.update(TimeKeeperContract.Notes.TABLE_NAME, values, where, selectionArgs);
                if(updateCount > 0) {
                    projectIds = getProjectIdsForNotes(uri, selection, selectionArgs);
                    updateProjectNoteCache(projectIds);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI for update: " + uri);
        }
        if(updateCount > 0 && !isBatchProcessing()) { getContext().getContentResolver().notifyChange(uri, null); }
        return updateCount;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        mBatchProcessing.set(true);
        db.beginTransaction();
        try {
            final ContentProviderResult[] result = super.applyBatch(operations);
            db.setTransactionSuccessful();
            getContext().getContentResolver().notifyChange(TimeKeeperContract.CONTENT_URI, null);
            return result;
        }finally {
            mBatchProcessing.remove();
            db.endTransaction();
        }
    }

    /**
     * Update a contentValues to include defaults for all required common columns.
     * @param contentValues
     * @param overwrite - true will overwrite all columns while allow overwriting with defaults (ex. updated_ad)
     */
    private void verifyDefaultColumns(ContentValues contentValues, boolean overwrite) {
        DateTime currentTime = new DateTime(DateTimeZone.UTC);
        if(!contentValues.containsKey(TimeKeeperContract.CommonColumns.CREATED_AT)) {
            contentValues.put(TimeKeeperContract.CommonColumns.CREATED_AT, currentTime.toString());
        } else {
            unifyDatetimeContentValue(contentValues, TimeKeeperContract.CommonColumns.CREATED_AT);
        }
        if(overwrite || !contentValues.containsKey(TimeKeeperContract.CommonColumns.UPDATED_AT)) {
            contentValues.put(TimeKeeperContract.CommonColumns.UPDATED_AT, currentTime.toString());
        } else {
            unifyDatetimeContentValue(contentValues, TimeKeeperContract.CommonColumns.UPDATED_AT);
        }
    }

    private void verifyTimeRecordColumns(ContentValues contentValues) {
        unifyDatetimeContentValue(contentValues, TimeKeeperContract.TimeRecords.START_AT);
        unifyDatetimeContentValue(contentValues, TimeKeeperContract.TimeRecords.END_AT);
    }

    private static void unifyDatetimeContentValue(ContentValues contentValues, String columnName) {
        if(contentValues.containsKey(columnName)) {
            contentValues.put(columnName, TimeKeeperContract.getStandardDateString(contentValues.get(columnName)));
        }
    }

    private boolean isBatchProcessing() {
        return mBatchProcessing.get() != null && mBatchProcessing.get();
    }

    private String getWhere(int id, String selection) {
        String where = selection;
        if(id >= 0) {
            String idString = Integer.toString(id);
            where = TimeKeeperContract.CommonColumns._ID + " = " + idString;
            if (!TextUtils.isEmpty(selection)) { where += " AND " + selection; }
        }
        return where;
    }

    private void updateProjectTimeRecordCache(Set<String> projectIds) {
        for(String projectId:projectIds) {
            updateProjectTimeRecordCache(projectId);
        }
    }
    private void updateProjectTimeRecordCache(String projectId) {
        boolean running = false;
        int runningTimeRecordId = -1;
        DateTime projectStartAt = null;
        Cursor timeRecordsCursor = getContext().getContentResolver().query(TimeKeeperContract.TimeRecords.CONTENT_URI, TimeKeeperContract.TimeRecords.PROJECTION_ALL, TimeKeeperContract.TimeRecords.PROJECT_ID + " = " + projectId, null, TimeKeeperContract.TimeRecords.START_AT + " ASC");
        if(!timeRecordsCursor.moveToFirst()) { return; }

        int idColumnIndex = timeRecordsCursor.getColumnIndex(TimeKeeperContract.TimeRecords._ID);
        int startAtColumnIndex = timeRecordsCursor.getColumnIndex(TimeKeeperContract.TimeRecords.START_AT);
        int endAtColumnIndex = timeRecordsCursor.getColumnIndex(TimeKeeperContract.TimeRecords.END_AT);
        DateTime startAt = new DateTime(timeRecordsCursor.getString(startAtColumnIndex));
        String endAtValue = timeRecordsCursor.getString(endAtColumnIndex);
        DateTime endAt;
        if(endAtValue == null) {
            running = true;
            projectStartAt = startAt;
            endAt = startAt;
            runningTimeRecordId = timeRecordsCursor.getInt(idColumnIndex);
        }else {
            endAt = new DateTime(endAtValue);
        }

        Duration totalDuration = new Duration(startAt, endAt);
        DateTime maxEnd = endAt;
        while(timeRecordsCursor.moveToNext()) {
            startAt = new DateTime(timeRecordsCursor.getString(startAtColumnIndex));
            endAtValue = timeRecordsCursor.getString(endAtColumnIndex);
            if(endAtValue == null) {
                running = true;
                projectStartAt = startAt;
                endAt = startAt;
                runningTimeRecordId = timeRecordsCursor.getInt(idColumnIndex);
            }else {
                endAt = new DateTime(endAtValue);
            }
            Duration duration = new Duration(startAt, endAt);
            Duration deltaEnds = new Duration(maxEnd, endAt);
            if(deltaEnds.compareTo(Duration.ZERO) >= 0) {
                if (duration.compareTo(deltaEnds) >= 0) {
                    totalDuration = totalDuration.plus(deltaEnds);
                } else {
                    totalDuration = totalDuration.plus(duration);
                    maxEnd = endAt;
                }
            }
        }
        timeRecordsCursor.close();
        ContentValues values = new ContentValues();
        values.put(TimeKeeperContract.Projects.DURATION, totalDuration.getMillis());
        if(running) {
            values.put(TimeKeeperContract.Projects.RUNNING, true);
            values.put(TimeKeeperContract.Projects.RUNNING_TIME_RECORD, runningTimeRecordId);
            values.put(TimeKeeperContract.Projects.STARTED_AT, TimeKeeperContract.getStandardDateString(projectStartAt));
        }else {
            values.put(TimeKeeperContract.Projects.RUNNING, false);
            values.putNull(TimeKeeperContract.Projects.RUNNING_TIME_RECORD);
            values.putNull(TimeKeeperContract.Projects.STARTED_AT);
        }
        getContext().getContentResolver().update(ContentUris.withAppendedId(TimeKeeperContract.Projects.CONTENT_URI, Long.parseLong(projectId)), values, null, null);
    }

    private void updateProjectNoteCacheFromTimeRecord(Set<String> timeRecordIds) { for(String projectId:timeRecordIds) { updateProjectNoteCacheFromTimeRecord(timeRecordIds); } }
    private void updateProjectNoteCacheFromTimeRecord(String timeRecordId) {
        updateProjectNoteCache(getProjectIdForTimeRecord(timeRecordId));
    }

    private void updateProjectNoteCache(Set<String> projectIds) { for(String projectId:projectIds) { updateProjectNoteCache(projectId); } }
    private void updateProjectNoteCache(String projectId) {
        ContentResolver resolver = getContext().getContentResolver();
        long lProjectId = Long.parseLong(projectId);
        Cursor countCursor = resolver.query( ContentUris.withAppendedId(Uri.withAppendedPath(TimeKeeperContract.Notes.CONTENT_URI, "counts"), lProjectId), null, null, null, null);
        ContentValues values = new ContentValues();
            // BLANK CACHE
            values.put(TimeKeeperContract.Projects.TEXT_NOTE_COUNT, 0);
            values.put(TimeKeeperContract.Projects.LIST_NOTE_COUNT, 0);
            values.put(TimeKeeperContract.Projects.CAMERA_NOTE_COUNT, 0);
            values.put(TimeKeeperContract.Projects.AUDIO_NOTE_COUNT, 0);
        if(countCursor.moveToFirst()) {
            int typeIndex = countCursor.getColumnIndex(TimeKeeperContract.Notes.NOTE_TYPE);
            int countIndex = countCursor.getColumnIndex(TimeKeeperContract.Notes.NOTE_TYPE + "_count");
            do {
                values.put(countCursor.getString(typeIndex) + "_note_count", countCursor.getString(countIndex));
            } while (countCursor.moveToNext());
        }
        countCursor.close();
        resolver.update(ContentUris.withAppendedId(TimeKeeperContract.Projects.CONTENT_URI, lProjectId), values, null, null);
    }

    private Set<String> getProjectIdsForTimeRecords(Uri contentUri, String selection, String[] selectionArgs, Set<String> projectIds) {
        return getIds(contentUri, TimeKeeperContract.TimeRecords.PROJECT_ID, selection, selectionArgs, projectIds);
    }
    private Set<String> getProjectIdsForTimeRecords(Uri contentUri, String selection, String[] selectionArgs) {
        HashSet<String> projectIds = new HashSet<String>();
        return getProjectIdsForTimeRecords(contentUri, selection, selectionArgs, projectIds);
    }

    private Set<String> getProjectIdsForNotes(Uri contentUri, String selection, String[] selectionArgs, Set<String> projectIds) {
        Set<String> timeRecordIds = getIds(contentUri, TimeKeeperContract.Notes.TIME_RECORD_ID, selection, selectionArgs, projectIds);
        return getProjectIdsForTimeRecords(TimeKeeperContract.TimeRecords.CONTENT_URI, null, null, projectIds);
    }
    private Set<String> getProjectIdsForNotes(Uri contentUri, String selection, String[] selectionArgs) {
        HashSet<String> projectIds = new HashSet<String>();
        return getProjectIdsForNotes(contentUri, selection, selectionArgs, projectIds);
    }

    private Set<String> getIds(Uri contentUri, String idColumnName, String selection, String[] selectionArgs) {
        HashSet<String> idSet = new HashSet<String>();
        return getIds(contentUri, idColumnName, selection, selectionArgs, idSet);
    }
    private Set<String> getIds(Uri contentUri, String idColumnName, String selection, String[] selectionArgs, Set<String> idSet) {
        Cursor cursor = getContext().getContentResolver().query(contentUri, new String[] { idColumnName }, selection, selectionArgs, null);
        DatabaseHelper.getIdsFromCursor(cursor, cursor.getColumnIndex(idColumnName), idSet);
        cursor.close();
        return idSet;
    }

    private String getProjectIdForTimeRecord(String timeRecordId) {
        Cursor cursor = getContext().getContentResolver().query(ContentUris.withAppendedId(TimeKeeperContract.TimeRecords.CONTENT_URI, Long.parseLong(timeRecordId)), new String[] {TimeKeeperContract.TimeRecords.PROJECT_ID }, null, null, null);
        if(!cursor.moveToFirst()) { return null; }
        String projectId = cursor.getString(cursor.getColumnIndex(TimeKeeperContract.TimeRecords.PROJECT_ID));
        cursor.close();
        return projectId;
    }

    private Set<String> getAllProjectIds() {
        Cursor cursor = getContext().getContentResolver().query(TimeKeeperContract.Projects.CONTENT_URI, new String[] { TimeKeeperContract.Projects._ID }, null, null, null);
        return DatabaseHelper.getIdsFromCursor(cursor, cursor.getColumnIndex(TimeKeeperContract.Projects._ID));
    }
}

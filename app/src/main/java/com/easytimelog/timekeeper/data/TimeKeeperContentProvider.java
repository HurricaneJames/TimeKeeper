package com.easytimelog.timekeeper.data;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
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
    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(TimeKeeperContract.AUTHORITY, "projects",       PROJECT_LIST);
        URI_MATCHER.addURI(TimeKeeperContract.AUTHORITY, "projects/#",     PROJECT_ID);
        URI_MATCHER.addURI(TimeKeeperContract.AUTHORITY, "time_records",   TIME_RECORD_LIST);
        URI_MATCHER.addURI(TimeKeeperContract.AUTHORITY, "time_records/#", TIME_RECORD_ID);
        URI_MATCHER.addURI(TimeKeeperContract.AUTHORITY, "notes",          NOTE_LIST);
        URI_MATCHER.addURI(TimeKeeperContract.AUTHORITY, "notes/#",        NOTE_ID);
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
                updateProjectCache((contentValues.getAsInteger(TimeKeeperContract.TimeRecords.PROJECT_ID)));
                return retUri;
            case NOTE_LIST:
                return getUriForId(uri, db.insert(TimeKeeperContract.Notes.TABLE_NAME, null, contentValues));
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
        // TODO - update insert/delete/update for time records to update project cache... w/ corresponding no_cache contentValues... add some kind of way to notify that caching is a good thing
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int deletedCount = 0,
            uriMatch = URI_MATCHER.match(uri),
            id = (uriMatch == PROJECT_ID || uriMatch == TIME_RECORD_ID || uriMatch == NOTE_ID) ? id = Integer.parseInt(uri.getLastPathSegment()) : -1;
        String where = getWhere(id, selection);

        switch(uriMatch) {
            case PROJECT_ID:
            case PROJECT_LIST:
                deletedCount = db.delete(TimeKeeperContract.Projects.TABLE_NAME, where, selectionArgs);
                break;
            case TIME_RECORD_ID:
            case TIME_RECORD_LIST:
                Set<Integer> projectIds = getProjectIdsForTimeRecords(uri, selection, selectionArgs);
                deletedCount = db.delete(TimeKeeperContract.TimeRecords.TABLE_NAME, where, selectionArgs);
                if(deletedCount > 0) {
                    updateProjectCache(projectIds);
                }
                break;
            case NOTE_ID:
            case NOTE_LIST:
                deletedCount = db.delete(TimeKeeperContract.Notes.TABLE_NAME, where, selectionArgs);
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
                Set<Integer> projectIds = getProjectIdsForTimeRecords(uri, selection, selectionArgs);
                updateCount = db.update(TimeKeeperContract.TimeRecords.TABLE_NAME, values, where, selectionArgs);
                if(updateCount > 0) {
                    // check for any updates (in case the change was moving to a different project)
                    getProjectIdsForTimeRecords(uri, selection, selectionArgs, projectIds);
                    updateProjectCache(projectIds);
                }
                break;
            case NOTE_ID:
            case NOTE_LIST:
                updateCount = db.update(TimeKeeperContract.Notes.TABLE_NAME, values, where, selectionArgs);
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

    private void unifyDatetimeContentValue(ContentValues contentValues, String columnName) {
        if(contentValues.containsKey(columnName)) {
            contentValues.put(columnName, fixDate(contentValues.get(columnName)));
        }
    }

    private String fixDate(Object timestamp) {
        if(timestamp instanceof Long)     { return getStringForDateTime(new DateTime(timestamp)); }
        if(timestamp instanceof DateTime) { return getStringForDateTime((DateTime)timestamp); }
        return getStringForDateTime(DateTime.parse(timestamp.toString()));
    }

    private String getStringForDateTime(DateTime datetime) {
        return datetime.withZone(DateTimeZone.UTC).toString();
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

    private void updateProjectCache(Set<Integer> projectIds) {
        for(int projectId:projectIds) {
            updateProjectCache(projectId);
        }
    }
    private void updateProjectCache(int projectId) {
        Cursor timeRecordsCursor = getContext().getContentResolver().query(TimeKeeperContract.TimeRecords.CONTENT_URI, TimeKeeperContract.TimeRecords.PROJECTION_ALL, TimeKeeperContract.TimeRecords.PROJECT_ID + " = " + projectId, null, TimeKeeperContract.TimeRecords.START_AT + " ASC");
        if(!timeRecordsCursor.moveToFirst()) { return; }

        int startAtColumnIndex = timeRecordsCursor.getColumnIndex(TimeKeeperContract.TimeRecords.START_AT);
        int endAtColumnIndex = timeRecordsCursor.getColumnIndex(TimeKeeperContract.TimeRecords.END_AT);
        DateTime startAt = new DateTime(timeRecordsCursor.getString(startAtColumnIndex));
        DateTime endAt   = new DateTime(timeRecordsCursor.getString(endAtColumnIndex));
        Duration totalDuration = new Duration(startAt, endAt);
        DateTime maxEnd = endAt;
        while(timeRecordsCursor.moveToNext()) {
            startAt = new DateTime(timeRecordsCursor.getString(startAtColumnIndex));
            endAt   = new DateTime(timeRecordsCursor.getString(endAtColumnIndex));
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
        getContext().getContentResolver().update(ContentUris.withAppendedId(TimeKeeperContract.Projects.CONTENT_URI, 1), values, null, null);
    }

    private static final String[] TIME_RECORD_PROJECT_ID = { TimeKeeperContract.TimeRecords.PROJECT_ID };
    private Set<Integer> getProjectIdsForTimeRecords(Uri contentUri, String selection, String[] selectionArgs, Set<Integer> projectIds) {
        Cursor cursor = getContext().getContentResolver().query(contentUri, TIME_RECORD_PROJECT_ID, selection, selectionArgs, null);
        if(cursor.moveToFirst()) { return projectIds; }
        int columnIndex = cursor.getColumnIndex(TimeKeeperContract.TimeRecords.PROJECT_ID);
        do {
            projectIds.add(cursor.getInt(columnIndex));
        }while(cursor.moveToNext());
        cursor.close();
        return projectIds;
    }
    private Set<Integer> getProjectIdsForTimeRecords(Uri contentUri, String selection, String[] selectionArgs) {
        HashSet<Integer> projectIds = new HashSet<Integer>();
        return getProjectIdsForTimeRecords(contentUri, selection, selectionArgs, projectIds);
    }
}

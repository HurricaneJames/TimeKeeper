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
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;

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
                builder.setTables(TimeKeeperContract.TimeRecords.TIME_RECORDS_WITH_PROJECTS);
                if(TextUtils.isEmpty(sortOrder)) { sortOrder = TimeKeeperContract.TimeRecords.DEFAULT_SORT_ORDER; }
                break;
            case TIME_RECORD_ID:
                builder.setTables(TimeKeeperContract.TimeRecords.TIME_RECORDS_WITH_PROJECTS);
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

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        updateDefaultColumns(contentValues, false);
        switch (URI_MATCHER.match(uri)) {
            case PROJECT_LIST:
                return getUriForId(uri, db.insert(TimeKeeperContract.Projects.TABLE_NAME, null, contentValues));
            case TIME_RECORD_LIST:
                verifyTimeRecordColumns(contentValues);
                return getUriForId(uri, db.insert(TimeKeeperContract.TimeRecords.TABLE_NAME, null, contentValues));
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
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int deletedCount = 0,
            uriMatch = URI_MATCHER.match(uri);
        String where = getWhere(uri, uriMatch, selection);
        switch(uriMatch) {
            case PROJECT_ID:
            case PROJECT_LIST:
                deletedCount = db.delete(TimeKeeperContract.Projects.TABLE_NAME, where, selectionArgs);
                break;
            case TIME_RECORD_ID:
            case TIME_RECORD_LIST:
                deletedCount = db.delete(TimeKeeperContract.TimeRecords.TABLE_NAME, where, selectionArgs);
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
            uriMatch = URI_MATCHER.match(uri);
        String where = getWhere(uri, uriMatch, selection);
        updateDefaultColumns(values, true);
        switch(uriMatch) {
            case PROJECT_ID:
            case PROJECT_LIST:
                updateCount = db.update(TimeKeeperContract.Projects.TABLE_NAME, values, where, selectionArgs);
                break;
            case TIME_RECORD_ID:
            case TIME_RECORD_LIST:
                verifyTimeRecordColumns(values);
                updateCount = db.update(TimeKeeperContract.TimeRecords.TABLE_NAME, values, where, selectionArgs);
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
    private void updateDefaultColumns(ContentValues contentValues, boolean overwrite) {
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

    private String getWhere(Uri uri, int uriType, String selection) {
        String where = selection;
        if(uriType == PROJECT_ID || uriType == TIME_RECORD_ID || uriType == NOTE_ID) {
            String idString = uri.getLastPathSegment();
            where = TimeKeeperContract.CommonColumns._ID + " = " + idString;
            if (!TextUtils.isEmpty(selection)) { where += " AND " + selection; }
        }
        return where;
    }

}

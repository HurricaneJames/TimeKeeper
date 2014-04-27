package com.easytimelog.timekeeper.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class TimeKeeperContract {
    public static final String AUTHORITY = "com.easytimelog.timekeeper.contract";
    public static final Uri CONTENT_URI  = Uri.parse("content://" + AUTHORITY);

    public static final class Projects implements CommonColumns {
        public static final String TABLE_NAME = "projects";
        private static final String _mimeType  = "/" + TimeKeeperContract.AUTHORITY + "." + TABLE_NAME;

        public static final Uri CONTENT_URI = Uri.withAppendedPath(TimeKeeperContract.CONTENT_URI, TABLE_NAME);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + _mimeType;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + _mimeType;

        public static final String NAME = "name";
        public static final String[] PROJECTION_ALL = { _ID, GLOBAL_ID, NAME, CREATED_AT, UPDATED_AT };
        public static final String DEFAULT_SORT_ORDER = UPDATED_AT + " ASC";
    }
    public static final class TimeRecords implements CommonColumns {
        public static final String TABLE_NAME = "time_records";
        private static final String _mimeType = "/" + TimeKeeperContract.AUTHORITY + "." + TABLE_NAME;

        public static final Uri CONTENT_URI = Uri.withAppendedPath(TimeKeeperContract.CONTENT_URI, TABLE_NAME);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + _mimeType;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + _mimeType;

        public static final String START_AT   = "start_at";
        public static final String END_AT     = "end_at";
        public static final String PROJECT_ID = "project_id";
        public static final String[] PROJECTION_ALL = { _ID, GLOBAL_ID, START_AT, END_AT, PROJECT_ID, CREATED_AT, UPDATED_AT };
        public static final String DEFAULT_SORT_ORDER = START_AT + " ASC";
    }
    public static final class Notes implements CommonColumns {
        public static final String TABLE_NAME = "notes";
        private static final String _mimeType = "/" + TimeKeeperContract.AUTHORITY + "." + TABLE_NAME;

        public static final Uri CONTENT_URI = Uri.withAppendedPath(TimeKeeperContract.CONTENT_URI, TABLE_NAME);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + _mimeType;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + _mimeType;

        public static final String SCRIBBLE       = "scribble";
        public static final String LINK           = "link";
        public static final String TIME_RECORD_ID = "time_record_id";
        public static final String[] PROJECTION_ALL = { _ID, GLOBAL_ID, SCRIBBLE, LINK, TIME_RECORD_ID, CREATED_AT, UPDATED_AT };
        public static final String DEFAULT_SORT_ORDER = _ID + " DESC";
    }

    public static interface CommonColumns extends BaseColumns {
        public static final String GLOBAL_ID  = "global_id";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";
    }

}

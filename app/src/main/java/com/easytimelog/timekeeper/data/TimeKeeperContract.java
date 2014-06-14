package com.easytimelog.timekeeper.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

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
        public static final String DURATION = "duration";
        public static final String RUNNING = "currently_running";
        public static final String RUNNING_TIME_RECORD = "running_time_record_id";
        public static final String STARTED_AT = "cached_start_at";

        public static final String TEXT_NOTE_COUNT   = "text_note_count";
        public static final String LIST_NOTE_COUNT   = "list_note_count";
        public static final String CAMERA_NOTE_COUNT = "camera_note_count";
        public static final String AUDIO_NOTE_COUNT  = "audio_note_count";

        public static final String[] PROJECTION_ALL = { _ID, GLOBAL_ID, NAME, DURATION, RUNNING, RUNNING_TIME_RECORD, STARTED_AT, TEXT_NOTE_COUNT, LIST_NOTE_COUNT, CAMERA_NOTE_COUNT, AUDIO_NOTE_COUNT, CREATED_AT, UPDATED_AT };
//        public static final String DEFAULT_SORT_ORDER = UPDATED_AT + " DESC";
        public static final String DEFAULT_SORT_ORDER = _ID + " DESC";
    }
    public static final class TimeRecords implements CommonColumns {
        public static final String TABLE_NAME = "time_records";
        private static final String _mimeType = "/" + TimeKeeperContract.AUTHORITY + "." + TABLE_NAME;

        public static final Uri CONTENT_URI = Uri.withAppendedPath(TimeKeeperContract.CONTENT_URI, TABLE_NAME);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + _mimeType;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + _mimeType;

        public static final String START_AT     = "start_at";
        public static final String END_AT       = "end_at";
        public static final String PROJECT_ID   = "project_id";
        public static final String NOTE_COUNT   = "note_count";
        public static final String NOTE_COUNT_SELECTION = "(select count(*) from " + Notes.TABLE_NAME + " where " + Notes.TABLE_NAME + '.' + Notes.TIME_RECORD_ID + " = " + TABLE_NAME + '.' + _ID + ") as " + NOTE_COUNT;
        public static final String[] PROJECTION_ALL = { _ID, GLOBAL_ID, START_AT, END_AT, PROJECT_ID, CREATED_AT, UPDATED_AT, NOTE_COUNT_SELECTION };

        public static final String DEFAULT_SORT_ORDER = START_AT + " DESC";

        public static final String whereProjectId(String projectId) {
            return TABLE_NAME + "." + PROJECT_ID + "=" + projectId;
        }
    }
    public static final class Notes implements CommonColumns {
        public static final String TABLE_NAME = "notes";
        private static final String _mimeType = "/" + TimeKeeperContract.AUTHORITY + "." + TABLE_NAME;

        public static final Uri CONTENT_URI = Uri.withAppendedPath(TimeKeeperContract.CONTENT_URI, TABLE_NAME);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + _mimeType;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + _mimeType;

        public static final String SCRIBBLE       = "scribble";
        public static final String NOTE_TYPE      = "content_type";
        public static final String LINK           = "link";
        public static final String TIME_RECORD_ID = "time_record_id";
        public static final String[] PROJECTION_ALL = { _ID, GLOBAL_ID, SCRIBBLE, NOTE_TYPE, LINK, TIME_RECORD_ID, CREATED_AT, UPDATED_AT };
        public static final String DEFAULT_SORT_ORDER = _ID + " DESC";

        public static final String whereTimeRecordId(int timeRecordId) {
            return TABLE_NAME + '.' + TIME_RECORD_ID + "=" + timeRecordId;
        }

        public static final String TEXT_NOTE = "text";
        public static final String LIST_NOTE = "list";
        public static final String CAMERA_NOTE = "camera";
        public static final String AUDIO_NOTE = "audio";
    }

    public static interface CommonColumns extends BaseColumns {
        public static final String GLOBAL_ID  = "global_id";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";
    }

    public static String getStandardDateString(Object timestamp) {
        if(timestamp instanceof Long)     { return getStringForDateTime(new DateTime(timestamp)); }
        if(timestamp instanceof DateTime) { return getStringForDateTime((DateTime)timestamp); }
        return getStringForDateTime(DateTime.parse(timestamp.toString()));
    }

    public static String getStringForDateTime(DateTime datetime) {
        return datetime.withZone(DateTimeZone.UTC).toString();
    }
}

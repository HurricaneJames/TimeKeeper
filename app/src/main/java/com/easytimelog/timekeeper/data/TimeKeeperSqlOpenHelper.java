package com.easytimelog.timekeeper.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TimeKeeperSqlOpenHelper extends SQLiteOpenHelper {
    private static final String NAME = "timekeeper.db";
    private static final int VERSION = 1;

    private interface Migration {
        public String COMMON_COLUMNS = "_id integer primary key autoincrement, created_at datetime not null, updated_at datetime not null, global_id integer, ";
        // Throw exception to prevent up/down from happening. Most useful when downgrading is not possible.
        public String[] up();
        public String[] down();
    }
    private static final Migration[] MIGRATIONS = {
        new Migration() {
            public String[] up() {
                return new String[]{
                    "create table projects (" +     COMMON_COLUMNS + "name text not null, duration integer default 0, currently_running integer default 0, running_time_record_id integer, cached_start_at datetime);",
                    "create table time_records (" + COMMON_COLUMNS + "project_id integer not null, start_at datetime not null, end_at datetime, duration datetime);",
                    "create table notes (" +        COMMON_COLUMNS + "time_record_id integer not null, content_type text not null, content text);",
                    "create index index_notes_on_time_record_id on notes (time_record_id);",
                    "create index index_time_records_on_project_id_and_start_at on time_records (project_id, start_at);",
                    "create index index_time_records_on_start_at on time_records (start_at);",
                    "create index index_projects_on_updated_at on projects (updated_at);",
//                    "create index index_time_records_on_updated_at on time_records (updated_at);",
//                    "create index index_notes_on_updated_at on notes (updated_at);"
                };
            }
            public String[] down() {
                return new String[]{
                    "drop table notes;",
                    "drop table time_records;",
                    "drop table projects;"
                };
            }
        },
    };

    public TimeKeeperSqlOpenHelper(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            for(Migration migration: MIGRATIONS) {
                for(String step: migration.up()) {
                    db.execSQL(step);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // error handling done by SQLiteOpenHelper
        for(int i=oldVersion; i<newVersion; i++) {
            String[] upgradeSteps = MIGRATIONS[i-1].up();
            for(String step: upgradeSteps) {
                db.execSQL(step);
            }
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // error handling done by SQLiteOpenHelper
        for(int i=oldVersion; i>newVersion; i--) {
            String[] upgradeSteps = MIGRATIONS[i-1].down();
            for(String step: upgradeSteps) {
                db.execSQL(step);
            }
        }
    }
}

package com.example.slime;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActivityLogDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "slime_activity_log.db";
    private static final int DB_VERSION = 1;

    static final String TABLE = "activity_log";
    static final String COL_ID = "id";
    static final String COL_TIMESTAMP = "timestamp";
    static final String COL_SCREEN = "screen_name";
    static final String COL_USERNAME = "username";
    static final String COL_MANAGER = "manager_name";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE + " (" +
            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_TIMESTAMP + " TEXT NOT NULL, " +
            COL_SCREEN + " TEXT NOT NULL, " +
            COL_USERNAME + " TEXT, " +
            COL_MANAGER + " TEXT)";

    private static ActivityLogDbHelper instance;

    static synchronized ActivityLogDbHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new ActivityLogDbHelper(ctx.getApplicationContext());
        }
        return instance;
    }

    private ActivityLogDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    void insert(String screenName, String username, String managerName) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        ContentValues cv = new ContentValues();
        cv.put(COL_TIMESTAMP, timestamp);
        cv.put(COL_SCREEN, screenName);
        cv.put(COL_USERNAME, username);
        cv.put(COL_MANAGER, managerName);
        getWritableDatabase().insert(TABLE, null, cv);
    }

    List<ActivityLogEntry> queryAll() {
        List<ActivityLogEntry> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(
                TABLE, null, null, null, null, null,
                COL_ID + " DESC")) {
            int idxTs   = c.getColumnIndexOrThrow(COL_TIMESTAMP);
            int idxScr  = c.getColumnIndexOrThrow(COL_SCREEN);
            int idxUser = c.getColumnIndexOrThrow(COL_USERNAME);
            int idxMgr  = c.getColumnIndexOrThrow(COL_MANAGER);
            while (c.moveToNext()) {
                list.add(new ActivityLogEntry(
                        c.getString(idxTs),
                        c.getString(idxScr),
                        c.getString(idxUser),
                        c.getString(idxMgr)));
            }
        }
        return list;
    }

    static class ActivityLogEntry {
        final String timestamp;
        final String screenName;
        final String username;
        final String managerName;

        ActivityLogEntry(String timestamp, String screenName, String username, String managerName) {
            this.timestamp = timestamp;
            this.screenName = screenName;
            this.username = username;
            this.managerName = managerName;
        }
    }
}

package com.jakehilborn.speedr.heremaps.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CacheHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "HereCache.db";
    static final String TABLE_NAME = "tile_limits";
    static final String REF_ID = "ref_id";
    static final String SPEED_LIMIT = "speed_limit";
    static final String TILE = "tile";
    static final String INSERT_TIME = "insert_time";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
            REF_ID + " INTEGER PRIMARY KEY," +
            SPEED_LIMIT + " INTEGER," +
            TILE + " TEXT," +
            INSERT_TIME + " INTEGER)";
    private static final String SQL_CREATE_TILE_INDEX =
            "CREATE INDEX " + TILE + "_idx ON " + TABLE_NAME + "(" + TILE + ")";
    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TABLE_NAME;

    public CacheHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
        db.execSQL(SQL_CREATE_TILE_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}

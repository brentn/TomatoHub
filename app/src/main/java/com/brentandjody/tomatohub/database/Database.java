package com.brentandjody.tomatohub.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.brentandjody.tomatohub.database.DBContract.*;
/**
 * Created by brent on 28/11/15.
 */
public class Database extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "tomatohub.db";

    private static final String CREATE_DEVICES_TABLE =
            "CREATE TABLE " + DeviceEntry.TABLE_NAME + " (" +
                    DeviceEntry._ID + " INTEGER PRIMARY KEY," +
                    DeviceEntry.COLUMN_NAME + " TEXT," +
                    DeviceEntry.COLUMN_LAST_IP + " Text," +
                    DeviceEntry.COLUMN_MAC + ")";

    private static final String DROP_DEVICES_TABLE = "" +
            "DROP TABLE IF EXISTS " + DeviceEntry.TABLE_NAME;

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DEVICES_TABLE);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_DEVICES_TABLE);
        onCreate(db);
    }
}

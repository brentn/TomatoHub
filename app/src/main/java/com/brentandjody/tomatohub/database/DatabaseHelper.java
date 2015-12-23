package com.brentandjody.tomatohub.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.brentandjody.tomatohub.database.DBContract.*;
/**
 * Created by brent on 28/11/15.
 * Manages database creation and upgrades
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "tomatohub.db";

    private static final String CREATE_DEVICES_TABLE =
            "CREATE TABLE " + DeviceEntry.TABLE_NAME + " (" +
                    DeviceEntry._ID + " INTEGER PRIMARY KEY," +
                    DeviceEntry.COLUMN_ROUTER_ID + " TEXT," +
                    DeviceEntry.COLUMN_NAME + " TEXT," +
                    DeviceEntry.COLUMN_CUSTOM_NAME + " TEXT," +
                    DeviceEntry.COLUMN_MAC + " TEXT UNIQUE," +
                    DeviceEntry.COLUMN_NETWORK_ID + " TEXT," +
                    DeviceEntry.COLUMN_LAST_IP + " TEXT," +
                    DeviceEntry.COLUMN_ACTIVE + " INTEGER," +
                    DeviceEntry.COLUMN_TRAFFIC_TIMESTAMP + " INTEGER," +
                    DeviceEntry.COLUMN_TX_BYTES + " INTEGER," +
                    DeviceEntry.COLUMN_RX_BYTES + " INTEGER," +
                    DeviceEntry.COLUMN_LAST_SPEED + " REAL," +
                    DeviceEntry.COLUMN_BLOCKED + " INTEGER," +
                    DeviceEntry.COLUMN_PRIORITIZED + " INTEGER)";

    private static final String CREATE_NETWORKS_TABLE =
            "CREATE TABLE " + NetworkEntry.TABLE_NAME + " (" +
                    NetworkEntry._ID + " INTEGER PRIMARY KEY," +
                    NetworkEntry.COLUMN_ROUTER_ID + " TEXT," +
                    NetworkEntry.COLUMN_NETWORK_ID + " TEXT," +
                    NetworkEntry.COLUMN_CUSTOM_NAME + " TEXT," +
                    NetworkEntry.COLUMN_TRAFFIC_TIMESTAMP + " INTEGER," +
                    NetworkEntry.COLUMN_TX_BYTES + " INTEGER," +
                    NetworkEntry.COLUMN_RX_BYTES + " INTEGER," +
                    NetworkEntry.COLUMN_LAST_SPEED + " REAL,"+
                    " UNIQUE ("+NetworkEntry.COLUMN_ROUTER_ID+","+
                    NetworkEntry.COLUMN_NETWORK_ID+") ON CONFLICT REPLACE)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DEVICES_TABLE);
        db.execSQL(CREATE_NETWORKS_TABLE);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1: db.execSQL("ALTER TABLE "+DeviceEntry.TABLE_NAME+" ADD COLUMN " + DeviceEntry.COLUMN_BLOCKED + " INTEGER DEFAULT 0");
            case 2: db.execSQL("ALTER TABLE "+DeviceEntry.TABLE_NAME+" ADD COLUMN " + DeviceEntry.COLUMN_PRIORITIZED + " INTEGER DEFAULT 0");
        }
    }
}

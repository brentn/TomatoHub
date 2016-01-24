package com.brentandjody.tomatohub.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by brentn on 22/01/16.
 * Speed databasehelper
 */
public class Speeds extends DatabaseHelper {

    private static final String TAG = Speeds.class.getName();

    private static final String[] PROJECTION = {
            DBContract.SpeedEntry._ID,
            DBContract.SpeedEntry.COLUMN_ROUTER_ID,
            DBContract.SpeedEntry.COLUMN_TIMESTAMP,
            DBContract.SpeedEntry.COLUMN_LAN_SPEED,
            DBContract.SpeedEntry.COLUMN_WAN_SPEED
    };

    public Speeds(Context context) { super(context); }

    public long insert(Speed speed) {
        SQLiteDatabase db = getWritableDatabase();
        long result=-1;
        try {
            ContentValues values = new ContentValues();
            values.put(DBContract.SpeedEntry.COLUMN_ROUTER_ID, speed.routerId());
            values.put(DBContract.SpeedEntry.COLUMN_TIMESTAMP, speed.timestamp());
            values.put(DBContract.SpeedEntry.COLUMN_LAN_SPEED, speed.lanSpeed());
            values.put(DBContract.SpeedEntry.COLUMN_WAN_SPEED, speed.wanSpeed());
            result = db.insert(DBContract.SpeedEntry.TABLE_NAME, null, values);
        } finally {
            db.close();
        }
        return result;
    }

    public Collection<Speed> get(String router_id) {
        SQLiteDatabase db = getReadableDatabase();
        Collection<Speed> result = new ArrayList<>();
        try {
            Cursor c = db.query(
                    DBContract.SpeedEntry.TABLE_NAME,
                    PROJECTION,
                    DBContract.SpeedEntry.COLUMN_ROUTER_ID+"=?",
                    new String[] {router_id},
                    null,
                    null,
                    DBContract.SpeedEntry.COLUMN_TIMESTAMP
            );
            while (c.moveToNext()) {
                result.add(new Speed(
                        c.getString(c.getColumnIndex(DBContract.SpeedEntry.COLUMN_ROUTER_ID)),
                        c.getLong(c.getColumnIndex(DBContract.SpeedEntry.COLUMN_TIMESTAMP)),
                        c.getDouble(c.getColumnIndex(DBContract.SpeedEntry.COLUMN_LAN_SPEED)),
                        c.getDouble(c.getColumnIndex(DBContract.SpeedEntry.COLUMN_WAN_SPEED))
                ));
            }
            c.close();
        } finally {
            db.close();
        }
        return result;
    }

    public void deleteAllHistory() {
        SQLiteDatabase db = getWritableDatabase();
        Log.d(TAG, "Erasing all speed test history data");
        try {
            db.delete(DBContract.SpeedEntry.TABLE_NAME, null, null);
        } finally {
            db.close();
        }
    }

    public int isExtreme(String router_id, int wan_or_lan, double speed) {
        double avg = avgSpeed(router_id, wan_or_lan);
        double dev = stdDev(router_id, wan_or_lan, avg);
        if (dev<0) return 0; //not enough samples
        Log.d(TAG, "isExtreme: "+(int)((speed-avg)/dev));
        return (int) ((speed-avg)/dev);
    }

    private double avgSpeed(String router_id, int wan_or_lan) {
        double result = -1;
        String column = wan_or_lan==Network.LAN?DBContract.SpeedEntry.COLUMN_LAN_SPEED
                :wan_or_lan==Network.WAN?DBContract.SpeedEntry.COLUMN_WAN_SPEED:"";
        SQLiteDatabase db = getReadableDatabase();
        try {
            Cursor c = db.rawQuery("SELECT AVG(" + column + ")"
                    + " FROM " + DBContract.SpeedEntry.TABLE_NAME
                    + " WHERE " + DBContract.SpeedEntry.COLUMN_ROUTER_ID + "=? AND " + column + ">0"
                    , new String[]{router_id});
            if (c.moveToFirst()) result = c.getDouble(0);
            c.close();
        } finally {
            db.close();
        }
        Log.d(TAG, "Avg Speed: "+result);
        return result;
    }

    private double stdDev(String router_id, int wan_or_lan, double average_speed) {
        double result = -1;
        String column = wan_or_lan==Network.LAN?DBContract.SpeedEntry.COLUMN_LAN_SPEED
                :wan_or_lan==Network.WAN?DBContract.SpeedEntry.COLUMN_WAN_SPEED:"";
        SQLiteDatabase db = getReadableDatabase();
        try {
            Cursor c = db.query(
                    DBContract.SpeedEntry.TABLE_NAME,
                    PROJECTION,
                    DBContract.SpeedEntry.COLUMN_ROUTER_ID + "=? AND " + column + ">0",
                    new String[]{router_id}, null, null, null
            );
            if (c.getCount() >= 5) { //require at least 5 values
                double squared_difference=0;
                while (c.moveToNext()) {
                    squared_difference += Math.pow(average_speed - c.getDouble(c.getColumnIndex(column)), 2);
                }
                double variance = squared_difference / (c.getCount() - 1); // Sample data requires (count-1)
                result = Math.sqrt(variance);
            } else {
                Log.d(TAG, "Too few samples to calculate stddev");
            }
            c.close();
        } finally {
            db.close();
        }
        Log.d(TAG, "StdDev: "+result);
        return result;
    }

}
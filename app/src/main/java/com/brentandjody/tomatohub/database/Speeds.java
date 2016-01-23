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

    public double lanSpeedStdDev(String router_id) {
        double result = -1;
        SQLiteDatabase db = getReadableDatabase();
        try {
            Cursor c = db.query(
                    DBContract.SpeedEntry.TABLE_NAME,
                    PROJECTION,
                    DBContract.SpeedEntry.COLUMN_ROUTER_ID + "=? AND " + DBContract.SpeedEntry.COLUMN_LAN_SPEED + ">0",
                    new String[]{router_id}, null, null, null
            );
            int count=0;
            double total=0;
                while (c.moveToNext()) {
                count++;
                total+=c.getDouble(c.getColumnIndex(DBContract.SpeedEntry.COLUMN_LAN_SPEED));
            }
            if (count >= 5) { //require at least 5 values
                double mean = total / count;
                c.moveToFirst();
                double squared_difference = Math.pow(mean - c.getDouble(c.getColumnIndex(DBContract.SpeedEntry.COLUMN_LAN_SPEED)), 2);
                while (c.moveToNext()) {
                    squared_difference += Math.pow(mean - c.getDouble(c.getColumnIndex(DBContract.SpeedEntry.COLUMN_LAN_SPEED)), 2);
                }
                double variance = squared_difference / (count - 1); // Sample data requires (count-1)
                result = Math.sqrt(variance);
            } else {
                Log.d(TAG, "Too few samples to calculate stddev");
            }
            c.close();
        } finally {
            db.close();
        }
        Log.d(TAG, "LAN StdDev: "+result);
        return result;
    }

    public double wanSpeedStdDev(String router_id) {
        double result = -1;
        SQLiteDatabase db = getReadableDatabase();
        try {
            Cursor c = db.query(
                    DBContract.SpeedEntry.TABLE_NAME,
                    PROJECTION,
                    DBContract.SpeedEntry.COLUMN_ROUTER_ID + "=? AND " + DBContract.SpeedEntry.COLUMN_WAN_SPEED + ">0",
                    new String[]{router_id}, null, null, null
            );
            int count=0;
            double total=0;
            while (c.moveToNext()) {
                count++;
                total+=c.getDouble(c.getColumnIndex(DBContract.SpeedEntry.COLUMN_WAN_SPEED));
            }
            if (count >= 5) { //require at least 5 values
                double mean = total / count;
                c.moveToFirst();
                double squared_difference = Math.pow(mean - c.getDouble(c.getColumnIndex(DBContract.SpeedEntry.COLUMN_WAN_SPEED)), 2);
                while (c.moveToNext()) {
                    squared_difference += Math.pow(mean - c.getDouble(c.getColumnIndex(DBContract.SpeedEntry.COLUMN_WAN_SPEED)), 2);
                }
                double variance = squared_difference / (count - 1); // Sample data requires (count-1)
                result = Math.sqrt(variance);
            }
            c.close();
        } finally {
            db.close();
        }
        Log.d(TAG, "WAN StdDev: "+result);
        return result;
    }

}
package com.brentandjody.tomatohub.classes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


import com.brentandjody.tomatohub.database.DBContract.*;
import com.brentandjody.tomatohub.database.DatabaseHelper;

/**
 * Created by brent on 02/12/15.
 */
public class Devices {
    private DatabaseHelper mDatabaseHelper;
    private String mRouterId;

    public Devices(Context context, String routerId){
        mRouterId = routerId;
        mDatabaseHelper = new DatabaseHelper(context);
    }

    public void inactivateAll() {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        try {
            db.rawQuery("UPDATE " + DeviceEntry.TABLE_NAME + " SET " + DeviceEntry.COLUMN_ACTIVE + "=0", null);
        } finally {
            db.close();
        }
    }

    public Device get(String mac) {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        Device result = new Device(mRouterId, mac, "unknown");
        String[] projection = {
                DeviceEntry._ID,
                DeviceEntry.COLUMN_ROUTER_ID,
                DeviceEntry.COLUMN_MAC,
                DeviceEntry.COLUMN_NAME,
                DeviceEntry.COLUMN_CUSTOM_NAME,
                DeviceEntry.COLUMN_LAST_NETWORK,
                DeviceEntry.COLUMN_LAST_IP,
                DeviceEntry.COLUMN_ACTIVE,
                DeviceEntry.COLUMN_TRAFFIC_TIMESTAMP,
                DeviceEntry.COLUMN_TX_BYTES,
                DeviceEntry.COLUMN_RX_BYTES,
                DeviceEntry.COLUMN_LAST_SPEED
        };
        try {
            String[] macs = {mac};
            Cursor c = db.query(
                    DeviceEntry.TABLE_NAME,
                    projection,
                    DeviceEntry.COLUMN_MAC,
                    macs,
                    null, null, null
            );
            if (c.moveToFirst()) {
                result.setDetails(
                        c.getString(c.getColumnIndex(DeviceEntry.COLUMN_NAME)),
                        c.getString(c.getColumnIndex(DeviceEntry.COLUMN_CUSTOM_NAME)),
                        c.getString(c.getColumnIndex(DeviceEntry.COLUMN_LAST_NETWORK)),
                        c.getString(c.getColumnIndex(DeviceEntry.COLUMN_LAST_IP)),
                        c.getInt(c.getColumnIndex(DeviceEntry.COLUMN_ACTIVE))==1,
                        c.getInt(c.getColumnIndex(DeviceEntry.COLUMN_TX_BYTES)),
                        c.getInt(c.getColumnIndex(DeviceEntry.COLUMN_RX_BYTES)),
                        c.getInt(c.getColumnIndex(DeviceEntry.COLUMN_TRAFFIC_TIMESTAMP)),
                        c.getFloat(c.getColumnIndex(DeviceEntry.COLUMN_LAST_SPEED))
                );
            };
        } finally {
            db.close();
        }
        return result;
    }

    public long insertOrUpdate(Device device) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        long result = -1;
        try {
            ContentValues values = new ContentValues();
            values.put(DeviceEntry.COLUMN_ROUTER_ID, device.router_id());
            values.put(DeviceEntry.COLUMN_MAC, device.mac());
            values.put(DeviceEntry.COLUMN_NAME, device.originalName());
            values.put(DeviceEntry.COLUMN_CUSTOM_NAME, device.customName());
            values.put(DeviceEntry.COLUMN_LAST_NETWORK, device.lastNetwork());
            values.put(DeviceEntry.COLUMN_LAST_IP, device.lastIP());
            values.put(DeviceEntry.COLUMN_ACTIVE, device.isActive());
            values.put(DeviceEntry.COLUMN_TRAFFIC_TIMESTAMP, device.timestamp());
            values.put(DeviceEntry.COLUMN_TX_BYTES, device.txTraffic());
            values.put(DeviceEntry.COLUMN_RX_BYTES, device.rxTraffic());
            values.put(DeviceEntry.COLUMN_LAST_SPEED, device.lastSpeed());

            result = db.insertWithOnConflict(DeviceEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } finally {
            db.close();
        }
        return result;
    }

}

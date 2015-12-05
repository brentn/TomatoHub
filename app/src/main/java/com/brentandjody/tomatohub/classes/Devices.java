package com.brentandjody.tomatohub.classes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


import com.brentandjody.tomatohub.database.DBContract.*;
import com.brentandjody.tomatohub.database.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brent on 02/12/15.
 * Class to manage all SQL operations on the devices table
 */
public class Devices {

    private static final String TAG = Devices.class.getName();

    private DatabaseHelper mDatabaseHelper;
    private String mRouterId;
    private static final String[] PROJECTION = {
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

    public Devices(Context context, String routerId){
        mRouterId = routerId;
        mDatabaseHelper = new DatabaseHelper(context);
    }

    public void inactivateAll() {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(DeviceEntry.COLUMN_ACTIVE, 0);
            db.update(DeviceEntry.TABLE_NAME, values, null, null);
        } finally {
            db.close();
        }
    }

    public Device get(String mac) {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        Device result = new Device(mRouterId, mac, "unknown");
        try {
            String[] macs = {mac};
            Cursor c = db.query(
                    DeviceEntry.TABLE_NAME,
                    PROJECTION,
                    DeviceEntry.COLUMN_MAC+"=?",
                    macs,
                    null, null, null
            );
            if (c.moveToFirst()) {
                result = getDeviceFromCursor(c);
            }
            c.close();
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        } finally {
            db.close();
        }
        return result;
    }

    public List<Device> getDevicesOnNetwork(String network_id) {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        List<Device> result = new ArrayList<>();
        try {
            Cursor c = db.query(
                    DeviceEntry.TABLE_NAME,
                    PROJECTION,
                    DeviceEntry.COLUMN_LAST_NETWORK+"=?",
                    new String[] {network_id},
                    null,
                    null,
                    DeviceEntry.COLUMN_ACTIVE+" DESC, " + DeviceEntry.COLUMN_LAST_SPEED+" DESC"
            );
            while (c.moveToNext()) {
                result.add(getDeviceFromCursor(c));
            }
            c.close();
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

    private Device getDeviceFromCursor(Cursor c) {
        String mac = c.getString(c.getColumnIndex(DeviceEntry.COLUMN_MAC));
        Device device = new Device(mRouterId, mac, "unknown");
        device.setDetails(
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
        return device;
    }

}

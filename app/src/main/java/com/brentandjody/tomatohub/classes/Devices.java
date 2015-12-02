package com.brentandjody.tomatohub.classes;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.brentandjody.tomatohub.database.DBContract;
import com.brentandjody.tomatohub.database.DatabaseHelper;

/**
 * Created by brent on 02/12/15.
 */
public class Devices {
    private DatabaseHelper mDatabaseHelper;

    public Devices(Context context){
        mDatabaseHelper = new DatabaseHelper(context);
    }

    public void inactivateAll() {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        try {
            db.rawQuery("UPDATE " + DBContract.DeviceEntry.TABLE_NAME + " SET " + DBContract.DeviceEntry.COLUMN_ACTIVE + "=0", null);
        } finally {
            db.close();
        }
    }

    public long insertOrUpdate(Device device) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        long result = -1;
        try {
            ContentValues values = new ContentValues();
            values.put(DBContract.DeviceEntry.COLUMN_MAC, device.getMac());
            values.put(DBContract.DeviceEntry.COLUMN_NAME, device.getOriginalName());
            values.put(DBContract.DeviceEntry.COLUMN_CUSTOM_NAME, device.getCustomName());
            values.put(DBContract.DeviceEntry.COLUMN_LAST_IP, device.getLastIP());
            values.put(DBContract.DeviceEntry.COLUMN_ACTIVE, device.isActive());
            if (!device.getLastIP().isEmpty()) {
                values.put(DBContract.DeviceEntry.COLUMN_TRAFFIC_TIMESTAMP, device.timestamp());
                values.put(DBContract.DeviceEntry.COLUMN_TX_BYTES, device.txTraffic());
                values.put(DBContract.DeviceEntry.COLUMN_RX_BYTES, device.rxTraffic());
            }
            result = db.insertWithOnConflict(DBContract.DeviceEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } finally {
            db.close();
        }
        return result;
    }

}

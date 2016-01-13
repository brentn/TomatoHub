package com.brentandjody.tomatohub.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by brent on 05/12/15.
 * Class for network object
 */
public class Networks {

    private static final String TAG = Networks.class.getName();

    private DatabaseHelper mDatabaseHelper;
    private static final String[] PROJECTION = {
            DBContract.NetworkEntry._ID,
            DBContract.NetworkEntry.COLUMN_ROUTER_ID,
            DBContract.NetworkEntry.COLUMN_NETWORK_ID,
            DBContract.NetworkEntry.COLUMN_CUSTOM_NAME,
            DBContract.NetworkEntry.COLUMN_TRAFFIC_TIMESTAMP,
            DBContract.NetworkEntry.COLUMN_TX_BYTES,
            DBContract.NetworkEntry.COLUMN_RX_BYTES,
            DBContract.NetworkEntry.COLUMN_LAST_SPEED
    };

    public Networks(Context context){
        mDatabaseHelper = new DatabaseHelper(context);
    }

    public Network get(String router_id, String network_id) {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        Network result = new Network(router_id, network_id);
        try {
            String[] args = {router_id, network_id};
            Cursor c = db.query(
                    DBContract.NetworkEntry.TABLE_NAME,
                    PROJECTION,
                    DBContract.NetworkEntry.COLUMN_ROUTER_ID+"=? AND "+
                    DBContract.NetworkEntry.COLUMN_NETWORK_ID+"=?",
                    args,
                    null, null, null
            );
            if (c.moveToFirst()) {
                result = networkFromCursor(router_id, c);
            }
            c.close();
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        } finally {
            db.close();
        }
        return result;
    }

    public long insertOrUpdate(Network network) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        long result = -1;
        try {
            ContentValues values = new ContentValues();
            values.put(DBContract.NetworkEntry.COLUMN_ROUTER_ID, network.routerId());
            values.put(DBContract.NetworkEntry.COLUMN_NETWORK_ID, network.networkId());
            values.put(DBContract.NetworkEntry.COLUMN_CUSTOM_NAME, network.customName());
            values.put(DBContract.NetworkEntry.COLUMN_TRAFFIC_TIMESTAMP, network.timestamp());
            values.put(DBContract.NetworkEntry.COLUMN_TX_BYTES, network.txBytes());
            values.put(DBContract.NetworkEntry.COLUMN_RX_BYTES, network.rxBytes());
            values.put(DBContract.NetworkEntry.COLUMN_LAST_SPEED, network.speed());

            result = db.insertWithOnConflict(DBContract.NetworkEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } finally {
            db.close();
        }
        return result;
    }

    private Network networkFromCursor(String router_id, Cursor c) {
        String networkId = c.getString(c.getColumnIndex(DBContract.NetworkEntry.COLUMN_NETWORK_ID));
        Network network = new Network(router_id, networkId);
        network.setDetails(c.getString(c.getColumnIndex(DBContract.NetworkEntry.COLUMN_CUSTOM_NAME)),
                c.getInt(c.getColumnIndex(DBContract.NetworkEntry.COLUMN_TX_BYTES)),
                c.getInt(c.getColumnIndex(DBContract.NetworkEntry.COLUMN_RX_BYTES)),
                c.getInt(c.getColumnIndex(DBContract.NetworkEntry.COLUMN_TRAFFIC_TIMESTAMP)),
                c.getFloat(c.getColumnIndex(DBContract.NetworkEntry.COLUMN_LAST_SPEED))
        );
        return network;
    }
}

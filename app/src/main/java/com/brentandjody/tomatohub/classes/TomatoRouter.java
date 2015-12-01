package com.brentandjody.tomatohub.classes;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.brentandjody.tomatohub.MainActivity;
import com.brentandjody.tomatohub.R;
import com.brentandjody.tomatohub.WelcomeActivity;
import com.brentandjody.tomatohub.database.DBContract;
import com.brentandjody.tomatohub.database.DatabaseHelper;

/**
 * Created by brent on 28/11/15.
 */
public class TomatoRouter extends Router {

    private static final String TAG = TomatoRouter.class.getName();

    private boolean mStartActivityHasBeenRun;
    private String mWAN="";
    private String[] mNetworks;
    private String[] mWifi;
    private String[][] mDevices;

    public TomatoRouter(MainActivity context) {
        super(context);
        mStartActivityHasBeenRun=false;
    }

    // COMMANDS
    @Override
    public String getWANInterface() {
        try { return sshCommand("nvram show|grep wan_iface|cut -d= -f2")[0]; }
        catch (Exception ex) { Log.w(TAG, "Error getting WAN interface"); return ""; }
    }
    @Override
    public String[] getLANInterfaces() {
        if (mWAN.isEmpty()) mWAN = getWANInterface();
        return sshCommand("arp|cut -d' ' -f8|sort -u|grep -v "+mWAN);
    }
    @Override
    public String[] getWIFILabels() { return sshCommand("nvram show|grep _ssid|cut -d= -f2");}
    @Override
    public String[] getConnectedDevices(String network) {return sshCommand("arp|grep -v "+network);}
    @Override
    public int getTxTrafficForIP(String ip) {
        try {return Integer.parseInt(sshCommand("grep "+ip+" /proc/net/ipt_account/*|cut -d' ' -f6")[0]); }
        catch (Exception ex) { Log.w(TAG, "Error getting tx traffic"); return 0;}
    }
    @Override
    public int getRxTrafficForIP(String ip) {
        try {return Integer.parseInt(sshCommand("grep "+ip+" /proc/net/ipt_account/*|cut -d' ' -f20")[0]); }
        catch (Exception ex) { Log.w(TAG, "Error getting tx traffic"); return 0;}
    }

    private long currentTime() { return System.currentTimeMillis()/1000L;}

    private class SSHLogon extends Router.SSHLogon
    {

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                mContext.showIcon(R.id.router, success);
                if (success) {
                    mContext.setStatusMessage(mContext.getString(R.string.scanning_network));
                    new ValueInitializer().execute();
                } else {
                    if (!mStartActivityHasBeenRun) {
                        mStartActivityHasBeenRun = true;
                        Log.i(TAG, "Redirecting to Welcome screen");
                        mContext.startActivity(new Intent(mContext, WelcomeActivity.class));
                    } else {
                        mContext.setStatusMessage(mContext.getString(R.string.connection_failure));
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "SSHLogon.postExecute:"+ex.getMessage());
            }
        }
    }

    private class ValueInitializer extends AsyncTask<Void, Void, Void> {

        boolean success=false;
        String message = "";
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                // identify various networks
                mNetworks = getLANInterfaces();
                // identify various wifi networks
                mWifi = getWIFILabels();
                // enumerate devices on each network
                mDevices = new String[mNetworks.length][];
                for (int i = 0; i < mNetworks.length; i++) {
                    mDevices[i] = getConnectedDevices(mNetworks[i]);
                }
                success=true;
            } catch(Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                if (success) {
                    int total = 0;
                    for (int i = 0; i < 5; i++) {
                        int id = -1;
                        if (i==0) id=R.id.lan_0;
                        if (i==1) id=R.id.lan_1;
                        if (i==2) id=R.id.lan_2;
                        if (i==3) id=R.id.lan_3;
                        if (i==4) id=R.id.lan_4;
                        if (mNetworks != null && i < mNetworks.length) {
                            total += mDevices[i].length;
                            mContext.showIcon(id, true);
                            mContext.setIconText(id, String.valueOf(mDevices[i].length));
                        } else {
                            mContext.showIcon(id, false);
                        }
                    }
                    mContext.setStatusMessage(mContext.getString(R.string.everything_looks_good));
                    mContext.setDevicesMessage(String.valueOf(total) + mContext.getString(R.string.devices), mContext.getString(R.string.are_connected));
                    mContext.setWifiMessage("'" + TextUtils.join("' is ON,  '", mWifi) + "' is ON");
                    if (mDevices!=null) {
                        new DeviceStatusUpdater().execute(mDevices);
                    }
                } else {
                    mContext.setStatusMessage(mContext.getString(R.string.scan_failure));
                    Log.e(TAG, message);
                }
            } catch (Exception ex) {
                Log.e(TAG, "ValueInitializer.postExecute:"+ex.getMessage());
            }
        }
    }

    private class DeviceStatusUpdater extends AsyncTask<String[], Void, Void> {

        @Override
        protected Void doInBackground(String[]... networkDevices) {
            SQLiteDatabase db = new DatabaseHelper((mContext)).getWritableDatabase();
            try {
                // to start, mark all devices as inactive
                db.rawQuery("UPDATE "+ DBContract.DeviceEntry.TABLE_NAME+ " SET " + DBContract.DeviceEntry.COLUMN_ACTIVE + "=0", null);

                for (String[] network : networkDevices) {
                    for (String device : network) {
                        String[] fields = device.split(" ");
                        String name = (fields.length > 0?fields[0]:"");
                        String ip = (fields.length > 1?fields[1]:"");
                        String mac = (fields.length > 2?fields[2]:"");
                        if (mac.length() == 18) {
                            ContentValues values = new ContentValues();
                            values.put(DBContract.DeviceEntry.COLUMN_MAC, mac);
                            values.put(DBContract.DeviceEntry.COLUMN_NAME, name);
                            values.put(DBContract.DeviceEntry.COLUMN_LAST_IP, ip);
                            values.put(DBContract.DeviceEntry.COLUMN_ACTIVE, 1);
                            if (! ip.isEmpty()) {
                                values.put(DBContract.DeviceEntry.COLUMN_TRAFFIC_TIMESTAMP, currentTime());
                                values.put(DBContract.DeviceEntry.COLUMN_TX_BYTES, getTxTrafficForIP(ip));
                                values.put(DBContract.DeviceEntry.COLUMN_RX_BYTES, getRxTrafficForIP(ip));
                            }
                            db.insertWithOnConflict(DBContract.DeviceEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                        }
                    }
                }
            } finally {
                db.close();
            }
            return null;
        }
    }

}

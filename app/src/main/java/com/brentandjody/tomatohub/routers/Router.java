package com.brentandjody.tomatohub.routers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.brentandjody.tomatohub.R;
import com.brentandjody.tomatohub.database.Wifi;
import com.brentandjody.tomatohub.routers.connection.IConnection;
import com.brentandjody.tomatohub.routers.connection.SshConnection;
import com.brentandjody.tomatohub.routers.connection.TelnetConnection;

import java.util.List;

/**
 * Created by brent on 28/11/15.
 * Abstract router base class
 */
public abstract class Router implements IConnection.OnConnectionActionCompleteListener {

    public static final int ACTIVITY_LOGON = 1;
    public static final int ACTIVITY_INTIALIZE = 2;
    public static final int ACTIVITY_DEVICES_UPDATED = 3;
    public static final int ACTIVITY_TRAFFIC_UPDATED = 4;
    public static final int ACTIVITY_INTERNET_10MDOWNLOAD = 5;
    public static final int ACTIVITY_WIFI_SPEED_TEST = 6;
    public static final int ACTIVITY_STATUS_SUCCESS = 1;
    public static final int ACTIVITY_STATUS_FAILURE = 2;

    protected OnRouterActivityCompleteListener mListener;
    protected SharedPreferences mPrefs;
    private IConnection mConnection;
    protected String mIpAddress;
    protected String mUser;
    protected String mPassword;
    private float mSpeed=-1;

    private static final String TAG = Router.class.getName();
    protected Context mContext;
    private String mWAN="";

    public Router(Context activity) {
        if (activity instanceof OnRouterActivityCompleteListener) {
            mListener = (OnRouterActivityCompleteListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnRouterActivityCompleteListener");
        }
        mContext = activity;
        mPrefs = activity.getSharedPreferences(activity.getString(R.string.sharedPreferences_name), Context.MODE_PRIVATE);

        mIpAddress = mPrefs.getString(activity.getString(R.string.pref_key_ip_address), "0.0.0.0");
        mUser = mPrefs.getString(activity.getString(R.string.pref_key_username), "root");
        mPassword = mPrefs.getString(activity.getString(R.string.pref_key_password), "");
        String protocol = mPrefs.getString(mContext.getString(R.string.pref_key_protocol), "ssh");
        switch (protocol) {
            case "ssh": mConnection = new SshConnection(this); break;
            case "telnet": mConnection = new TelnetConnection(this); break;
            default: Log.e(TAG, "Unrcognized protocol"); mConnection = null;
        }
    }

    public void connect() {
        Log.d(TAG, "Attempting to connect to: "+mIpAddress+" as: "+mUser);
        if (mConnection != null) mConnection.connect(mIpAddress, mUser, mPassword);
    }

    public void disconnect() {
        if (mConnection != null) mConnection.disconnect();
    }

    public String[] command(String command) {
        if (mConnection != null) return mConnection.execute(command);
        else return new String[0];
    }

    public void wifiSpeedTest() {
        if (mConnection != null) mConnection.speedTest();
    }

    public float getSpeedTestResult() {return mSpeed;}

    @Override
    public void onActionComplete(int action, boolean success) {
        switch (action) {
            case IConnection.ACTION_LOGON:
                mListener.onRouterActivityComplete(ACTIVITY_LOGON, success?ACTIVITY_STATUS_SUCCESS:ACTIVITY_STATUS_FAILURE);
                break;
            case IConnection.ACTION_SPEED_TEST:
                mSpeed = mConnection.getSpeedTestResult();
                mListener.onRouterActivityComplete(ACTIVITY_WIFI_SPEED_TEST, success?ACTIVITY_STATUS_SUCCESS:ACTIVITY_STATUS_FAILURE);
                break;
        }
    }

    // COMMANDS
    public abstract void initialize();
    public abstract long getBootTime();
    public abstract String getExternalIP();
    public abstract int getMemoryUsage();
    public abstract int[] getCPUUsage();
    public abstract void updateDevices();
    public abstract void updateTrafficStats();
    public abstract String getRouterId();
    public abstract List<Wifi> getWifiList();
    public abstract String[] getNetworkIds();
    public abstract int getTotalDevices();
    public abstract int getTotalDevicesOn(String network_id);
    public abstract void internetSpeedTest();

    public interface OnRouterActivityCompleteListener {
        void onRouterActivityComplete(int activity_id, int status);
    }


}


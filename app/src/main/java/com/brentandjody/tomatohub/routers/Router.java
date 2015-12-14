package com.brentandjody.tomatohub.routers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.brentandjody.tomatohub.MainActivity;
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
public abstract class Router implements IConnection.OnLogonCompleteListener {

    public static final int ACTIVITY_LOGON = 1;
    public static final int ACTIVITY_INTIALIZE = 2;
    public static final int ACTIVITY_DEVICES_UPDATED = 3;
    public static final int ACTIVITY_TRAFFIC_UPDATED = 4;
    public static final int ACTIVITY_INTERNET_10MDOWNLOAD = 5;
    public static final int ACTIVITY_STATUS_SUCCESS = 1;
    public static final int ACTIVITY_STATUS_FAILURE = 2;

    protected OnRouterActivityCompleteListener mListener;
    protected SharedPreferences mPrefs;
    private IConnection mConnection;
    protected String mIpAddress;
    protected String mUser;
    protected String mPassword;

    private static final String TAG = LinuxRouter.class.getName();
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
        mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);//activity.getSharedPreferences("Application", Context.MODE_PRIVATE);

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
        if (mConnection != null) mConnection.connect(mIpAddress, mUser, mPassword);
    }

    public String[] command(String command) {
        if (mConnection != null) return mConnection.execute(command);
        else return new String[0];
    }

    public void disconnect() {
        if (mConnection != null) mConnection.disconnect();
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
    public abstract void download10MbFile();

    public interface OnRouterActivityCompleteListener {
        void onRouterActivityComplete(int activity_id, int status);
    }


}


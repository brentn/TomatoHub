package com.brentandjody.tomatohub.routers;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.brentandjody.tomatohub.R;
import com.brentandjody.tomatohub.database.Device;
import com.brentandjody.tomatohub.routers.connection.IConnection;
import com.brentandjody.tomatohub.routers.connection.SshConnection;
import com.brentandjody.tomatohub.routers.connection.TelnetConnection;
import com.brentandjody.tomatohub.routers.connection.TestableConnection;

/**
 * Created by brent on 28/11/15.
 * Abstract router base class
 */
public abstract class Router implements IRouter, IConnection.OnConnectionActionCompleteListener {

    public static final int ACTIVITY_CONNECTED = 1;
    public static final int ACTIVITY_INTIALIZE = 2;
    public static final int ACTIVITY_DEVICES_UPDATED = 3;
    public static final int ACTIVITY_TRAFFIC_UPDATED = 4;
    public static final int ACTIVITY_INTERNET_10MDOWNLOAD = 5;
    public static final int ACTIVITY_WIFI_SPEED_TEST = 6;
    public static final int ACTIVITY_STATUS_SUCCESS = 1;
    public static final int ACTIVITY_STATUS_FAILURE = 2;

    private static final String TAG = Router.class.getName();
    protected OnRouterActivityCompleteListener mListener;
    protected Context mContext;
    protected SharedPreferences mPrefs;
    protected String mIpAddress;
    protected String mUser;
    protected String mPassword;
    private TestableConnection mConnection;
    private Boolean mTestFileSent =null;

    public Router(Context activity) {
        try {
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
                case "ssh":
                    mConnection = new SshConnection(this);
                    break;
                case "telnet":
                    mConnection = new TelnetConnection(this);
                    break;
                default:
                    Log.e(TAG, "Unrcognized protocol");
                    mConnection = null;
            }
        } catch (Exception ex) {
            Log.e(TAG, "constructor error: "+ex.getMessage());
        }
    }

    public void connect() {
        Log.d(TAG, "Attempting to connect to: "+mIpAddress+" as: "+mUser);
        try {
            mConnection.connect(mIpAddress, mUser, mPassword);
        } catch (Exception ex) {
            Log.e(TAG, "connect(): "+ex.getMessage());
        }
    }

    public void disconnect() {
        Log.d(TAG, "Disconnecting from router");
        try {
            if (mConnection != null) mConnection.disconnect();
        } catch (Exception ex) {
            Log.e(TAG, "disconnect(): "+ex.getMessage());
        }
    }

    @Override
    public float getConnectionSpeed() {
        return mConnection.getLastTestedSpeed();
    }

    public String[] command(String command) {
        Log.d(TAG, "executing command: "+command);
        try {
            if (mConnection != null) return mConnection.execute(command);
            else {
                Log.w(TAG, "Cannot execute command: null connection");
                return new String[0];
            }
        } catch (Exception ex) {
            Log.e(TAG, "command(): "+ex.getMessage());
            return new String[0];
        }
    }

    @Override
    public void wifiSpeedTest(int port) {
        Log.d(TAG, "performing wifiSpeedTest");
        try {
            mTestFileSent =null;
            WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            String myIp = intToIp(wifi.getConnectionInfo().getIpAddress());
            mConnection.listen(port);
            String[] output = (mConnection.execute("dd if=/dev/zero bs=1K count=1K | nc "+myIp+" "+port +"; echo $?"));
            mTestFileSent = output[output.length-1].equals("0"); //grab the last line of the output only
        } catch (Exception ex) {
            Log.e(TAG, "wifiSpeedTest: "+ex.getMessage());
        }
    }

    @Override
    public long isPrioritizedUntil(String ip) {
        return Device.NOT_PRIORITIZED;
    }

    @Override
    public void onActionComplete(int action, boolean success) {
        try {
            switch (action) {
                case IConnection.ACTION_LOGON:
                    mListener.onRouterActivityComplete(ACTIVITY_CONNECTED, success ? ACTIVITY_STATUS_SUCCESS : ACTIVITY_STATUS_FAILURE);
                    break;
                case IConnection.ACTION_SPEED_TEST:
                    while (mTestFileSent==null) {
                        Thread.sleep(10);
                    }
                    if (!mTestFileSent) {
                        Log.w(TAG, "Test file not sent successfully from router");
                    }
                    if (!success) {
                        Log.w(TAG, "Test file not received successfully from router");
                    }
                    mListener.onRouterActivityComplete(ACTIVITY_WIFI_SPEED_TEST, (success && mTestFileSent) ? ACTIVITY_STATUS_SUCCESS : ACTIVITY_STATUS_FAILURE);
                    break;
            }
        } catch (Exception ex) {
            Log.e(TAG, "onActionComplete(): "+ex.getMessage());
        }
    }

    public interface OnRouterActivityCompleteListener {
        void onRouterActivityComplete(int activity_id, int status);
    }

    public static String intToIp(int i) {

        return (i & 0xFF ) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF) + "." +
                ( (i >> 24 ) & 0xFF) ;
    }

}


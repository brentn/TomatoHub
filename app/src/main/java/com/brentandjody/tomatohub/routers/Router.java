package com.brentandjody.tomatohub.routers;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.brentandjody.tomatohub.MainActivity;
import com.brentandjody.tomatohub.R;
import com.brentandjody.tomatohub.database.Wifi;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

/**
 * Created by brent on 28/11/15.
 * Abstract router base class
 */
public abstract class Router {

    public static final int ACTIVITY_LOGON = 1;
    public static final int ACTIVITY_INTIALIZE = 2;
    public static final int ACTIVITY_DEVICES_UPDATED = 3;
    public static final int ACTIVITY_TRAFFIC_UPDATED = 4;
    public static final int ACTIVITY_STATUS_SUCCESS = 1;
    public static final int ACTIVITY_STATUS_FAILURE = 2;
    public static final int ACTIVITY_STATUS_ERROR = 3;

//    public static final String IP_PREF = "prefRouterIP";
//    public static final  String PORT_PREF = "prefRouterPort";
//    public static final  String USER_PREF = "prefRouterUser";
//    public static final String PASS_PREF = "prefRouterPass";

    protected OnRouterActivityCompleteListener mListener;
    protected SharedPreferences mPrefs;
    protected String mIpAddress;
    protected String mUser;
    protected String mPassword;

    private static final String TAG = LinuxRouter.class.getName();
    protected MainActivity mContext;
    protected Session mSession = null;
    protected Socket mSocket = null;
    private String mWAN="";

    public Router(MainActivity activity) {
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
    }

    public void connect() {
        switch (mPrefs.getString(mContext.getString(R.string.pref_key_protocol), "ssh")) {
            case "ssh":
                new SSHLogon().execute();
                break;
            case "telnet":
                new TelnetLogon().execute();
                break;
        }
    }

    public void disconnect() {
        if (mSession != null) {
            mSession.disconnect();
            mSession = null;
        }
        if (mSocket != null) {
            try { mSocket.close(); }
            catch (Exception ex) {}
            mSocket=null;
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

    public String getRouterType() { return mPrefs.getString(mContext.getString(R.string.pref_key_router_type), "<unknown"); }

    protected class SSHLogon extends AsyncTask<Void,Void,Void>
    {
        boolean success;
        @Override
        protected Void doInBackground(Void... voids) {
            JSch ssh = new JSch();
            try {
                Log.d(TAG, "Logging in via SSH");
                if (mSession!=null) mSession.disconnect();
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                mSession = ssh.getSession(mUser, mIpAddress, 22);
                mSession.setConfig(config);
                mSession.setPassword(mPassword);
                mSession.connect(10000);
                success=true;
            } catch (Exception ex) {
                success=false;
                if (mSession!=null)
                    mSession.disconnect();
                Log.e(TAG, ex.getMessage());
            }
            return null;
        }

    }

    protected class TelnetLogon extends AsyncTask<Void, Void, Void> {
        boolean success;
        @Override
        protected Void doInBackground(Void... params) {
            try {
                success=false;
                Log.d(TAG, "Logging in via Telnet");
                if (mSocket !=null) {
                    try { mSocket.close(); }
                    catch (Exception ex) {}
                }
                mSocket = new Socket(mIpAddress, 23);
                mSocket.setKeepAlive(true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(mSocket.getOutputStream(), true);
                while (mSocket!=null) {
                    String line = reader.readLine();
                    if (line==null) break;
                    Log.v(TAG, "TELNET sent:"+line);
                    if (line.trim().endsWith("username:") || line.trim().endsWith("ogin:")) {
                        Log.d(TAG, "TELNET: sending username");
                        writer.write(mUser+"\r\n");
                        writer.flush();
                        writer.write("\r\n");
                        writer.flush();
                    } else if (line.trim().endsWith("assword:")) {
                        Log.d(TAG, "TELNET: sending password");
                        writer.write(mPassword+"\r\n");
                        writer.flush();
                        Log.w(TAG, "TELNET: Logged in");
                        success=true;
                        break;
                    } else {
                        writer.write("\r\n");
                        writer.flush();
                    }
                }
            } catch (Exception ex) {
                success=false;
                Log.d(TAG, ex.getMessage());
            }
            return null;
        }
    }

    protected String[] command(String command){
        String[] result = new String[0];
        if (mSession!=null) {
            try {
                Channel channel = mSession.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                ByteArrayOutputStream sb = new ByteArrayOutputStream();
                channel.setOutputStream(sb);
                channel.connect();
                while (!channel.isClosed()) {
                    Thread.sleep(10);
                }
                channel.disconnect();
                List<String> lines = Arrays.asList(sb.toString().split("\n"));
                try {lines.removeAll(Arrays.asList("", null));}
                catch (Exception ex) {}
                result = lines.toArray(new String[lines.size()]);
                Log.v("SSH result", sb.toString());
            } catch (Exception ex) {
                result = new String[0];
                Log.e(TAG, (ex.getMessage()==null?"SSH command failed: "+command:ex.getMessage()));
            }
        } else {
            Log.d(TAG, "null ssh session");
        }
        return result;
    }

    public interface OnRouterActivityCompleteListener {
        void onRouterActivityComplete(int activity_id, int status);
    }
}

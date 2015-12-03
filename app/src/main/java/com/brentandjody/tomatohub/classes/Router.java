package com.brentandjody.tomatohub.classes;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.brentandjody.tomatohub.MainActivity;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Created by brent on 28/11/15.
 */
public abstract class Router {

    public static final String IP_PREF = "prefRouterIP";
    public static final  String PORT_PREF = "prefRouterPort";
    public static final  String USER_PREF = "prefRouterUser";
    public static final String PASS_PREF = "prefRouterPass";

    protected SharedPreferences mPrefs;
    protected String mIpAddress;
    protected int mPort;
    protected String mUser;
    protected String mPassword;

    private static final String TAG = TomatoRouter.class.getName();
    protected MainActivity mContext;
    protected Session mSession;
    private String mWAN="";

    public Router(MainActivity context) {
        mContext = context;
        mPrefs = context.getSharedPreferences("Application", Context.MODE_PRIVATE);

        mIpAddress = mPrefs.getString(IP_PREF, "0.0.0.0");
        mPort = mPrefs.getInt(PORT_PREF, 22);
        mUser = mPrefs.getString(USER_PREF, "root");
        mPassword = mPrefs.getString(PASS_PREF, "");
    }

    public void connect() {
        new SSHLogon().execute();
    }

    public void disconnect() {
        if (mSession != null) {
            mSession.disconnect();
            mSession = null;
        }
    }
    // COMMANDS
    public abstract String getWANInterface();
    public abstract String[] getLANInterfaces();
    public abstract String[] getWIFILabels();
    public abstract String[] getConnectedDevices(String network);
    public abstract int getTxTrafficForIP(String ip);
    public abstract int getRxTrafficForIP(String ip);

    protected class SSHLogon extends AsyncTask<Void,Void,Void>
    {
        boolean success;
        @Override
        protected Void doInBackground(Void... voids) {
            JSch ssh = new JSch();
            try {
                if (mSession!=null) mSession.disconnect();
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                mSession = ssh.getSession(mUser, mIpAddress, mPort);
                mSession.setConfig(config);
                mSession.setPassword(mPassword);
                mSession.connect(10000);
                success = true;
            } catch (Exception ex) {
                success = false;
                if (mSession!=null)
                    mSession.disconnect();
                Log.e(TAG, ex.getMessage());
            }
            return null;
        }
    }


    protected String[] sshCommand(String command){
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
                lines.removeAll(Arrays.asList("", null));
                return lines.toArray(new String[lines.size()]);
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
        }
        return new String[0];
    }
}

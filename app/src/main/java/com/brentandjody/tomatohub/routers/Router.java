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

import org.apache.commons.net.telnet.TelnetClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
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

    protected OnRouterActivityCompleteListener mListener;
    protected SharedPreferences mPrefs;
    protected String mIpAddress;
    protected String mUser;
    protected String mPassword;

    private static final String TAG = LinuxRouter.class.getName();
    protected MainActivity mContext;
    protected Session mSession = null;
    private TelnetHelper mTelnet;
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
        try {
            if (mSession != null) {
                mSession.disconnect();
                mSession = null;
            }
            if (mTelnet != null) {
                mTelnet.disconnect();
            }
        } catch (Exception ex) {}
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

    protected class SSHLogon extends AsyncTask<Void,Void,Void>
    {
        boolean success;
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                JSch ssh = new JSch();
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
                if (mSession!=null) {
                    mSession.disconnect();
                    mSession = null;
                }
                Log.e(TAG, ex.getMessage());
            }
            return null;
        }

    }

    protected class TelnetLogon extends AsyncTask<Void, Void, Void> {
        boolean success=false;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Log.d(TAG, "Logging in via telnet");
                mTelnet = new TelnetHelper(mIpAddress, mUser, mPassword);
                success = true;
                Log.d(TAG, "Telnet logged in");
            } catch (Exception ex) {
                success=false;
                if (mTelnet!=null) {
                    mTelnet.disconnect();
                    mTelnet=null;
                }
                Log.e(TAG, ex.getMessage());
            }
            return null;
        }
    }


    public String[] command(String command) {
        String[] result;
        switch (mPrefs.getString(mContext.getString(R.string.pref_key_protocol), "ssh")) {
            case "ssh": result = sshCommand(command); break;
            case "telnet": result = telnetCommand(command); break;
            default:
                Log.w(TAG, "Unrecognized protocol");
                result = new String[0];
                break;
        }
        return result;
    }

    private String[] sshCommand(String command){
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

    private String[] telnetCommand(String command) {
        String[] result = new String[0];
        Log.v("Telnet command:",command);
        try {
            result = mTelnet.sendCommand(command);
            Log.v("Telnet result", Arrays.toString(result));
        } catch (Exception ex) {
            Log.e(TAG, (ex.getMessage()==null?"Telnet command failed: "+command:ex.getMessage()));
        }
        return result;
    }

    public interface OnRouterActivityCompleteListener {
        void onRouterActivityComplete(int activity_id, int status);
    }

    private class TelnetHelper {

        private TelnetClient telnet = new TelnetClient();
        private InputStream in;
        private PrintStream out;
        private String prompt = "# ";

        public TelnetHelper(String server, String user, String password) throws Exception {
            telnet.connect(server, 23);
            in = telnet.getInputStream();
            out = new PrintStream(telnet.getOutputStream());

            readUntil("ogin: ");
            write(user);
            readUntil("assword: ");
            write(password);
            readUntil(prompt);
            write("stty -echo");
            readUntil(prompt);
        }

        public String readUntil(String pattern) throws Exception {
            char lastChar = pattern.charAt(pattern.length() - 1);
            StringBuffer sb = new StringBuffer();
            char ch = (char) in.read();
            while (true) {
                sb.append(ch);
                //System.out.print(ch);
                if (sb.toString().endsWith("Closing connection")) {
                    throw new Exception("Wrong Telnet arguments passed");
                }
                if (sb.toString().endsWith(prompt) && !pattern.equals(prompt)) {
                    return sb.toString();
                }
                if (ch == lastChar) {
                    if (sb.toString().endsWith(pattern)) {
                        return sb.toString();
                    }
                }
                ch = (char) in.read();
            }
        }

        public void write(String value) {
            try {
                out.println(value);
                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public String[] sendCommand(String command) {
            try {
                write(command);
                //readUntil(command+"  \n");  //ignore echo of command
                String[] result = readUntil(prompt).split("\n");
                return Arrays.copyOfRange(result, 0, result.length-1); //remove prompt from end
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public void disconnect() {
            try {
                telnet.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}


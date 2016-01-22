package com.brentandjody.tomatohub.routers.connection;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by brentn on 13/12/15.
 * Implement telnet connection to router using sockets
 */
public class TelnetConnection extends TestableConnection implements TestableConnection.OnSpeedTestCompleteListener {
    private static final String TAG = TelnetConnection.class.getName();
    private OnConnectionActionCompleteListener mListener;

    private String mIpAddress;
    private String mUser;
    private String mPassword;
    private TelnetSession mSession;
    private List<AsyncTask> mRunningTasks;

    public TelnetConnection(OnConnectionActionCompleteListener listener)  {
        super();
        mListener=listener;
    }

    @Override
    public void connect(String ipAddress, String username, String password) {
        try {
            mRunningTasks = new ArrayList<>();
            mIpAddress=ipAddress;
            mUser=username;
            mPassword=password;
            new BackgroundLogon().execute();
        } catch (Exception ex) {
            Log.e(TAG, "connect() "+ex.getMessage());
        }
    }

    @Override
    public void disconnect() {
        try {
            for (AsyncTask task : mRunningTasks) {
                task.cancel(true);
            }
            if (mSession != null) {
                mSession.disconnect();
                mSession=null;
            }
        } catch (Exception ex) {
            Log.e(TAG, "disconnect() "+ex.getMessage());
        }
    }

    @Override
    public String[] execute(String command) {
        String[] result = new String[0];
        Log.v("Telnet command:",command);
        try {
            result = mSession.sendCommand(command);
            Log.v("Telnet result", Arrays.toString(result));
        } catch (Exception ex) {
            Log.e(TAG, (ex.getMessage()==null?"Telnet command failed: "+command:ex.getMessage()));
        }
        return result;
    }

    @Override
    public void onSpeedTestComplete(boolean success) {
        mListener.onActionComplete(ACTION_SPEED_TEST, success);
    }

    protected class BackgroundLogon extends AsyncTask<Void, Void, Void> {
        boolean success=false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mRunningTasks.add(this);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Log.d(TAG, "Logging in via telnet");
                resetSession();
                mSession = new TelnetSession(mIpAddress, mUser, mPassword);
                success = true;
                Log.d(TAG, "Telnet logged in");
            } catch (Exception ex) {
                success=false;
                resetSession();
                Log.e(TAG, ex.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mRunningTasks.remove(this);
            if (isCancelled()) resetSession();
            else mListener.onActionComplete(IConnection.ACTION_LOGON, success);
        }

        private void resetSession() {
            if (mSession!=null) {
                mSession.disconnect();
                mSession=null;
            }
        }
    }

}



package com.brentandjody.tomatohub.routers.connection;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by brentn on 13/12/15.
 * Implements SSH connection to router
 */
public class SshConnection extends TestableConnection implements TestableConnection.SpeedTestCompleteListener {
    private static final String TAG = SshConnection.class.getName();
    private OnConnectionActionCompleteListener mListener;

    private String mIpAddress;
    private String mUser;
    private String mPassword;
    private Session mSession;
    private List<AsyncTask> mRunningTasks;

    public SshConnection(OnConnectionActionCompleteListener listener) {
        super();
        mListener = listener;
        super.mListener = this;
    }

    @Override
    public void connect(String ipAddress, String username, String password){
        try {
            mRunningTasks = new ArrayList<>();
            mIpAddress = ipAddress;
            mUser = username;
            mPassword = password;
            new BackgroundLogon().execute();
        } catch (Exception ex) {
            Log.e(TAG, "connect() "+ex.getMessage());
        }
    }

    @Override
    public void disconnect() {
        try {
            super.disconnect();
            for(AsyncTask task : mRunningTasks) {
                task.cancel(true);
            }
            if (mSession!=null) {
                mSession.disconnect();
                mSession=null;
            }
        } catch (Exception ex) {
            Log.e(TAG, "disconnect() "+ex.getMessage());
        }

    }

    public void onSpeedTestComplete(boolean success) {
        mListener.onActionComplete(ACTION_SPEED_TEST, success);
    }

    @Override
    public String[] execute(String command) {
        String[] result = new String[0];
        Log.v(TAG, "Ssh command: "+command);
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
                try { lines.removeAll(Arrays.asList("", null));}
                catch (Exception ex) {}
                result = lines.toArray(new String[lines.size()]);
                Log.v("SSH result", sb.toString());
            } catch (Exception ex) {
                result = new String[0];
                Log.e(TAG, (ex.getMessage()==null?"SSH command failed: "+command:ex.getMessage()));
            }
        } else {
            Log.d(TAG, "command failed: null ssh session");
        }
        return result;
    }

    private class BackgroundLogon extends AsyncTask<Void,Void,Void>
    {
        boolean success;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mRunningTasks.add(this);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                JSch ssh = new JSch();
                Log.d(TAG, "Logging in via SSH");
                resetSession();
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                mSession = ssh.getSession(mUser, mIpAddress, 22);
                mSession.setConfig(config);
                mSession.setPassword(mPassword);
                mSession.connect(10000);
                success=true;
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
                mSession = null;
            }
        }
    }

}

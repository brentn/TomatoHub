package com.brentandjody.tomatohub.routers.connection;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Created by brentn on 13/12/15.
 * Implements SSH connection to router
 */
public class SshConnection implements IConnection {
    private static final String TAG = SshConnection.class.getName();
    private OnLogonCompleteListener mListener;

    private String mIpAddress;
    private String mUser;
    private String mPassword;
    private Session mSession;

    public SshConnection(OnLogonCompleteListener listener) {
        mListener = listener;
    }

    @Override
    public void connect(String ipAddress, String username, String password) {
        mIpAddress=ipAddress;
        mUser=username;
        mPassword=password;
        new BackgroundLogon().execute();
    }

    @Override
    public void disconnect() {
        if (mSession!=null) {
            mSession.disconnect();
            mSession=null;
        }
    }

    @Override
    public String[] execute(String command) {
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

    private class BackgroundLogon extends AsyncTask<Void,Void,Void>
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

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mListener.onLogonComplete(success);
        }
    }

}

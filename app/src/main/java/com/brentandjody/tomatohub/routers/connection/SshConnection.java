package com.brentandjody.tomatohub.routers.connection;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.xml.transform.stream.StreamSource;

/**
 * Created by brentn on 13/12/15.
 * Implements SSH connection to router
 */
public class SshConnection implements IConnection {
    private static final String TAG = SshConnection.class.getName();
    private OnConnectionActionCompleteListener mListener;

    public SshConnection(OnConnectionActionCompleteListener listener) {
        mListener = listener;
    }

    private String mIpAddress;
    private String mUser;
    private String mPassword;
    private Session mSession;
    private float mSpeed=-1;
    private List<AsyncTask> mRunningTasks;


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

    @Override
    public void speedTest() {
        try {
            new Transfer10MbToRouter().execute();
        } catch (Exception ex) {
            Log.e(TAG, "speedTest() "+ex.getMessage());
        }

    }

    @Override
    public float getSpeedTestResult() {return mSpeed;}

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

    private class Transfer10MbToRouter extends AsyncTask<Void, Void, Void> {
        int number_of_bytes=1000000;
        boolean success=false;
        long startTime;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mRunningTasks.add(this);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                startTime = System.currentTimeMillis();
                Channel channel = mSession.openChannel("exec");
                try {
                    ((ChannelExec) channel).setCommand("scp -t /dev/null");
                    BufferedOutputStream out = new BufferedOutputStream(channel.getOutputStream());
                    channel.connect();
                    out.write(("C0644 "+number_of_bytes+" filename\n").getBytes());
                    out.flush();
                    byte[] buf=new byte[1024];
                    RandomInputStream ris = new RandomInputStream();
                    int remaining_bytes=number_of_bytes;
                    while(remaining_bytes>0){
                        int len;
                        if (remaining_bytes > buf.length) {
                            len = ris.read(buf, 0, buf.length);
                            remaining_bytes -= buf.length;
                        } else {
                            len = ris.read(buf, 0, (remaining_bytes));
                            remaining_bytes = 0;
                        }
                        out.write(buf, 0, len);
                    }
                    ris.close();
                    out.flush();
                    out.close();
                } finally {
                    channel.disconnect();
                }
                success=true;
            } catch (Exception ex) {
                success=false;
                Log.e(TAG, (ex.getMessage()==null?"SSH transferBytes failed":"Transfer10MbToRouter: "+ex.getMessage()+ TextUtils.join("\n", ex.getStackTrace())));
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mRunningTasks.remove(this);
            if (!isCancelled()) {
                mSpeed = -1;
                if (success) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    float Mbits = number_of_bytes * 8 / 1000000; //convert from bytes to Megabits
                    if (elapsedTime > 0)
                        mSpeed = Mbits / elapsedTime * 1000; // convert from milliseconds to seconds
                }
                mListener.onActionComplete(IConnection.ACTION_SPEED_TEST, success);
            }
        }

    }

    private class RandomInputStream extends InputStream {
        private Random rn = new Random(0);

        @Override
        public int read() { return rn.nextInt(); }
    }
}

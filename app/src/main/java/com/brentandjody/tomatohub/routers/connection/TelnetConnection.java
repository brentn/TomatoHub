package com.brentandjody.tomatohub.routers.connection;

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
public class TelnetConnection extends TestableConnection implements TestableConnection.SpeedTestCompleteListener{
    private static final String TAG = TelnetConnection.class.getName();
    private OnConnectionActionCompleteListener mListener;

    private String mIpAddress;
    private String mUser;
    private String mPassword;
    private TelnetSession mSession;
    private List<AsyncTask> mRunningTasks;

    public TelnetConnection(OnConnectionActionCompleteListener listener)  {
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
    protected void setUpConnection(int port) {
    }

    public void onSpeedTestComplete(boolean success) {
        mListener.onActionComplete(ACTION_SPEED_TEST, success);
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

    private class TelnetSession {

        private TelnetClient telnet = new TelnetClient();
        private InputStream in;
        private PrintStream out;
        private String prompt = "# ";

        public TelnetSession(String server, String user, String password) throws Exception {
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
            StringBuilder sb = new StringBuilder();
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

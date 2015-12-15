package com.brentandjody.tomatohub.routers.connection;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * Created by brentn on 13/12/15.
 * Implement telnet connection to router using sockets
 */
public class TelnetConnection implements IConnection {
    private static final String TAG = TelnetConnection.class.getName();
    private OnConnectionActionCompleteListener mListener;

    private String mIpAddress;
    private String mUser;
    private String mPassword;
    private TelnetSession mSession;
    private float mSpeed=-1;

    public TelnetConnection(OnConnectionActionCompleteListener listener)  {
        mListener=listener;
    }

    @Override
    public void connect(String ipAddress, String username, String password) {
        try {
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
            if (mSession != null) {
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
    public float getSpeedTestResult() { return mSpeed; }

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
        protected void onCancelled() {
            super.onCancelled();
            success=false;
            resetSession();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mListener.onActionComplete(IConnection.ACTION_LOGON, success);
        }

        private void resetSession() {
            if (mSession!=null) {
                mSession.disconnect();
                mSession=null;
            }
        }
    }

    private class Transfer10MbToRouter extends AsyncTask<Void, Void, Void> {
        int number_of_bytes = 10000000;
        long startTime;
        boolean success=false;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                throw new NoSuchMethodException("Not working");
//                startTime = System.currentTimeMillis();
//                mSession.sendCommand("scp -t /dev/null");
//                byte[] data = new byte[number_of_bytes];
//                Arrays.fill(data, (byte) 0);
//                mSession.out.write(data);
//                success=true;
            } catch (Exception ex) {
                success=false;
                Log.e(TAG, ex.getMessage()==null?"Error transferring bytes via telnet":ex.getMessage());
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            success=false;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mSpeed = -1;
            if (success) {
                long elapsedTime = System.currentTimeMillis()-startTime;
                float Mbits = number_of_bytes*8/1000000; //convert from bytes to Megabits
                if (elapsedTime>0)
                    mSpeed = Mbits/elapsedTime * 1000; // convert from milliseconds to seconds
            }
            mListener.onActionComplete(IConnection.ACTION_SPEED_TEST, success);
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

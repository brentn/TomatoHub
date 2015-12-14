package com.brentandjody.tomatohub.routers.connection;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * Created by brentn on 13/12/15.
 */
public class TelnetConnection implements IConnection {
    private static final String TAG = TelnetConnection.class.getName();
    private OnLogonCompleteListener mListener;

    private String mIpAddress;
    private String mUser;
    private String mPassword;
    private TelnetSession mSession;

    public TelnetConnection(OnLogonCompleteListener listener)  {
        mListener=listener;
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
        if (mSession != null) {
            mSession.disconnect();
            mSession=null;
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

    protected class BackgroundLogon extends AsyncTask<Void, Void, Void> {
        boolean success=false;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Log.d(TAG, "Logging in via telnet");
                mSession = new TelnetSession(mIpAddress, mUser, mPassword);
                success = true;
                Log.d(TAG, "Telnet logged in");
            } catch (Exception ex) {
                success=false;
                if (mSession!=null) {
                    mSession.disconnect();
                    mSession=null;
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

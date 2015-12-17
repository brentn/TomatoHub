package com.brentandjody.tomatohub.routers.connection;

import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by brentn on 15/12/15.
 * Implement IConnection, and add features for speed test
 */
public abstract class TestableConnection implements IConnection {
    private static final String TAG = TestableConnection.class.getName();

    protected SpeedTestCompleteListener mListener;
    private float mSpeed;
    private List<AsyncTask> mRunningTests;
    private String mIPAddress;
    private int mPort;

    public TestableConnection() {
        mRunningTests = new ArrayList<>();
    }

    protected abstract void setUpConnection(int port);
    public void speedTest(String to_ip, int to_port) {
        mIPAddress=to_ip;
        mPort = to_port;
        new SpeedTester().execute();
    }
    public float getSpeedTestResult() {return mSpeed;}
    @Override
    public void disconnect() {
        for(AsyncTask task : mRunningTests)
            task.cancel(true);
    }

    private class SpeedTester extends AsyncTask<Void, Void, Void> {
        static final int NUMBER_OF_BYTES=100000;
        long startTime;
        long stopTime;
        boolean success;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setUpConnection(mPort);
            mRunningTests.add(this);
        }

        @Override
        protected Void doInBackground(Void... params) {
            Socket socket=null;
            try {
                OutputStream out=null;
                try {
                    socket = new Socket(mIPAddress, mPort);
                    out = socket.getOutputStream();
                    byte[] buf = new byte[1024];
                    RandomInputStream ris = new RandomInputStream();
                    int remaining_bytes = NUMBER_OF_BYTES;
                    startTime = System.currentTimeMillis();
                    while (remaining_bytes > 0) {
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
                    stopTime=System.currentTimeMillis();
                    success=true;
                } finally {
                    if (out!=null) {
                        out.flush();
                        out.close();
                    }
                    if (socket!=null)
                        socket.close();
                }
            } catch (Exception ex) {
                success=false;
                Log.e(TAG, ex.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mRunningTests.remove(this);
            if (!isCancelled()) {
                if (success) {
                    int Mbits = NUMBER_OF_BYTES * 8 / 1000000;
                    long elapsedTime = stopTime = startTime;
                    mSpeed = Mbits / (float) elapsedTime * 1000;
                } else {
                    mSpeed = -1;
                }
                mListener.onSpeedTestComplete(success);
            }
        }
    }

    private class RandomInputStream extends InputStream {
        private Random rn = new Random(0);

        @Override
        public int read() { return rn.nextInt(); }
    }

    public interface SpeedTestCompleteListener {
        void onSpeedTestComplete(boolean success);
    }
}

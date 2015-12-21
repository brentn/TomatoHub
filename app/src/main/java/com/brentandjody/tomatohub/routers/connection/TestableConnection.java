package com.brentandjody.tomatohub.routers.connection;

import android.os.AsyncTask;
import android.test.InstrumentationTestCase;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * Created by brentn on 15/12/15.
 * Implement IConnection, and add features for speed test
 */
public abstract class TestableConnection implements IConnection {
    private static final String TAG = TestableConnection.class.getName();
    private float mSpeed=0;
    private ByteListener mByteListener;

    public abstract void onSpeedTestComplete(boolean success);
    public float getLastTestedSpeed() {return mSpeed;}

    public void listen(int port) {
        Log.d(TAG, "Listening for bytes to be sent on port "+port);
        mByteListener = new ByteListener();
        mByteListener.execute(port);
    }

    public void stopListening() {
        Log.d(TAG, "Stop listening");
        if (mByteListener !=null) mByteListener.cancel(true);
    };

    @Override
    public void disconnect() {
        stopListening();
    }

    private class ByteListener extends AsyncTask<Integer, Void, Void> {
        boolean success;

        @Override
        protected Void doInBackground(Integer... ports) {
            ServerSocket serverSocket = null;
            Socket socket = null;
            InputStream in = null;
            NullOutputStream out = null;
            try {
                serverSocket = new ServerSocket(ports[0]);
                socket = serverSocket.accept();
                in = socket.getInputStream();
                out = new NullOutputStream();
                try {
                    byte[] buffer = new byte[1024];
                    int len;
                    int bytes=0;
                    long start = System.currentTimeMillis();
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                        bytes+=len;
                    }
                    long finish= System.currentTimeMillis();
                    Log.d(TAG, "Successfully received "+bytes+" bytes");
                    float Mbits = bytes*8/1000000F;
                    float seconds = (finish-start)/1000F;
                    mSpeed = Mbits/seconds;
                    success=true;
                } finally {
                    if (out!=null) out.close();
                    if (in!=null) in.close();
                    if (socket!=null) socket.close();
                    if (serverSocket != null) serverSocket.close();
                }
            } catch (Exception ex) {
                success=false;
                Log.e(TAG, "ByteListener: "+ex.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!isCancelled()) {
                onSpeedTestComplete(success);
            }
        }
    }

    interface OnSpeedTestCompleteListener {
        void onSpeedTestComplete(boolean success);
    }

    private class RandomInputStream extends InputStream {
        private Random rn = new Random(0);

        @Override
        public int read() { return rn.nextInt(); }
    }

    private class NullOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
        }
    }

}

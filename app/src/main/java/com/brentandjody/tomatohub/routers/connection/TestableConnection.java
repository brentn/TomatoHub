package com.brentandjody.tomatohub.routers.connection;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.brentandjody.tomatohub.R;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
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
    private String mUser;
    private String mPass;
    private Context mContext;

    public TestableConnection() {
        mRunningTests = new ArrayList<>();
    }

    public void speedTest(Context context, String url) {
        mContext = context;
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.sharedPreferences_name),Context.MODE_PRIVATE);
        mUser = prefs.getString(context.getString(R.string.pref_key_username), "root");
        mPass = prefs.getString(context.getString(R.string.pref_key_password), "");
        new SpeedTester().execute(url);
    }
    public float getSpeedTestResult() {return mSpeed;}
    @Override
    public void disconnect() {
        for(AsyncTask task : mRunningTests)
            task.cancel(true);
    }

    private class SpeedTester extends AsyncTask<String, Void, Void> {
        float speed;
        boolean success;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mRunningTests.add(this);
        }

        @Override
        protected Void doInBackground(String... urls) {
            ConnectivityManager connMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            try {
                if (networkInfo != null && networkInfo.isConnected()) {
                    InputStream is = null;
                    try {
                        URL url = new URL(urls[0]);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        String userpass = mUser + ":" + mPass;
                        String basicAuth = "Basic " + Base64.encodeToString(userpass.getBytes(), Base64.DEFAULT);
                        conn.setRequestProperty ("Authorization", basicAuth);
                        conn.setReadTimeout(10000 /* milliseconds */);
                        conn.setConnectTimeout(15000 /* milliseconds */);
                        conn.setRequestMethod("GET");
                        conn.setDoInput(true);
                        // Starts the query
                        conn.connect();
                        int response = conn.getResponseCode();
                        Log.d(TAG, "The response is: " + response);
                        is = conn.getInputStream();
                        BufferedInputStream br = new BufferedInputStream(is);
                        byte[] buffer = new byte[1024];
                        int bytes;
                        int size = 0;
                        long startTime = System.currentTimeMillis();
                        while ((bytes = is.read(buffer)) >= 0) {
                            size += bytes;
                        }
                        float seconds = (float) (System.currentTimeMillis() - startTime) / 1000;
                        float Mbits = (float) size * 8 / 1000000;
                        speed = Mbits / seconds;
                        success=true;
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                } else {
                    success=false;
                    Log.w(TAG, "No network connectivity");
                }
            } catch (Exception ex){
                success=false;
                Log.e(TAG, "Error running speedTester: " + ex.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mRunningTests.remove(this);
            if (!isCancelled()) {
                if (success) {
                    mSpeed = speed;
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

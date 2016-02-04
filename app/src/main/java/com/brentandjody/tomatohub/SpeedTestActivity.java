package com.brentandjody.tomatohub;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.brentandjody.tomatohub.database.Network;
import com.brentandjody.tomatohub.database.Speed;
import com.brentandjody.tomatohub.database.Speeds;
import com.brentandjody.tomatohub.routers.DDWrtRouter;
import com.brentandjody.tomatohub.routers.FakeRouter;
import com.brentandjody.tomatohub.routers.Router;
import com.brentandjody.tomatohub.routers.RouterType;
import com.brentandjody.tomatohub.routers.TomatoRouter;

public class SpeedTestActivity extends Activity implements Router.OnRouterActivityCompleteListener {

    private static final int TEST_PORT = 4343;
    private static final String TAG = SpeedTestActivity.class.getName();

    Router mRouter;
    TextView mInternetSpeed;
    TextView mWifiSpeed;
    ProgressBar mInternetTesting;
    ProgressBar mWifiTesting;
    long mStartTime, mStartSize;
    float mLanSpeed=-1;
    boolean mDownloading;
    String mRouterId;
    Speeds speeds = new Speeds(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test);
        mInternetSpeed = (TextView)findViewById(R.id.internet_speed);
        mWifiSpeed = (TextView)findViewById(R.id.wifi_speed);
        mInternetTesting = (ProgressBar)findViewById(R.id.pb_internet);
        mWifiTesting = (ProgressBar)findViewById(R.id.pb_wifi);

        mInternetSpeed.setText("");
        mInternetTesting.setVisibility(View.INVISIBLE);
        mWifiSpeed.setText("");
        mWifiTesting.setVisibility(View.VISIBLE);
        findViewById(R.id.wifi_fastslow).setVisibility(View.INVISIBLE);
        findViewById(R.id.internet_fastslow).setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRouter != null) {
            mRouter.disconnect();
            mRouter=null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPreferences_name), MODE_PRIVATE);
        int router_type = RouterType.value(prefs.getString(getString(R.string.pref_key_router_type), RouterType.defaultValue));
        switch (router_type) {
            case RouterType.TOMATO: mRouter = new TomatoRouter(this, null, null); break;
            case RouterType.DDWRT: mRouter = new DDWrtRouter(this, null, null); break;
            case RouterType.FAKE: mRouter = new FakeRouter(this); break;
            default: mRouter = new FakeRouter(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRouter.connect();
    }

    private void runDownloadTest() {
        mInternetTesting.setVisibility(View.VISIBLE);
        mDownloading=true;
        mStartTime = System.currentTimeMillis();
        mStartSize = 0;
        if (mRouter!=null)
            mRouter.internetSpeedTest();
    }

    private void runWifiTest() {
        if (mRouter!=null) {
            mRouter.wifiSpeedTest(TEST_PORT);
        }
    }

    @Override
    public void onRouterActivityComplete(int activity_id, int status) {
        long elapsedTime;
        int currentSize;
        double fileSize;
        double wanSpeed;
        switch(activity_id) {
            case Router.ACTIVITY_CONNECTED:
                mRouterId = mRouter.command("nvram get wan_hwaddr")[0];
                runWifiTest();
                break;
            case Router.ACTIVITY_WIFI_SPEED_TEST:
                mWifiTesting.setVisibility(View.INVISIBLE);
                if (status==Router.ACTIVITY_STATUS_SUCCESS) {
                    mLanSpeed = mRouter.getConnectionSpeed();
                    String speed = String.format("%.2f", mLanSpeed);
                    mWifiSpeed.setText(speed + " Mbps");
                    notifyExtreme(speeds.isExtreme(mRouterId, Network.LAN, mLanSpeed), findViewById(R.id.wifi_fastslow));
                } else {
                    mLanSpeed=-1;
                    mWifiSpeed.setText(R.string.test_failed);
                }
                runDownloadTest();
                break;
            case Router.ACTIVITY_10MDOWNLOAD_PROGRESS:
                // status is used for size of file in bytes
                currentSize = status;
                if (mDownloading) {
                    if (currentSize > 0) {
                        long currentTime = System.currentTimeMillis();
                        if (mStartSize == 0) { //first time, set initial values
                            mStartTime = currentTime;
                            mStartSize = currentSize;
                            mInternetSpeed.setText("0.00 Mbps");
                            Log.d(TAG, "first progress recorded");
                        } else {
                            elapsedTime = currentTime - mStartTime;
                            fileSize = ((currentSize - mStartSize) * 8) / 1000000; //adjust size to megabits
                            wanSpeed = fileSize / (elapsedTime / 1000d); //adjust time to seconds
                            mInternetSpeed.setText(String.format("%.2f", wanSpeed) + " Mbps");
                            Log.d(TAG, "Progress size:" + fileSize + " time:" + elapsedTime + " speed:" + wanSpeed);
                        }
                    }
                }
                break;
            case Router.ACTIVITY_INTERNET_10MDOWNLOAD:
                mDownloading=false;
                mInternetTesting.setVisibility(View.INVISIBLE);
                if (status!=Router.ACTIVITY_STATUS_FAILURE) {
                    currentSize = status;
                    elapsedTime = System.currentTimeMillis() - mStartTime;
                    fileSize = ((currentSize-mStartSize) * 8) / 1000000; //status is bytes (if not ACTIVITY_STATUS_FAILURE) //adjust size to megabits
                    wanSpeed = fileSize / (elapsedTime / 1000F); //adjust time to seconds
                    mInternetSpeed.setText(String.format("%.2f", wanSpeed) + " Mbps");
                    Log.d(TAG, "Final size:" + fileSize + " time:" + elapsedTime + " speed:" + wanSpeed);
                    notifyExtreme(speeds.isExtreme(mRouterId, Network.WAN, wanSpeed), findViewById(R.id.internet_fastslow));
                } else {
                    wanSpeed=-1;
                    mInternetSpeed.setText(R.string.test_failed);
                }
                if (mLanSpeed > 0 && wanSpeed > 0) {
                    speeds.insert(new Speed(mRouterId, System.currentTimeMillis(), mLanSpeed, wanSpeed));
                }
                break;
        }
    }

    private void notifyExtreme(int isExtreme, View view) {
        if (isExtreme==0) {
            view.setVisibility(View.INVISIBLE);
        } else if (isExtreme>0) {
            view.setVisibility(View.VISIBLE);
            ((TextView) view).setText(R.string.unusually_fast);
            ((TextView) view).setTextColor(Color.GREEN);
        } else if (isExtreme < 0) {
            view.setVisibility(View.VISIBLE);
            ((TextView) view).setText(R.string.unusually_slow);
            ((TextView) view).setTextColor(Color.RED);
        }
    }

}

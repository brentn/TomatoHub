package com.brentandjody.tomatohub;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.brentandjody.tomatohub.database.Speed;
import com.brentandjody.tomatohub.database.Speeds;
import com.brentandjody.tomatohub.routers.DDWrtRouter;
import com.brentandjody.tomatohub.routers.FakeRouter;
import com.brentandjody.tomatohub.routers.Router;
import com.brentandjody.tomatohub.routers.RouterType;
import com.brentandjody.tomatohub.routers.TomatoRouter;

public class SpeedTestActivity extends AppCompatActivity implements Router.OnRouterActivityCompleteListener {

    private static final int TEST_PORT = 4343;

    Router mRouter;
    TextView mInternetSpeed;
    TextView mWifiSpeed;
    ProgressBar mInternetTesting;
    ProgressBar mWifiTesting;
    long mStartTime;
    float mLanSpeed=-1;
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
        mStartTime = System.currentTimeMillis();
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
        float fileSize;
        float wanSpeed;
        switch(activity_id) {
            case Router.ACTIVITY_CONNECTED:
                runWifiTest();
                break;
            case Router.ACTIVITY_WIFI_SPEED_TEST:
                mWifiTesting.setVisibility(View.INVISIBLE);
                if (status==Router.ACTIVITY_STATUS_SUCCESS) {
                    mLanSpeed = mRouter.getConnectionSpeed();
                    String speed = String.format("%.2f", mLanSpeed);
                    mWifiSpeed.setText(speed + " Mbps");
                } else {
                    mLanSpeed=-1;
                    mWifiSpeed.setText(R.string.test_failed);
                }
                runDownloadTest();
                break;
            case Router.ACTIVITY_INTERNET_10MDOWNLOAD:
                mInternetTesting.setVisibility(View.INVISIBLE);
                if (status==Router.ACTIVITY_STATUS_SUCCESS) {
                    elapsedTime = System.currentTimeMillis() - mStartTime;
                    fileSize = (10485760 * 8) / 1000000; //adjust size to megabits
                    wanSpeed = fileSize / (elapsedTime / 1000F); //adjust time to seconds
                    mInternetSpeed.setText(String.format("%.2f", wanSpeed) + " Mbps");
                } else {
                    wanSpeed=-1;
                    mInternetSpeed.setText(R.string.test_failed);
                }
                if (mLanSpeed>0 || wanSpeed>0) {
                    speeds.insert(new Speed(mRouter.getRouterId(), System.currentTimeMillis(), mLanSpeed, wanSpeed));
                }
                break;
        }
    }
}

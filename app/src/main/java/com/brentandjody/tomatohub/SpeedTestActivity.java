package com.brentandjody.tomatohub;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.brentandjody.tomatohub.routers.DDWrtRouter;
import com.brentandjody.tomatohub.routers.LinuxRouter;
import com.brentandjody.tomatohub.routers.Router;
import com.brentandjody.tomatohub.routers.RouterType;
import com.brentandjody.tomatohub.routers.TomatoRouter;

public class SpeedTestActivity extends AppCompatActivity implements Router.OnRouterActivityCompleteListener {

    Router mRouter;
    TextView mInternetSpeed;
    TextView mWifiSpeed;
    ProgressBar mInternetTesting;
    ProgressBar mWifiTesting;
    long mStartTime;

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

        SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPreferences_name), MODE_PRIVATE);
        int router_type = RouterType.value(prefs.getString(getString(R.string.pref_key_router_type), RouterType.defaultValue));
        switch (router_type) {
            case RouterType.TOMATO: mRouter = new TomatoRouter(this, null, null); break;
            case RouterType.DDWRT: mRouter = new DDWrtRouter(this, null, null); break;
            default: mRouter = new LinuxRouter(this, null, null);
        }
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
            String url = mRouter.getUrlToTest();
            if (url.isEmpty()) {
                mWifiTesting.setVisibility(View.INVISIBLE);
                mWifiSpeed.setText(R.string.low_memory);
                runDownloadTest();
            } else {
                mRouter.wifiSpeedTest(url);
            }
        }
    }

    @Override
    public void onRouterActivityComplete(int activity_id, int status) {
        long elapsedTime;
        float fileSize;
        float Mbps;
        switch(activity_id) {
            case Router.ACTIVITY_LOGON:
                runWifiTest();
                break;
            case Router.ACTIVITY_WIFI_SPEED_TEST:
                mWifiTesting.setVisibility(View.INVISIBLE);
                if (status==Router.ACTIVITY_STATUS_SUCCESS) {
                    String speed = String.format("%.2f", mRouter.getSpeedTestResult());
                    mWifiSpeed.setText(speed + " Mbps");
                } else {
                    mWifiSpeed.setText(R.string.test_failed);
                }
                mRouter.command("rm /www/user/speedtest.txt");
                runDownloadTest();
                break;
            case Router.ACTIVITY_INTERNET_10MDOWNLOAD:
                mInternetTesting.setVisibility(View.INVISIBLE);
                if (status==Router.ACTIVITY_STATUS_SUCCESS) {
                    elapsedTime = System.currentTimeMillis() - mStartTime;
                    fileSize = (10485760 * 8) / 1000000; //adjust size to megabits
                    Mbps = fileSize / (elapsedTime / 1000F); //adjust time to seconds
                    mInternetSpeed.setText(String.format("%.2f", Mbps) + " Mbps");
                } else {
                    mInternetSpeed.setText(R.string.test_failed);
                }
                break;
        }
    }
}

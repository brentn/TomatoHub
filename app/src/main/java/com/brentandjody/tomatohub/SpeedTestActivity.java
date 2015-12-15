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
        mInternetTesting.setVisibility(View.VISIBLE);
        mWifiSpeed.setText("");
        mWifiTesting.setVisibility(View.VISIBLE);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
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
        mStartTime = System.currentTimeMillis();
        mRouter.internetSpeedTest();
    }

    private void runWifiTest() {
        mStartTime = System.currentTimeMillis();
        mRouter.transferBytes(10000000);
    }

    @Override
    public void onRouterActivityComplete(int activity_id, int status) {
        long elapsedTime;
        float fileSize;
        float Mbps;
        switch(activity_id) {
            case Router.ACTIVITY_LOGON:
                runDownloadTest();
                break;
            case Router.ACTIVITY_INTERNET_10MDOWNLOAD:
                elapsedTime = System.currentTimeMillis()-mStartTime;
                mInternetTesting.setVisibility(View.INVISIBLE);
                fileSize = (10485760*8)/1000000; //adjust size to megabits
                Mbps = fileSize/(elapsedTime/1000F); //adjust time to seconds
                mInternetSpeed.setText(String.format("%.2f", Mbps)+" Mbps");
                runWifiTest();
                break;
            case Router.ACTIVITY_TRANSFER_BYTES:
                elapsedTime = System.currentTimeMillis()-mStartTime;
                mWifiTesting.setVisibility(View.INVISIBLE);
                fileSize = 80; //size in megabits
                Mbps = fileSize/(elapsedTime/1000F); //adjust time to seconds
                mWifiSpeed.setText(String.format("%.2f", Mbps)+" Mbps");
                mRouter.disconnect();
        }
    }
}

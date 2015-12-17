package com.brentandjody.tomatohub.routers;

import android.content.Context;
import android.util.Log;

import com.brentandjody.tomatohub.MainActivity;
import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Networks;

/**
 * Created by brentn on 11/12/15.
 */
public class TomatoRouter extends LinuxRouter {
    private static final String TAG = TomatoRouter.class.getName();

    public TomatoRouter(Context context, Devices devices, Networks networks) {
        super(context, devices, networks);
    }

    @Override
    public String getUrlToTest() {
        try {
            int freemem = Integer.parseInt(command("free|grep Mem|awk '{print $4}'")[0]) / 1000;
            if (freemem > 11) {
                command("dd if=/dev/zero of=/www/user/speedtest.txt bs=1048576 count=10");
                return "http://" + mIpAddress + "/user/speedtest.txt?id=" + System.currentTimeMillis();
            } else if (freemem > 2) {
                command("dd if=/dev/zero of=/www/user/speedtest.txt bs=1048576 count=" + (freemem - 1)); //  don't overflow available memory
                return "http://" + mIpAddress + "/user/speedtest.txt?id=" + System.currentTimeMillis();
            } else {
                return "";
            }
        } catch (Exception ex) {
            Log.e(TAG, "internetSpeedTest: "+ex.getMessage() );
            return "";
        }
    }

    @Override
    public void cleanUpAfterTest() {
        command("rm /www/user/speedtest.txt");
    }
}

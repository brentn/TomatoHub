package com.brentandjody.tomatohub.routers;

import android.content.Context;
import android.util.Log;

import com.brentandjody.tomatohub.MainActivity;
import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Networks;

/**
 * Created by brentn on 11/12/15.
 */
public class DDWrtRouter extends LinuxRouter {
    private static final String TAG = DDWrtRouter.class.getName();

    public DDWrtRouter(Context context, Devices devices, Networks networks) {
        super(context, devices, networks);
    }

    @Override
    public String getUrlToTest() { return "http://"+mIpAddress+"/Info.htm"; }
}

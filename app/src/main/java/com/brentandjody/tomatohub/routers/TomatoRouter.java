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

}

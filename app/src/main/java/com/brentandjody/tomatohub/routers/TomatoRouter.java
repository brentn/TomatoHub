package com.brentandjody.tomatohub.routers;

import android.content.Context;

import com.brentandjody.tomatohub.MainActivity;
import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Networks;

/**
 * Created by brentn on 11/12/15.
 */
public class TomatoRouter extends LinuxRouter {
    public TomatoRouter(Context context, Devices devices, Networks networks) {
        super(context, devices, networks);
    }
}

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

    private Boolean mQOS=null;

    public TomatoRouter(Context context, Devices devices, Networks networks) {
        super(context, devices, networks);
    }

    @Override
    public boolean isQOSEnabled() {
        // returns true only if wshaper is enabled, and uplink/downlink values have been set
        if (mQOS==null) {
            mQOS = true;
            if (grep(cacheNVRam, "qos_enable=1").length == 0)
                mQOS = false;
            String[] uplink = grep(cacheNVRam, "qos_obw=");
            if (uplink.length == 0 || uplink[0].equals("qos_obw=") || uplink[0].equals("qos_obw=0"))
                mQOS = false;
            String[] downlink = grep(cacheNVRam, "qos_ibw=");
            if (downlink.length == 0 || downlink[0].equals("qos_ibw=") || downlink[0].equals("qos_ibw=0"))
                mQOS = false;
        }
        return mQOS;
    }


}

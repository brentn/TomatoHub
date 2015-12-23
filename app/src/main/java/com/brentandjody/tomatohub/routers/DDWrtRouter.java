package com.brentandjody.tomatohub.routers;

import android.content.Context;
import android.util.Log;

import com.brentandjody.tomatohub.MainActivity;
import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Networks;

/**
 * Created by brentn on 11/12/15.
 * Implenet DD-wrt router firmware
 */
public class DDWrtRouter extends LinuxRouter {
    private static final String TAG = DDWrtRouter.class.getName();

    private Boolean mQOS=null;

    public DDWrtRouter(Context context, Devices devices, Networks networks) {
        super(context, devices, networks);
    }

    @Override
    public boolean isQOSEnabled() {
        // returns true only if wshaper is enabled, and uplink/downlink values have been set
        if (mQOS==null) {
            mQOS = true;
            if (grep(cacheNVRam, "wshaper_enable=1").length == 0)
                mQOS = false;
            String[] uplink = grep(cacheNVRam, "wshaper_uplink=");
            if (uplink.length == 0 || uplink[0].equals("wshaper_uplink=") || uplink[0].equals("wshaper_uplink=0"))
                mQOS = false;
            String[] downlink = grep(cacheNVRam, "wshaper_downlink=");
            if (downlink.length == 0 || downlink[0].equals("wshaper_downlink=") || downlink[0].equals("wshaper_downlink=0"))
                mQOS = false;
        }
        return false;  //TODO: return mQOS when this feature is ready
    }

    @Override
    public void prioritize(String ip, long until) {
        //TODO:Not yet implemented
        super.prioritize(ip, until);
    }
}

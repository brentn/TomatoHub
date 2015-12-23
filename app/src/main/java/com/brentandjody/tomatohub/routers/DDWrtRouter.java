package com.brentandjody.tomatohub.routers;

import android.content.Context;

import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Networks;

import java.util.Calendar;

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
    public void initialize() {
        super.initialize();
        refreshCronCache();
    }

    @Override
    public boolean isQOSEnabled() {
        // returns true only if wshaper is enabled, and uplink/downlink values have been set
        if (mQOS==null) {
            mQOS = grep(cacheNVRam, "wshaper_enable=1").length != 0;
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
    protected boolean addQOSRule(String ip) {
        return super.addQOSRule(ip);
    }

    @Override
    protected void scheduleUndoQOSRule(String ip, Calendar timeToRevert) {
        super.scheduleUndoQOSRule(ip, timeToRevert);
    }

    private void refreshCronCache() {
        cacheCrond = command("cat /tmp/crontab/wrtHub");
    }

}

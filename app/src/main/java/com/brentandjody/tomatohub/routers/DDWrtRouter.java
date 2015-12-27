package com.brentandjody.tomatohub.routers;

import android.content.Context;
import android.util.Log;

import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Networks;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by brentn on 11/12/15.
 * Implenet DD-wrt router firmware
 */
public class DDWrtRouter extends LinuxRouter {
    private static final String TAG = DDWrtRouter.class.getName();
    private static final String CRONFILE = "/tmp/cron.d/wrtHub";

    private Boolean mQOS=null;

    public DDWrtRouter(Context context, Devices devices, Networks networks) {
        super(context, devices, networks);
    }

    @Override
    public void initialize() {
        command("touch "+CRONFILE);
        refreshCronCache();
        super.initialize();
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
        return mQOS;
    }

    @Override
    public boolean isPrioritized(String ip) {
        return grep(cacheNVRam, ip+"/32 10").length > 0;
    }

    @Override
    protected boolean addQOSRule(String ip) {
        try {
            String[] result = command("nvram get svqos_ips; echo $?");
            if (result[result.length - 1].equals("0")) {
                String original = result[0];
                if (original.length() > 1 && !original.endsWith("|"))
                    Log.w(TAG, "original rule does not end with |");
                else {
                    original = original.replace(" "+ip+"/32 10 |",""); //remove this ip if it is already there
                    String modified = original + " " + ip + "/32 10 |";
                    result = command("nvram set svqos_ips=\"" + modified + "\"; echo $?");
                    if (result[result.length - 1].equals("0")) {
                        cacheCrond = command("cat "+CRONFILE);
                        cacheNVRam = command("nvram show");
                        runInBackground("stopservice wshaper && startservice wshaper");
                        Log.i(TAG, "nvram successfully updated with new rules");
                        return true;
                    }
                }
            }
            Log.e(TAG, "ERROR setting QOS rules");
        } catch (Exception ex) {
            Log.e(TAG, "addQOSRule() "+ex.getMessage());
        }
        return false;
    }

    @Override
    protected void scheduleUndoQOSRule(String ip, Calendar when) {
        final String PATTERN = " "+ip+"/32 10 |";
        final String DELETE_SELF = "cat "+ CRONFILE +"|grep -v '"+PATTERN+"' > "+ CRONFILE;
        final String TEMPFILE = "/tmp/backup_cron";
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(when.getTimeInMillis());
        try {
            String undo = "nvram set svqos_ips=\\\"\\`/usr/sbin/nvram get svqos_ips|/bin/sed 's%"+PATTERN+"%%'\\`\\\"; /sbin/stopservice wshaper && /sbin/startservice wshaper";
            if (when.before(new Date())) return;
            Log.d(TAG, "Scheduling prioritization of " + ip + " to end at " + when.getTime());
            int min = utc.get(Calendar.MINUTE);
            int hour = utc.get(Calendar.HOUR_OF_DAY);
            int day = utc.get(Calendar.DAY_OF_MONTH);
            int month = utc.get(Calendar.MONTH) + 1;
            command(DELETE_SELF);
            //certain versions of ddwrt erase /etc/cron.d when stopping service
            command("cp "+CRONFILE+" "+TEMPFILE+"; stopservice cron; stopservice crond");
            command("echo \"" + min + " " + hour + " " + day + " " + month + " * root " + undo +"; "+ DELETE_SELF + " #"+PREFIX+ip+"#\" >> "+TEMPFILE);
            command("mkdir /tmp/cron.d; cp "+TEMPFILE+" "+CRONFILE+"; rm "+TEMPFILE+";");
            runInBackground("startservice cron; startservice crond");
            refreshCronCache();
        } catch (Exception ex) {
            Log.e(TAG, "scheduleNewQOSRule() "+ex.getMessage());
        }
    }

    private void refreshCronCache() {
        cacheCrond = command("cat "+ CRONFILE);
    }

}

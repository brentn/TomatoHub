package com.brentandjody.tomatohub.routers;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.brentandjody.tomatohub.database.Device;
import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Networks;
import com.brentandjody.tomatohub.database.Wifi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by brentn on 11/12/15.
 * Implenet DD-wrt router firmware
 */
public class DDWrtRouter extends LinuxRouter {
    private static final String TAG = DDWrtRouter.class.getName();

    private int timezone_adjust;
    private Boolean mQOS=null;

    public DDWrtRouter(Context context, Devices devices, Networks networks) {
        super(context, devices, networks);
    }

    @Override
    public void initialize() {
        refreshCronCache();
        super.initialize();
    }

    @Override
    public void reboot() {
        restore_from_backup();
        super.reboot();
    }

    @Override
    public void updateDevices() {
        try {
            String time_zone = grep(cacheNVRam, "time_zone")[0].split("=")[1];
            timezone_adjust = Integer.parseInt(time_zone)*60*60*1000; //adjust utc for time zone
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
            timezone_adjust=0;
        }
        super.updateDevices();
    }

    @Override
    public List<Wifi> getWifiList() {
        super.getWifiList();
        // find wifi passwords
        List<Wifi> result = new ArrayList<>();
        for (String ssid : mWifiIds) {
            Wifi wifi = new Wifi(ssid);
            try {
                String mode = "";
                String prefix = grep(cacheNVRam, "ssid=" + wifi.SSID())[0].split("_ssid")[0];
                // DD-Wrt replaces the . with an X in the security_mode parameter
                String[] items = grep(cacheNVRam, prefix.replace(".","X") + "_security_mode=");
                if (items.length > 0) {
                    mode = items[0].split("=")[1];
                }
                if (mode.contains("wpa")||mode.contains("psk")) {
                    items = grep(cacheNVRam, prefix + "_wpa_psk=");
                    if (items.length > 0)
                        wifi.setPassword(items[0].split("=")[1]);
                }
                wifi.setBroadcast(true);
                items = grep(cacheNVRam, prefix+"_closed=");
                if (items.length > 0) {
                    wifi.setBroadcast(items[0].equals(prefix + "_closed=0"));
                }
                wifi.setEnabled(true);
                items = grep(cacheNVRam, prefix+"_ifname=");
                if (items.length>0) {
                    String iface = items[0].split("=")[1];
                    String[] lines = grep(cacheIfconfig, iface+" ");
                    wifi.setEnabled(lines.length>0 && lines[0].substring(0, iface.length()+1).equals(iface+" "));
                }
                result.add(wifi);
            } catch (Exception ex) {
                Log.e(TAG, "Could not determine wifi password: "+ex.getMessage());
            }
        }
        return result;
    }

    @Override
    public void setWifiPassword(final Wifi wifi, final String newPassword) {
        String prefix = grep(cacheNVRam, "ssid=" + wifi.SSID())[0].split("_ssid")[0];
        final String key = prefix + "_wpa_psk";
        if (grep(cacheNVRam, key + "=").length > 0) {
            if (grep(cacheNVRam, key + "=")[0].split("=")[1].equals(wifi.password())) {
                if (!newPassword.isEmpty()) {
                    wifi.setPassword(newPassword);
                    command("nvram set " + key + "=\""+newPassword+"\"");
                    setCacheNVRam(key, newPassword);
                    runInBackground("nvram commit; rc start");
                    mListener.onRouterActivityComplete(Router.ACTIVITY_WIFI_UPDATED, ACTIVITY_STATUS_SUCCESS);
                    Log.d(TAG, "setWifiPassword() SUCCESS");
                }
            } else Log.w(TAG, "setWifiPassword(): Original password did not match");
        } else Log.w(TAG, "setWifiPassword(): Password not found in NVRam");
    }

    @Override
    public void broadcastWifi(String ssid, boolean broadcast) {
        if (grep(cacheNVRam, "ssid="+ssid).length>0) {
            String prefix = grep(cacheNVRam, "ssid=" + ssid)[0].split("_ssid")[0];
            String key = prefix+"_closed=";
            if (grep(cacheNVRam, key).length>0) {
                String value = (broadcast?"\"0\"":"\"1\"");
                command("nvram set " + key + value);
                setCacheNVRam(key, value);
                runInBackground("rc start");
                mListener.onRouterActivityComplete(Router.ACTIVITY_WIFI_UPDATED, ACTIVITY_STATUS_SUCCESS);
                Log.d(TAG, "broadcastWifi("+broadcast+") SUCCESS");
            } else Log.w(TAG, "broadcastWifi(): key not found in NVRam");
        }
    }

    @Override
    public void enableWifi(String ssid, boolean enabled) {
        if (grep(cacheNVRam, "ssid="+ssid).length>0) {
            String prefix = grep(cacheNVRam, "ssid=" + ssid)[0].split("_ssid")[0];
            String[] items = grep(cacheNVRam, prefix+"_ifname=");
            if (items.length>0) {
                String ifname = items[0].split("=")[1];
                command("ifconfig "+ifname+(enabled?" up":" down"));
                cacheIfconfig = command("ifconfig");
            } else { Log.w(TAG, "key "+prefix+"_ifname not found"); }
            mListener.onRouterActivityComplete(ACTIVITY_WIFI_UPDATED, ACTIVITY_STATUS_SUCCESS);
            Log.d(TAG, "enableWifi("+enabled+") SUCCESS");
        }
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
        return grep(grep(cacheNVRam, "svqos_ips="), ip+"/32 10").length > 0;
    }

    @Override
    public long isPrioritizedUntil(String ip) {
        if (isPrioritized(ip)) {
            String[] undo = grep(cacheCrond, PREFIX+ip);
            if (undo.length>0) {
                String[] fields = undo[0].split(" ");
                if (fields.length > 6) {
                    Calendar until = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    try {
                        int month = Integer.parseInt(fields[3]) - 1; //zero based
                        int day = Integer.parseInt(fields[2]);
                        int hour = Integer.parseInt(fields[1]);
                        int mins = Integer.parseInt(fields[0]);
                        int year = until.get(Calendar.YEAR);
                        until.set(year, month, day, hour, mins);
                        return until.getTimeInMillis()-timezone_adjust;
                    } catch (Exception ex) {
                        Log.e(TAG, "isPrioritizedUntil() "+ex.getMessage());
                        return Device.INDETERMINATE_PRIORITY;
                    }
                }
            }
            return Device.INDETERMINATE_PRIORITY;
        } else {
            return Device.NOT_PRIORITIZED;
        }
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
                    ensure_backup_exists();
                    result = command("nvram set svqos_ips=\"" + modified + "\"; echo $?");
                    setCacheNVRam("svqos_ips", modified);
                    if (result[result.length - 1].equals("0")) {
                        refreshCronCache();
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
        final String DELETE_CRON_JOB = "/usr/sbin/nvram set cron_jobs=\"`/usr/sbin/nvram get cron_jobs|/bin/grep -v '"+PREFIX+ip+"'`\"";
        try {
            ensure_backup_exists();
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(when.getTimeInMillis()+timezone_adjust);
            //String undo = "/usr/sbin/nvram set svqos_ips=\\\"\\`/usr/sbin/nvram get svqos_ips|/bin/sed 's%"+PATTERN+"%%'\\`\\\"; /sbin/stopservice wshaper; /sbin/startservice wshaper";
            if (when.before(new Date())) return;
            Log.d(TAG, "Scheduling prioritization of " + ip + " to end at " + when.getTime());
            int min = utc.get(Calendar.MINUTE);
            int hour = utc.get(Calendar.HOUR_OF_DAY);
            int day = utc.get(Calendar.DAY_OF_MONTH);
            int month = utc.get(Calendar.MONTH) + 1;
            String scriptName = "/tmp/unPrioritize"+ip;
            command(DELETE_CRON_JOB);
            command("echo '#!/bin/sh\n"
                    +"/usr/sbin/nvram set svqos_ips=\"`/usr/sbin/nvram get svqos_ips|/bin/sed \"s%"+PATTERN+"%%\"`\"\n"
                    +"/sbin/stopservice wshaper\n"
                    +"/sbin/startservice wshaper\n"
                    + DELETE_CRON_JOB.replace("'","\'")+"\n"
                    +"/sbin/stopservice cron; /sbin/stopservice crond\n"
                    +"/sbin/startservice cron; /sbin/startservice crond\n"
                    + "rm " + scriptName + "' > "+scriptName);
            command("chmod +x "+scriptName);

            String newJob = min + " " + hour + " " + day + " " + month + " * root " + scriptName + " #"+PREFIX+ip+"#";
            String[] oldJobs = command("nvram get cron_jobs");
            String newJobs = (oldJobs.length>0?TextUtils.join("\n", oldJobs)+"\n":"")+newJob;
            command("nvram set cron_jobs=\""+ newJobs + "\"");
            runInBackground("stopservice cron; stopservice crond; startservice cron; startservice crond");
            refreshCronCache();
        } catch (Exception ex) {
            Log.e(TAG, "scheduleNewQOSRule() "+ex.getMessage());
        }
    }

    private void refreshCronCache() {
        cacheCrond = command("nvram get cron_jobs");
    }

    private void ensure_backup_exists() {
        String key1 = PREFIX+"_svqos_ips";
        String key2 = PREFIX+"_cron_jobs";
        boolean changed=false;
        if (!Arrays.asList(cacheNVRam).contains(key1)) {
            command("nvram set "+key1+"=\"`nvram get svqos_ips`\"");
            changed=true;
        }
        if (!Arrays.asList(cacheNVRam).contains(key2)) {
            command("nvram set "+key2+"=\"`nvram get cron_jobs`\"");
            changed=true;
        }
        if (changed)
            Log.d(TAG, "Backed up unmodified QOS");
    }

    private void restore_from_backup() {
        String key1 = PREFIX+"_svqos_ips";
        String key2 = PREFIX+"_cron_jobs";
        if (Arrays.asList(cacheNVRam).contains(key1)) {
            command("nvram set svqos_ips=\"`nvram get "+key1+"`\"; nvram unset "+key1);
            Log.d(TAG, "Restoring unmodified QOS");
        }
        if (Arrays.asList(cacheNVRam).contains(key2)) {
            command("nvram set cron_jobs=\"`nvram get "+key2+"`\"; nvram unset "+key2);
        }
        refreshNVRamCache();
    }
}

package com.brentandjody.tomatohub.routers;

import android.content.Context;
import android.util.Log;

import com.brentandjody.tomatohub.database.Device;
import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Networks;
import com.brentandjody.tomatohub.database.Wifi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.DataFormatException;

/**
 * Created by brentn on 11/12/15.
 * Implement commands specific to Tomato firmware
 */
public class TomatoRouter extends LinuxRouter {
    private static final String TAG = TomatoRouter.class.getName();
    private static final String QOS_COMMENT = "wrtHub Prioritize ";
    private enum AddressType {ANY(0), DST_IP(1), SRC_IP(2), SRC_MAC(3);
        private int value;
        AddressType(int value) {this.value=value;}
    }
    private enum PortType {a, d, s, x}
    private enum Priority {DISABLED(-1), SERVICE(0), VOIPGame(1), MEDIA(2), REMOTE(3), WWW(4), MAIL(5), MESSENGER(6), FILEXFER(7), P2PBULK(8), CRAWL(9);
        private int value;
        Priority(int value) {this.value=value;}
    }

    private Boolean mQOS=null;

    public TomatoRouter(Context context, Devices devices, Networks networks) {
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
    public List<Wifi> getWifiList() {
        super.getWifiList();
        // find wifi passwords
        List<Wifi> result = new ArrayList<>();
        for (String ssid : mWifiIds) {
            Wifi wifi = new Wifi(ssid);
            try {
                String mode = "";
                String prefix = grep(cacheNVRam, "ssid=" + wifi.SSID())[0].split("_ssid")[0];
                if (grep(cacheNVRam, prefix + "_security_mode=").length > 0) {
                    mode = grep(cacheNVRam, prefix+"_security_mode=")[0].split("=")[1];
                }
                if (mode.contains("wpa")) {
                    if (grep(cacheNVRam, prefix + "_wpa_psk=").length > 0)
                        wifi.setPassword(grep(cacheNVRam, prefix + "_wpa_psk=")[0].split("=")[1]);
                }
                result.add(wifi);
            } catch (Exception ex) {
                Log.e(TAG, "Could not determine wifi password: "+ex.getMessage());
            }
        }
        return result;
    }

    @Override
    public boolean isQOSEnabled() {
        // returns true only if qos is enabled, and uplink/downlink values have been set
        if (mQOS==null) {
            mQOS = grep(cacheNVRam, "qos_enable=1").length != 0;
            String[] uplink = grep(cacheNVRam, "qos_obw=");
            if (uplink.length == 0 || uplink[0].equals("qos_obw=") || uplink[0].equals("qos_obw=0"))
                mQOS = false;
            String[] downlink = grep(cacheNVRam, "qos_ibw=");
            if (downlink.length == 0 || downlink[0].equals("qos_ibw=") || downlink[0].equals("qos_ibw=0"))
                mQOS = false;
        }
        return mQOS;
    }

    @Override
    public boolean isPrioritized(String ip) {
        try { return grep(cacheNVRam, incomingRule(ip).toString()).length>0; }
        catch (Exception ex) {return false;}
    }

    @Override
    public long isPrioritizedUntil(String ip) {
        if (isPrioritized(ip)) {
            String[] undo = grep(cacheCrond, PREFIX+ip);
            if (undo.length>0) {
                String[] fields = undo[0].split(" ");
                if (fields.length > 6) {
                    Calendar until = Calendar.getInstance();
                    try {
                        int month = Integer.parseInt(fields[3]) - 1; //zero based
                        int day = Integer.parseInt(fields[2]);
                        int hour = Integer.parseInt(fields[1]);
                        int mins = Integer.parseInt(fields[0]);
                        int year = until.get(Calendar.YEAR);
                        until.set(year, month, day, hour, mins);
                        return until.getTimeInMillis();
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
            List<QOSRule> rules = new LinkedList<>();
            String qos_orules;
            // Parse original string and ensure the format is correct.
            try {
                qos_orules = grep(cacheNVRam, "qos_orules")[0].split("=")[1];
                for (String orule : qos_orules.split(">")) {
                    rules.add(new QOSRule(orule));
                }

            } catch (Exception ex) {
                Log.e(TAG, "Error in original rule string: " + ex.getMessage());
                return false;
            }
            // add new rules
            //TODO: prevent adding duplicate rule
            ensure_backup_exists();
            String[] result = command("nvram set qos_orules=\"`nvram get qos_orules`>"+incomingRule(ip)+">"+outgoingRule(ip)+"\"; echo $?");
            if (result[result.length - 1].equals("0")) {
                cacheCrond = command("cru l");
                cacheNVRam = command("nvram show");
                runInBackground("service qos restart; echo $?");
                Log.i(TAG, "nvram successfully updated with new rules");
                return true;
            }
            Log.e(TAG, "ERROR SETTING NVRAM QOS_ORULES");
        } catch (Exception ex) {
            Log.e(TAG, "addQOSRule(): "+ex.getMessage());
        }
        return false;
    }

    @Override
    protected void scheduleUndoQOSRule(String ip, Calendar when) {
        final String DELETE_SELF = ";cru d "+PREFIX+ip;
        try {
            ensure_backup_exists();
            String undo = "/bin/nvram set qos_orules=\\\"\\`/bin/nvram get qos_orules|sed 's|>"+incomingRule(ip)+"||'|sed 's|>"+outgoingRule(ip)+"||'\\`\\\"; service qos restart";
            if (when.before(new Date())) return;
            Log.d(TAG, "Scheduling prioritization of " + ip + " to end at " + when);
            int min = when.get(Calendar.MINUTE);
            int hour = when.get(Calendar.HOUR_OF_DAY);
            int day = when.get(Calendar.DAY_OF_MONTH);
            int month = when.get(Calendar.MONTH) + 1;
            String[] output = command("cru d " + PREFIX + ip);
            Log.w(TAG, "schedule: " + output[0]);
            output = command("cru a " + PREFIX + ip + " \"" + min + " " + hour + " " + day + " " + month + " * " + undo + DELETE_SELF + "\"");
            Log.w(TAG, "schedule: " + output[0]);
            runInBackground("service crond restart");
            refreshCronCache();
        } catch (Exception ex) {
            Log.e(TAG, "scheduleNewQOSRule() "+ex.getMessage());
        }
    }

    private QOSRule outgoingRule(String ip) throws DataFormatException { return new QOSRule("1<" + ip + "<-1<a<<0<<<<1<" + QOS_COMMENT + ip);}
    private QOSRule incomingRule(String ip) throws DataFormatException { return new QOSRule("2<" + ip + "<-1<a<<0<<<<1<" + QOS_COMMENT + ip);}

    private void refreshCronCache() {
        cacheCrond = command("cru l");
    }

    private void ensure_backup_exists() {
        String key = PREFIX+"_qos_orules";
        if (! Arrays.asList(cacheNVRam).contains(key)) {
            command("nvram set "+key+"=\"`nvram get qos_orules`\"");
            cacheNVRam = command("nvram show");
            Log.d(TAG, "Backed up unmodified QOS");
        }
    }

    private void restore_from_backup() {
        String key = PREFIX+"_qos_orules";
        if (Arrays.asList(cacheNVRam).contains(key)) {
            command("nvram set qos_orules=\"`nvram get "+key+"`\"");
            command("nvram unset "+key);
            cacheNVRam = command("nvram show");
            Log.d(TAG, "Restoring unmodified QOS");
        }
    }

    class QOSRule {

        AddressType _addressType;
        String _address;
        int _protocol;
        PortType _portType;
        String _port;
        String _string1;
        String _string2;
        String _string3;
        String _string4;
        Priority _priority;
        String _comment;

        public QOSRule(String rule) throws DataFormatException{
            final int NUMBER_OF_FIELDS=11;
            if (rule.contains(">")) throw new DataFormatException("rule cannot contain '>' delimiter.");
            String[] fields = rule.split("<");
            if (fields.length!=NUMBER_OF_FIELDS) throw new DataFormatException("rule does not have "+NUMBER_OF_FIELDS+" fields, it has "+fields.length);
            try {
                _addressType = AddressType.values()[Integer.parseInt(fields[0])];
                _address = fields[1];
                _protocol = Integer.parseInt(fields[2]);
                _portType = PortType.valueOf(fields[3]);
                _port = fields[4];
                _string1 = fields[5];
                _string2 = fields[6];
                _string3 = fields[7];
                _string4 = fields[8];
                _priority = Priority.values()[Integer.parseInt(fields[9])+1];
                _comment = fields[10];
            } catch (NumberFormatException ex) {
                throw new DataFormatException("A number was incorrectly formatted: "+ex.getMessage());
            }
        }

        public String toString() {
            return _addressType.value+"<"
                    + _address+"<"
                    + _protocol+"<"
                    + _portType+"<"
                    + _port+"<"
                    + _string1+"<"+_string2+"<"+_string3+"<"+_string4+"<"
                    + _priority.value+"<"
                    + _comment;
        }
    }
}


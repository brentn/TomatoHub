package com.brentandjody.tomatohub.routers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.brentandjody.tomatohub.MainActivity;
import com.brentandjody.tomatohub.R;
import com.brentandjody.tomatohub.database.Device;
import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Networks;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.DataFormatException;

/**
 * Created by brentn on 11/12/15.
 */
public class TomatoRouter extends LinuxRouter {
    private static final String TAG = TomatoRouter.class.getName();
    private static final String QOS_COMMENT = "wrtHub Prioritize ";
    private static final String PREFIX = "wrtHub";
    private Boolean mQOS=null;
    private String[] cacheCrond;

    public TomatoRouter(Context context, Devices devices, Networks networks) {
        super(context, devices, networks);
    }

    @Override
    public void initialize() {
        super.initialize();
        cacheCrond = command("cru l");
    }

    @Override
    public boolean isQOSEnabled() {
        // returns true only if qos is enabled, and uplink/downlink values have been set
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

    @Override
    public long isPrioritizedUntil(String ip) {
        String[] lines = grep(cacheCrond, PREFIX+ip);
        if (lines.length==0) return Device.NOT_PRIORITIZED;
        String[] fields = lines[0].split(" ");
        try {
            Calendar timeToRevert = Calendar.getInstance();
            int year = Integer.parseInt(fields[5]);
            int month = Integer.parseInt(fields[3])-1; //zero based
            int day = Integer.parseInt(fields[2]);
            int hour = Integer.parseInt(fields[1]);
            int minute = Integer.parseInt(fields[0]);
            timeToRevert.set(year, month, day, hour, minute);
            return timeToRevert.getTimeInMillis();
        } catch (Exception ex) {
            Log.e(TAG, "isPrioritizedUntil() "+ex.getMessage());
            return Device.NOT_PRIORITIZED;
        }
    }

    @Override
    public void prioritize(String ip, long until) {
        try {
            Calendar timeToRevert = Calendar.getInstance();
            timeToRevert.setTimeInMillis(until);
            Log.d(TAG, "Prioritizing "+ip+" until "+timeToRevert.toString());
            SharedPreferences prefs = mContext.getSharedPreferences(mContext.getString(R.string.sharedPreferences_name), Context.MODE_PRIVATE);
            if (prefs.getBoolean(mContext.getString(R.string.pref_key_allow_changes), false)) {
                List<QOSRule> rules = new LinkedList<>();
                String qos_orules = "";
                // Parse original string and ensure the format is correct.
                try {
                    qos_orules = grep(cacheNVRam, "qos_orules")[0].split("=")[1];
                    for (String orule : qos_orules.split(">")) {
                        rules.add(new QOSRule(orule));
                    }

                } catch (Exception ex) {
                    Log.e(TAG, "Error in original rule string: " + ex.getMessage());
                    return;
                }
                QOSRule outgoingRule = new QOSRule("1<" + ip + "/32<-1<a<<0<<<<1<" + QOS_COMMENT + ip);
                QOSRule incomingRule = new QOSRule("2<" + ip + "/32<-1<a<<0<<<<1<" + QOS_COMMENT + ip);
                rules.add(outgoingRule);
                rules.add(incomingRule);
                StringBuilder new_orules = new StringBuilder();
                for (QOSRule rule : rules) {
                    new_orules.append(rule.toString() + ">");
                }
                if (!new_orules.toString().contains(qos_orules)) {
                    Log.e(TAG, "Error in new rule string");
                    return;
                }
                String[] result = command("nvram set qos_orules=" + new_orules.toString() + ";echo $?");
                if (result[result.length - 1].equals("0")) {
                    result = command("nvram commit; echo $0");
                    if (result[result.length - 1].equals("0")) {
                        result = command("service qos restart; echo $0");
                        if (result[result.length - 1].equals(0)) {
                            new Initializer().execute(); //re-read nvram, etc.
                            Log.i(TAG, "nvram successfully updated with new rules");
                            String undo = "x=`nvram get qos_orules`;y=`echo ${x/>"+incomingRule+"/}`;"+
                                        "z=`echo ${y/>"+outgoingRule+"/}`;nvram set qos_orules=$z;"+
                                        "nvram commit; service qos restart";
                            schedule(ip, undo, timeToRevert);
                            return;
                        }
                    }
                }
                Log.e(TAG, "ERROR SETTING NVRAM QOS_ORULES");
            }
        } catch (Exception ex) {
            Log.e(TAG, "prioritize: "+ex.getMessage());
        }
    }

    private void schedule(String ip, String command, Calendar when) {
        final String DELETE_SELF = ";cru d "+PREFIX+ip;
        if (when.before(new Date())) return;
        Log.d(TAG, "Scheduling prioritization of "+ip+" to end at "+when);
        int min = when.get(Calendar.MINUTE);
        int hour = when.get(Calendar.HOUR_OF_DAY);
        int day = when.get(Calendar.DAY_OF_MONTH);
        int month = when.get(Calendar.MONTH)+1;
        int year = when.get(Calendar.YEAR);
        String[] output = command("cru d "+PREFIX+ip);
        Log.w(TAG, "schedule: "+output[0]);
        output = command("cru a "+PREFIX+ip+" \""+min+" "+hour+" "+day+" "+month+" * "+year+" "+command+DELETE_SELF+"\"");
        Log.w(TAG, "schedule: "+output[0]);
        command("service crond restart");
    }
}

class QOSRule {
    enum AddressType {ANY(0), DST_IP(1), SRC_IP(2), SRC_MAC(3);
        private int value;
        AddressType(int value) {this.value=value;}
    };
    enum PortType {a, d, s, x};
    enum Priority {DISABLED(-1), SERVICE(0), VOIPGame(1), MEDIA(2), REMOTE(3), WWW(4), MAIL(5), MESSENGER(6), FILEXFER(7), P2PBULK(8), CRAWL(9);
        private int value;
        Priority(int value) {this.value=value;}
    }

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
        String result = _addressType.value+"<"
            + _address+"<"
            + _protocol+"<"
            + _portType+"<"
            + _port+"<"
            + _string1+"<"+_string2+"<"+_string3+"<"+_string4+"<"
            + _priority.value+"<"
            + _comment;
        return result;
    }
}
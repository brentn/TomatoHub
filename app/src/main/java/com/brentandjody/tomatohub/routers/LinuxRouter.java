package com.brentandjody.tomatohub.routers;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.brentandjody.tomatohub.MainActivity;
import com.brentandjody.tomatohub.R;
import com.brentandjody.tomatohub.database.Device;
import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Network;
import com.brentandjody.tomatohub.database.Networks;
import com.brentandjody.tomatohub.database.Wifi;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by brent on 28/11/15.
 * Commands to get results from Tomato Firmware
 */
public class LinuxRouter extends Router {

    private static final String TAG = LinuxRouter.class.getName();

    private Devices mDevicesDB = null;
    private Networks mNetworksDB = null;
    private String mRouterId;
    private long mBootTime;
    private int[] mCPUUsage;
    private int mMemoryUsage;
    private String mExternalIP;
    private String[] mNetworkIds;
    private String[] mWifiIds;
    private String[] cacheNVRam;
    private String[] cacheArp;
    private String[] cacheBrctl;
    private String[] cacheWf;

    public LinuxRouter(MainActivity context, Devices devices, Networks networks) {
        super(context);
        mDevicesDB=devices;
        mNetworksDB=networks;
    }

    @Override
    public void initialize() {
        mRouterId=null;
        mNetworkIds=null;
        mWifiIds=null;
        cacheNVRam=null;
        cacheBrctl=null;
        cacheWf=null;
        new Initializer().execute();
    }

    @Override
    public void connect() {
        switch (mPrefs.getString(mContext.getString(R.string.pref_key_protocol), "ssh")) {
            case "ssh":
                new SSHLogon().execute();
                break;
            case "telnet":
                new TelnetLogon().execute();
                break;
        }
    }

    @Override
    public void updateDevices() {
        new DeviceUpdater().execute();
    }
    @Override
    public void updateTrafficStats() {
        new NetworkTrafficAnalyzer().execute();
    }
    @Override
    public String getRouterId() {
        if (mRouterId==null) {
            String[] result = grep(cacheNVRam, "wan_hwaddr");
            if (result.length > 0 && result[0].contains("=")) mRouterId = result[0].split("=")[1];
        }
        return mRouterId;
    }
    @Override
    public long getBootTime() {
        return mBootTime;
    }

    @Override
    public int getMemoryUsage() {
        return mMemoryUsage;
    }

    @Override
    public int[] getCPUUsage() {
        return mCPUUsage;
    }

    @Override
    public String getExternalIP() {
        if (mExternalIP==null) {
            String[] result = grep(cacheNVRam, "wan_ipaddr");
            if (result.length > 0 && result[0].contains("=")) mExternalIP = result[0].split("=")[1];
        }
        return mExternalIP;
    }

    @Override
    public List<Wifi> getWifiList() {
        if (mWifiIds==null) {
            mWifiIds = new String[cacheWf.length];
            Pattern p = Pattern.compile("\"([^\"]*)\"");
            for (int i=0; i< cacheWf.length; i++) {
                Matcher m = p.matcher(cacheWf[i]);
                if (m.find()) mWifiIds[i] = m.group(1);
                else mWifiIds[i] = "<unknown>";
            }
        }
        List<Wifi> result = new ArrayList<>();
        for (String ssid : mWifiIds) {
            result.add(new Wifi(ssid));
        }
        return result;
    }
    @Override
    public String[] getNetworkIds() {
        if (mNetworkIds == null) {
            List<String> list = new ArrayList<>();
            for (String line:cacheBrctl) {
                if (line.contains("\t") && line.charAt(0)!='\t' && !line.startsWith("bridge name"))
                    list.add(line.split("\t")[0]);
            }
            mNetworkIds = list.toArray(new String[list.size()]);
        }
        return mNetworkIds;
    }
    @Override
    public int getTotalDevices() {
        int total=0;
        for (String network_id : getNetworkIds()) {
            total += getTotalDevicesOn(network_id);
        }
        return total;
    }

    @Override
    public int getTotalDevicesOn(String network_id) {
        int total=0;
        for (String line : cacheArp) {
            String[] fields = line.split(" ");
            if (fields.length>6 && fields[fields.length-1].equals(network_id)) {
                total++;
            }
        }
        return total;
    }

    private String[] grep(String[] lines, String pattern) {
        if (lines==null || lines.length==0) return new String[0];
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            if (line.contains(pattern)) result.add(line);
        }
        return result.toArray(new String[result.size()]);
    }

    private class SSHLogon extends Router.SSHLogon {
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mListener.onRouterActivityComplete(ACTIVITY_LOGON, success?ACTIVITY_STATUS_SUCCESS:ACTIVITY_STATUS_FAILURE);
        }
    }

    private class TelnetLogon extends Router.TelnetLogon {
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mListener.onRouterActivityComplete(ACTIVITY_LOGON, success?ACTIVITY_STATUS_SUCCESS:ACTIVITY_STATUS_FAILURE);
        }
    }


    private class Initializer extends AsyncTask<Void, Void, Void> {
        // Initialize Caches
        boolean success;
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                cacheNVRam = command("nvram show");
                cacheArp = command("arp");
                cacheBrctl = command("brctl show");
                cacheWf = command("for x in 0 1 2 3 4 5 6 7; do wl ssid -C $x 2>/dev/null; done");
                try {mBootTime = Long.parseLong(command("cat /proc/stat | grep btime | awk '{ print $2 }'")[0]); }
                catch (Exception ex){mBootTime = -1;}
                refreshLoadAverages();
                refreshMemoryStats();
                success=true;
            } catch(Exception ex) {
                success=false;
                Log.e(TAG, "QuickScan:"+ ex.getMessage()+TextUtils.join("\n", ex.getStackTrace()));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mListener.onRouterActivityComplete(ACTIVITY_INTIALIZE, success?ACTIVITY_STATUS_SUCCESS:ACTIVITY_STATUS_FAILURE);
        }
    }

    private class DeviceUpdater extends AsyncTask<Void, Void, Void> {
        // ensures all devices are in database
        // marks current devices as active
        boolean success;
        @Override
        protected Void doInBackground(Void... params) {
            try {
                mDevicesDB.inactivateAll();
                for (String network : getNetworkIds()) {
                    for (String line : grep(cacheArp, network)) {
                        String[] fields = line.split(" ");
                        // arp: android-acd667fdce64f7f1 (192.168.8.117) at C4:43:8F:F4:C4:C3 [ether]  on br0
                        if (fields.length>6 && fields[fields.length-1].equals(network)) {
                            String mac = line.split(" ")[3];
                            String ip = line.split(" ")[1].replaceAll("[()]", "");
                            Device device = mDevicesDB.get(getRouterId(), mac);
                            device.setOriginalName(line.split(" ")[0]);
                            device.setCurrentNetwork(network);
                            device.setCurrentIP(ip);
                            device.setActive(true);
                            mDevicesDB.insertOrUpdate(device);
                        }
                    }
                }
                success=true;
            } catch (Exception ex) {
                success=false;
                Log.e(TAG, ex.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mListener.onRouterActivityComplete(ACTIVITY_DEVICES_UPDATED, success?ACTIVITY_STATUS_SUCCESS:ACTIVITY_STATUS_FAILURE);
        }
    }

    private class NetworkTrafficAnalyzer extends AsyncTask<Void, Void, Void> {
        boolean success;
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                String[] ipTraffic = command("cat /proc/net/ipt_account/*|cut -d' ' -f3,6,20|grep -v ' 0 0$'"); //
                long timestamp = System.currentTimeMillis() / 1000L;
                for (String network_id : getNetworkIds()) {
                    float network_traffic = 0;
                    for (String line : grep(cacheArp, network_id)) {
                        String[] fields = line.split(" ");
                        if (fields.length>6) {
                            try {
                                String ip = fields[1].replaceAll("[()]", "");
                                String mac = fields[3];
                                String stats = grep(ipTraffic, ip)[0];
                                long tx = Long.parseLong(stats.split(" ")[1]);
                                long rx = Long.parseLong(stats.split(" ")[2]);
                                Device device = mDevicesDB.get(getRouterId(), mac);
                                device.setTrafficStats(tx, rx, timestamp);
                                mDevicesDB.insertOrUpdate(device);
                                network_traffic += device.lastSpeed();
                            } catch (Exception ex) {
                                Log.w(TAG, ex.getMessage());
                                //continue to next item
                            }
                        }
                    }
                    Network network = mNetworksDB.get(getRouterId(), network_id);
                    network.setSpeed(network_traffic);
                    mNetworksDB.insertOrUpdate(network);
                }
                refreshMemoryStats();
                refreshLoadAverages();
                success=true;
            } catch (Exception ex) {
                success=false;
                Log.e(TAG, ex.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mListener.onRouterActivityComplete(ACTIVITY_TRAFFIC_UPDATED, success?ACTIVITY_STATUS_SUCCESS:ACTIVITY_STATUS_FAILURE);
        }
    }

    private void refreshMemoryStats() {
        try {
            String[] mem = command("grep Mem /proc/meminfo |awk '{ print $2 }'");
            mMemoryUsage = Math.round(Float.parseFloat(mem[1])/Float.parseFloat(mem[0])*100);
        } catch (Exception ex) {
            mMemoryUsage = 0;
        }
    }

    private void refreshLoadAverages() {
        try {
            String[] load = command("cores=`grep -c processor /proc/cpuinfo`;for load in `cat /proc/loadavg|cut -d' ' -f1-3`; do echo $load $cores | awk '{print $1 * 100 / $2}'; done");
            mCPUUsage = new int[] { Math.round(Float.parseFloat(load[0])),
                    Math.round(Float.parseFloat(load[1])),
                    Math.round(Float.parseFloat(load[2]))};
        } catch (Exception ex) {
            mCPUUsage = new int[] {0,0,0};
        }
    }
}

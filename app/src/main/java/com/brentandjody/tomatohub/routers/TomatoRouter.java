package com.brentandjody.tomatohub.routers;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.brentandjody.tomatohub.MainActivity;
import com.brentandjody.tomatohub.database.Device;
import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Network;
import com.brentandjody.tomatohub.database.Networks;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brent on 28/11/15.
 * Commands to get results from Tomato Firmware
 */
public class TomatoRouter extends Router {

    private static final String TAG = TomatoRouter.class.getName();

    private Devices mDevicesDB = null;
    private Networks mNetworksDB = null;
    private String mRouterId;
    private String mExternalIP;
    private String[] mNetworkIds;
    private String[] mWifiIds;
    private String[] cacheNVRam;
    private String[] cacheArp;
    private String[] cacheBrctl;
    private String[] cacheWf;

    public TomatoRouter(MainActivity context, Devices devices, Networks networks) {
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
        new SSHLogon().execute();
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
    public String getExternalIP() {
        if (mExternalIP==null) {
            String[] result = grep(cacheNVRam, "wan_ipaddr");
            if (result.length > 0 && result[0].contains("=")) mExternalIP = result[0].split("=")[1];
        }
        return mExternalIP;
    }

    @Override
    public String[] getWIFILabels() {
        if (mWifiIds==null) {
            mWifiIds = new String[cacheWf.length];
            for (int i = 0; i < cacheWf.length; i++) {
                mWifiIds[i] = cacheWf[i].split("\"")[1].replace("\"", "");
            }
        }
        return mWifiIds;
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
        return grep(cacheArp, network_id).length;
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

    private class Initializer extends AsyncTask<Void, Void, Void> {
        // Initialize Caches
        boolean success;
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                cacheNVRam = sshCommand("nvram show");
                cacheArp = sshCommand("arp");
                cacheBrctl = sshCommand("brctl show");
                cacheWf = sshCommand("for x in 0 1 2 3 4 5 6 7; do wl ssid -C $x 2>/dev/null; done");
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
                        if (fields.length>7 && fields[7].equals(network)) {
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
                String[] ipTraffic = sshCommand("cat /proc/net/ipt_account/*|cut -d' ' -f3,6,20|grep -v ' 0 0$'"); //
                long timestamp = System.currentTimeMillis() / 1000L;
                for (String network_id : getNetworkIds()) {
                    float network_traffic = 0;
                    for (String line : grep(cacheArp, network_id)) {
                        try {
                            String ip = line.split(" ")[1].replaceAll("[()]", "");
                            String mac = line.split(" ")[3];
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
                    Network network = mNetworksDB.get(getRouterId(), network_id);
                    network.setSpeed(network_traffic);
                    mNetworksDB.insertOrUpdate(network);
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
            mListener.onRouterActivityComplete(ACTIVITY_TRAFFIC_UPDATED, success?ACTIVITY_STATUS_SUCCESS:ACTIVITY_STATUS_FAILURE);
        }
    }

}

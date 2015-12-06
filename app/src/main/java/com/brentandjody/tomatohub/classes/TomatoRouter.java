package com.brentandjody.tomatohub.classes;

import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.brentandjody.tomatohub.MainActivity;
import com.brentandjody.tomatohub.R;
import com.brentandjody.tomatohub.WelcomeActivity;
import com.brentandjody.tomatohub.database.Device;
import com.brentandjody.tomatohub.database.Devices;

/**
 * Created by brent on 28/11/15.
 * Commands to get results from Tomato Firmware
 */
public class TomatoRouter extends Router {

    private static final String TAG = TomatoRouter.class.getName();

    private boolean mStartActivityHasBeenRun;
    private String mRouterId;
    private String[] mNetworks = new String[0];
    private String[] mWifi;
    private String[][] mDevices;
    private Devices mDevicesDB = null;

    public TomatoRouter(MainActivity context) {
        super(context);
        mStartActivityHasBeenRun=false;
    }

    @Override
    public void connect() {
        new SSHLogon().execute();
    }
    @Override
    public void disconnect() {
        super.disconnect();
    }
    // COMMANDS

    @Override
    //router id is WAN MAC
    public String getRouterId() {
        String result = "";
        try {
            String[] response = sshCommand("arp|grep `nvram show|grep wan_iface|cut -d= -f2`|cut -d' ' -f4");
            if (response.length>0) result = response[0];
        } catch (Exception ex) {
            Log.e(TAG, "Error getting router ID");
        }
        return result;
    }
    @Override
    public Devices getDevicesDB() {return mDevicesDB;}
    @Override
    public String[] getNetworkIds() { return mNetworks; }

    // lookup values on router
    @Override
    public String lookupWANInterface() {
        String result = "none";
        try {
            String[] response = sshCommand("nvram show|grep wan_iface|cut -d= -f2");
            if (response.length>0) result=response[0];
        }
        catch (Exception ex) { Log.e(TAG, "Error getting WAN interface"); return ""; }
        return result;
    }
    @Override
    public String[] lookupLANInterfaces() {
        return sshCommand("arp|cut -d' ' -f8|sort -u|grep -v `nvram show|grep wan_iface|cut -d= -f2`");
    }
    @Override
    public String[] lookupWIFILabels() {
        return sshCommand(" for x in 0 1 2 3 4 5 6 7; do wl ssid -C $x 2>/dev/null|cut -d' ' -f3-|tr -d '\"'; done");
    }
    @Override
    public String[] lookupConnectedDevices(String network) {
        return sshCommand("arp|grep "+network);
    }
    @Override
    public int lookupTxTrafficForIP(String ip) {
        int result;
        try {result = Integer.parseInt(sshCommand("grep ' "+ip+" ' /proc/net/ipt_account/*|cut -d' ' -f6")[0]); }
        catch (Exception ex) { Log.w(TAG, "Error getting tx traffic"); result= -1;}
        return result;
    }
    @Override
    public int lookupRxTrafficForIP(String ip) {
        int result;
        try {result = Integer.parseInt(sshCommand("grep ' "+ip+" ' /proc/net/ipt_account/*|cut -d' ' -f20")[0]); }
        catch (Exception ex) { Log.w(TAG, "Error getting tx traffic"); result= -1;}
        return result;
    }

    @Override
    public int lookupRxTrafficForNetwork(String ip) {
        int result;
        ip=ip.substring(0,ip.lastIndexOf('.'));
        try {result = Integer.parseInt(sshCommand("grep ' "+ip+" ' /proc/net/ipt_account/*|cut -d' ' -f6|awk '{s+=$1} END {print s}'")[0]); }
        catch (Exception ex) { Log.w(TAG, "Error getting tx traffic"); result= -1;}
        return result;
    }

    @Override
    public int lookupTxTrafficForNetwork(String ip) {
        int result;
        ip=ip.substring(0,ip.lastIndexOf('.'));
        try {result = Integer.parseInt(sshCommand("grep ' "+ip+" ' /proc/net/ipt_account/*|cut -d' ' -f20|awk '{s+=$1} END {print s}'")[0]); }
        catch (Exception ex) { Log.w(TAG, "Error getting tx traffic"); result= -1;}
        return result;
    }

    private long currentTime() { return System.currentTimeMillis()/1000L;}

    private class SSHLogon extends Router.SSHLogon
    {

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                mContext.setWifiMessage("");
                mContext.setDevicesMessage("", "");
                mContext.hideAllIcons();
                if (success) {
                    mContext.showIcon(R.id.router, true);
                    mContext.showIcon(R.id.router_l, true);
                    mContext.addIconLabel(R.id.router, mContext.getString(R.string.router));
                    mContext.setStatusMessage(mContext.getString(R.string.scanning_network));
                    new QuickScan().execute();
                } else {
                    launchWelcomeActivityOrFail();
                }
            } catch (Exception ex) {
                Log.e(TAG, "SSHLogon.postExecute:"+ex.getMessage());
                launchWelcomeActivityOrFail();
            }
        }
    }

    private void launchWelcomeActivityOrFail() {
        if (!mStartActivityHasBeenRun) {
            mStartActivityHasBeenRun = true;
            Log.i(TAG, "Redirecting to Welcome screen");
            Intent intent = new Intent(mContext, WelcomeActivity.class);
            intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
            mContext.startActivity(intent);
        } else {
            mContext.setStatusMessage(mContext.getString(R.string.connection_failure));
        }
    }

    private class QuickScan extends AsyncTask<Void, Void, Void> {

        boolean success=false;
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                mRouterId = getRouterId();
                // identify networks
                Log.d(TAG, "identify networks");
                mNetworks = lookupLANInterfaces();
                // identify various wifi networks
                Log.d(TAG, "identify wifi");
                mWifi = lookupWIFILabels();
                // enumerate devices on each network
                Log.d(TAG, "enumerate network devices");
                mDevices = new String[mNetworks.length][];
                for (int i = 0; i < mNetworks.length; i++) {
                    mDevices[i] = lookupConnectedDevices(mNetworks[i]);
                }
                success=true;
            } catch(Exception ex) {
                Log.e(TAG, "QuickScan:"+ ex.getMessage()+TextUtils.join("\n", ex.getStackTrace()));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mDevicesDB = new Devices(mContext, mRouterId);
            int[] icons = new int[] {R.id.lan_0,R.id.lan_1,R.id.lan_2,R.id.lan_3,R.id.lan_4};
            try {
                if (success) {
                    int total = 0;
                    mContext.initializeNetworks();
                    for (int i = 0; i < 5; i++) {
                        if (mNetworks != null && i < mNetworks.length) {
                            total += mDevices[i].length;
                            mContext.addIconLabel(icons[i], mNetworks[i]);
                            mContext.setNetworkText(i, String.valueOf(mDevices[i].length));
                        } else {
                            mContext.hideNetwork(i);
                        }
                    }
                    mContext.setStatusMessage(mContext.getString(R.string.everything_looks_good));
                    mContext.setDevicesMessage(String.valueOf(total) + mContext.getString(R.string.devices), mContext.getString(R.string.are_connected));
                    mContext.setWifiMessage("'"+TextUtils.join("'"+mContext.getString(R.string.is_on)+",  '", mWifi) + "'" + mContext.getString(R.string.is_on));
                    if (mDevices!=null) {
                        new DeviceScan().execute(mDevices);
                    }
                } else {
                    mContext.setStatusMessage(mContext.getString(R.string.scan_failure));
                }
            } catch (Exception ex) {
                Log.e(TAG, "QuickScan.postExecute:"+ex.getMessage());
            }
        }
    }

    private class DeviceScan extends AsyncTask<String[], Void, Void> {

        @Override
        protected Void doInBackground(String[]... networkDevices) {
            try {
                Devices devices = mDevicesDB;
                devices.inactivateAll();
                // update device data
                for (String[] deviceLines : networkDevices) {
                    for (String line : deviceLines) {
                        String[] fields = line.split(" ");
                        String name = (fields.length > 0 ? fields[0] : "");
                        String ip = (fields.length > 1 ? fields[1].replace("(","").replace(")","") : "");
                        String mac = (fields.length > 3 ? fields[3] : "");
                        String nwk = (fields.length > 7 ? fields[7] : "");
                        if (mac.length() == 17) {
                            Device device = devices.get(mac);
                            device.setCurrentNetwork(nwk);
                            device.setOriginalName(name);
                            device.setActive(true);
                            if (!ip.isEmpty())
                                device.setCurrentIP(ip);
                                long tx = lookupTxTrafficForIP(ip);
                                long rx = lookupRxTrafficForIP(ip);
                                device.setTrafficStats(tx, rx, currentTime());
                            devices.insertOrUpdate(device);
                        }
                    }
                }
                // update network data
                for (int i=0; i<mNetworks.length; i++) {
                    String id = mNetworks[i];
                }
            } catch(Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mContext.onNetworkScanComplete();
        }
    }

}

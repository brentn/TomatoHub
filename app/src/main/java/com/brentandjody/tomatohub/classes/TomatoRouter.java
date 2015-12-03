package com.brentandjody.tomatohub.classes;

import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.brentandjody.tomatohub.MainActivity;
import com.brentandjody.tomatohub.R;
import com.brentandjody.tomatohub.WelcomeActivity;

/**
 * Created by brent on 28/11/15.
 */
public class TomatoRouter extends Router {

    private static final String TAG = TomatoRouter.class.getName();

    private boolean mStartActivityHasBeenRun;
    private String mWAN="";
    private String[] mNetworks;
    private String[] mWifi;
    private String[][] mDevices;

    public TomatoRouter(MainActivity context) {
        super(context);
        mStartActivityHasBeenRun=false;
    }

    @Override
    public void connect() {
        new SSHLogon().execute();
    }

    // COMMANDS
    @Override
    public String getWANInterface() {
        try {
            String[] result = sshCommand("nvram show|grep wan_iface|cut -d= -f2");
            if (result.length==0) return "";
            return result[0];
        }
        catch (Exception ex) { Log.e(TAG, "Error getting WAN interface"); return ""; }
    }
    @Override
    public String[] getLANInterfaces() {
        if (mWAN.isEmpty()) mWAN = getWANInterface();
        String[] result = sshCommand("arp|cut -d' ' -f8|sort -u|grep -v "+mWAN);
        return result;
    }
    @Override
    public String[] getWIFILabels() { return sshCommand("nvram show|grep _ssid|cut -d= -f2");}
    @Override
    public String[] getConnectedDevices(String network) {return sshCommand("arp|grep -v "+network);}
    @Override
    public int getTxTrafficForIP(String ip) {
        try {return Integer.parseInt(sshCommand("grep "+ip+" /proc/net/ipt_account/*|cut -d' ' -f6")[0]); }
        catch (Exception ex) { Log.w(TAG, "Error getting tx traffic"); return 0;}
    }
    @Override
    public int getRxTrafficForIP(String ip) {
        try {return Integer.parseInt(sshCommand("grep "+ip+" /proc/net/ipt_account/*|cut -d' ' -f20")[0]); }
        catch (Exception ex) { Log.w(TAG, "Error getting tx traffic"); return 0;}
    }

    private long currentTime() { return System.currentTimeMillis()/1000L;}

    private class SSHLogon extends Router.SSHLogon
    {

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                mContext.hideAllIcons();
                if (success) {
                    mContext.showIcon(R.id.router, success);
                    mContext.showIcon(R.id.router_l, success);
                    mContext.addIconLabel(R.id.router, mContext.getString(R.string.router));
                    mContext.setStatusMessage(mContext.getString(R.string.scanning_network));
                    new ValueInitializer().execute();
                } else {
                    if (!mStartActivityHasBeenRun) {
                        mStartActivityHasBeenRun = true;
                        Log.i(TAG, "Redirecting to Welcome screen");
                        mContext.startActivity(new Intent(mContext, WelcomeActivity.class));
                    } else {
                        mContext.setStatusMessage(mContext.getString(R.string.connection_failure));
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "SSHLogon.postExecute:"+ex.getMessage());
            }
        }
    }

    private class ValueInitializer extends AsyncTask<Void, Void, Void> {

        boolean success=false;
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                // identify various networks
                Log.d(TAG, "identify networks");
                mNetworks = getLANInterfaces();
                // identify various wifi networks
                Log.d(TAG, "identify wifi");
                mWifi = getWIFILabels();
                // enumerate devices on each network
                Log.d(TAG, "enumerate network devices");
                mDevices = new String[mNetworks.length][];
                for (int i = 0; i < mNetworks.length; i++) {
                    mDevices[i] = getConnectedDevices(mNetworks[i]);
                }
                success=true;
            } catch(Exception ex) {
                Log.e(TAG, "valueInitializer:"+ ex.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                if (success) {
                    int total = 0;
                    for (int i = 0; i < 5; i++) {
                        if (mNetworks != null && i < mNetworks.length) {
                            total += mDevices[i].length;
                            mContext.setNetworkText(i, String.valueOf(mDevices[i].length));
                        } else {
                            mContext.hideNetwork(i);
                        }
                    }
                    mContext.setStatusMessage(mContext.getString(R.string.everything_looks_good));
                    mContext.setDevicesMessage(String.valueOf(total) + mContext.getString(R.string.devices), mContext.getString(R.string.are_connected));
                    mContext.setWifiMessage("'" + TextUtils.join("' is ON,  '", mWifi) + "' is ON");
                    if (mDevices!=null) {
                        new DeviceStatusUpdater().execute(mDevices);
                    }
                } else {
                    mContext.setStatusMessage(mContext.getString(R.string.scan_failure));
                }
            } catch (Exception ex) {
                Log.e(TAG, "ValueInitializer.postExecute:"+ex.getMessage());
            }
        }
    }

    private class DeviceStatusUpdater extends AsyncTask<String[], Void, Void> {

        @Override
        protected Void doInBackground(String[]... networkDevices) {
            Devices devices = new Devices(mContext);
            try {
                devices.inactivateAll();
                // update device data
                for (String[] network : networkDevices) {
                    for (String device : network) {
                        String[] fields = device.split(" ");
                        String name = (fields.length > 0 ? fields[0] : "");
                        String ip = (fields.length > 1 ? fields[1] : "");
                        String mac = (fields.length > 2 ? fields[2] : "");
                        if (mac.length() == 18) {
                            Device d = devices.get(mac);
                            d.setOriginalName(name);
                            if (!ip.isEmpty())
                                d.setCurrentIP(ip);
                                d.setTrafficStats(getTxTrafficForIP(ip), getRxTrafficForIP(ip), currentTime());
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
    }

}

package com.brentandjody.tomatohub.routers;

import android.content.Context;

import com.brentandjody.tomatohub.MainActivity;
import com.brentandjody.tomatohub.database.Wifi;

import java.util.List;

/**
 * Created by brentn on 11/12/15.
 */
public class CiscoRouter extends Router {

    public CiscoRouter(Context activity) {
        super(activity);
    }

    @Override
    public void initialize() {

    }

    @Override
    public long getBootTime() {
        return 0;
    }

    @Override
    public String getExternalIP() {
        return null;
    }

    @Override
    public int getMemoryUsage() {
        return 0;
    }

    @Override
    public int[] getCPUUsage() {
        return new int[0];
    }

    @Override
    public void updateDevices() {

    }

    @Override
    public void updateTrafficStats() {

    }

    @Override
    public String getRouterId() {
        return null;
    }

    @Override
    public List<Wifi> getWifiList() {
        return null;
    }

    @Override
    public String[] getNetworkIds() {
        return new String[0];
    }

    @Override
    public int getTotalDevices() {
        return 0;
    }

    @Override
    public int getTotalDevicesOn(String network_id) {
        return 0;
    }

    @Override
    public void download10MbFile() {

    }

    @Override
    public void onLogonComplete(boolean success) {

    }
}

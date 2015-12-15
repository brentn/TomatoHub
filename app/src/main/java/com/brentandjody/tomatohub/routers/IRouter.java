package com.brentandjody.tomatohub.routers;

import com.brentandjody.tomatohub.database.Wifi;

import java.util.List;

/**
 * Created by brentn on 15/12/15.
 */
interface IRouter {

    void connect();
    void disconnect();
    String[] command(String command);
    void wifiSpeedTest();
    float getSpeedTestResult();

    // COMMANDS
    void initialize();
    long getBootTime();
    String getExternalIP();
    int getMemoryUsage();
    int[] getCPUUsage();
    void updateDevices();
    void updateTrafficStats();
    String getRouterId();
    List<Wifi> getWifiList();
    String[] getNetworkIds();
    int getTotalDevices();
    int getTotalDevicesOn(String network_id);
    void internetSpeedTest();
}

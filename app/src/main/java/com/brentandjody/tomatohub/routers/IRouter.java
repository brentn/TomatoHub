package com.brentandjody.tomatohub.routers;

import com.brentandjody.tomatohub.database.Wifi;

import java.util.List;

/**
 * Created by brentn on 15/12/15.
 * Interface for all router types
 */
interface IRouter {

    void connect();
    void disconnect();
    String[] command(String command);
    String getUrlToTest();
    void wifiSpeedTest(String url_on_router);
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
    String getRouterType();
    String getMacForIp(String ip);
    List<Wifi> getWifiList();
    String[] getNetworkIds();
    int getTotalDevices();
    int getTotalDevicesOn(String network_id);
    void internetSpeedTest();
}

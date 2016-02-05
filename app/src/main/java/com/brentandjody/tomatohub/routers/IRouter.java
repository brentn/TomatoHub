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
    void reboot();

    // COMMANDS
    void initialize();
    long getBootTime();
    String getExternalIP();
    String getInternalIP();
    int getMemoryUsage();
    int[] getCPUUsage();
    void updateDevices();
    void updateTrafficStats();
    String getRouterId();
    String getRouterType();
    String[] getNetworkIds();
    int getTotalDevices();
    int getTotalDevicesOn(String network_id);

    String getMacForIp(String ip);

    //Speed Test
    float getConnectionSpeed();
    void internetSpeedTest(boolean limitedSpace);

    //wifi
    List<Wifi> getWifiList();
    void setWifiPassword(Wifi wifi, String newPassword);
    void enableWifi(String ssid, boolean enabled);
    void broadcastWifi(String ssid, boolean broadcast);
    void wifiSpeedTest(int port);

    //Prioritize
    boolean isQOSEnabled();
    boolean isPrioritized(String ip);
    long isPrioritizedUntil(String ip);
    void prioritize(String ip, long until);
}

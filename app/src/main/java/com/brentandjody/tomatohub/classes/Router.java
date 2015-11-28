package com.brentandjody.tomatohub.classes;

/**
 * Created by brent on 28/11/15.
 */
public class Router {
    //uptime
    private long _uptime;
    private int _load1min;
    private int _load5min;
    private int _load15min;
    //ifconfig
    private NetworkInterface _wan;
    private NetworkInterface[] _ports;
    private NetworkInterface[] _wifi;
    //arp
    private Network[] _networks;
    //robocfg show

    private class Network {
        private Device[] devices;
    }
}

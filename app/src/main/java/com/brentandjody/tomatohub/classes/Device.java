package com.brentandjody.tomatohub.classes;

import java.util.Date;

/**
 * Created by brent on 28/11/15.
 */
public class Device {
    private String _name;
    private String _custom_name;
    private String _mac;
    private String _ip;
    private boolean _active;
    private long _tx_bypes;
    private long _rx_bytes;
    private long _timestamp; //unix time in seconds
    private DeviceType _type;

    public Device(String mac, String name) {
        _mac = mac;
        _name = name;
    }
    public Device(String mac, String ip, String name, boolean active) {
        _mac=mac;
        _ip=ip;
        _name=name;
        _active=active;
    }

    // Setters
    public void setCustomName(String name) {
        _custom_name=name;
    }

    public void setCurrentIP(String ip) {
        _ip=ip;
    }

    public void saveTrafficStats(long tx, long rx, long timestamp) {
        _tx_bypes=tx;
        _rx_bytes=rx;
        _timestamp=timestamp;
    }

    // Getters
    public String getMac() {
        return _mac;
    }

    public String getName() {
        if (_custom_name.isEmpty()) return _name;
        else return _custom_name;
    }

    public String getCustomName() {
        return _custom_name;
    }

    public String getOriginalName() {
        return _name;
    }

    public String getLastIP() {
        return _ip;
    }

    public boolean isActive() {
        return _active;
    }

    public long txTraffic() {
        return _tx_bypes;
    }

    public long rxTraffic() {
        return _rx_bytes;
    }

    public long timestamp() {
        return _timestamp;
    }

    private class DeviceType {
        public static final int Mobile = 1;
        public static final int Computer = 2;
    }
}

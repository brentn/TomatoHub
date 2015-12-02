package com.brentandjody.tomatohub.classes;

/**
 * Created by brent on 28/11/15.
 */
public class Device {

    private String _name;
    private String _custom_name;
    private String _mac;
    private String _ip;
    private boolean _active;
    private long _tx_bypes=0;
    private long _rx_bytes=0;
    private long _timestamp=-1; //unix time in seconds
    private float _last_speed=0;

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
    public void setOriginalName(String name) { _name=name;}
    public void setCustomName(String name) { _custom_name=name; }
    public void setCurrentIP(String ip) { _ip=ip; }
    public void setDetails(String name, String custom_name, String ip, boolean active, long tx, long rx, long timestamp, float last_speed) {
        _name=name;
        _custom_name=custom_name;
        _ip=ip;
        _active=active;
        _tx_bypes=tx;
        _rx_bytes=rx;
        _timestamp=timestamp;
        _last_speed=last_speed;
    }
    public void setTrafficStats(long tx, long rx, long timestamp) {
        _tx_bypes=tx;
        _rx_bytes=rx;
        _timestamp=timestamp;
    }

    // Getters
    public String mac() {
        return _mac;
    }

    public String name() {
        if (_custom_name.isEmpty()) return _name;
        else return _custom_name;
    }

    public String customName() {
        return _custom_name;
    }

    public String originalName() {
        return _name;
    }

    public String lastIP() {
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

    public float lastSpeed() {return _last_speed;}


}

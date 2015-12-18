package com.brentandjody.tomatohub.database;

import android.text.TextUtils;
import android.util.Log;

/**
 * Created by brent on 28/11/15.
 * Definition for Device object
 */
public class Device {

    private static final String TAG = Device.class.getName();

    private static final int SPEED_THRESHOLD = 60; //seconds below which speed won't be updated

    private String _router_id;
    private String _name;
    private String _custom_name;
    private String _mac;
    private String _ip;
    private String _last_network;
    private boolean _active;
    private boolean _blocked;
    private long _tx_bytes =0;
    private long _rx_bytes=0;
    private long _timestamp=-1; //unix time in seconds
    private float _last_speed=0;

    public Device(String router_id, String mac, String name) {
        _router_id = router_id;
        _mac = mac;
        _name = name;
    }
    public Device(String router_id, String mac, String last_network, String ip, String name, boolean active, boolean blocked) {
        _router_id =router_id;
        _mac=mac;
        _last_network=last_network;
        _ip=ip;
        _name=name;
        _active=active;
        _blocked=blocked;
    }

    // Setters
    public void setOriginalName(String name) { _name=name;}
    public void setCustomName(String name) { _custom_name=name; }
    public void setCurrentIP(String ip) { _ip=ip; }
    public void setCurrentNetwork(String network_name) {_last_network=network_name;}
    public void setActive(boolean active) {_active=active;}
    public void setBlocked(boolean blocked) {_blocked=blocked;}
    public void setDetails(String name, String custom_name, String network, String ip, boolean active, boolean blocked, long tx, long rx, long timestamp, float last_speed) {
        _name=name;
        _custom_name=custom_name;
        _last_network=network;
        _ip=ip;
        _active=active;
        _blocked=blocked;
        _tx_bytes =tx;
        _rx_bytes=rx;
        _timestamp=timestamp;
        _last_speed=last_speed;
    }
    public void setTrafficStats(long tx, long rx, long timestamp) {
        if (tx>=0 && rx>=0 && timestamp>_timestamp) { //ensure valid values
            if ((timestamp - _timestamp) > SPEED_THRESHOLD) {
                if (tx >=_tx_bytes && rx >= _rx_bytes) {  //if traffic stats were reset, don't calculate speed
                    long traffic = (tx - _tx_bytes) + (rx - _rx_bytes);
                    long time = (timestamp - _timestamp);
                    _last_speed = traffic / time;
                }
                _tx_bytes = tx;
                _rx_bytes = rx;
                _timestamp = timestamp;
            }
        }
    }

    // Getters
    public String router_id() {return _router_id;}
    public String mac() {
        return _mac;
    }
    public String name() {  //returns custom name if possible, else name.  Trims to 20 chars.
        if (TextUtils.isEmpty(_custom_name)) return _name.substring(0, Math.min(_name.length(), 20));
        else return _custom_name.substring(0, Math.min(_custom_name.length(), 20));
    }
    public String customName() {
        return _custom_name;
    }
    public String originalName() {
        return _name;
    }
    public String lastNetwork() {return _last_network;}
    public String lastIP() {
        return _ip;
    }
    public boolean isActive() {
        return _active;
    }
    public boolean isBlocked() {return _blocked; }
    public long txTraffic() {
        return _tx_bytes;
    }
    public long rxTraffic() {
        return _rx_bytes;
    }
    public long timestamp() {
        return _timestamp;
    }

    public float lastSpeed() {return _last_speed;}


}

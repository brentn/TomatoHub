package com.brentandjody.tomatohub.database;


/**
 * Created by brent on 05/12/15.
 *Network object
 */
public class Network {
    private static final long SPEED_THRESHOLD=300;

    private String _routerId;
    private String _networkId;
    private String _customName;
    private long _txBytes;
    private long _rxBytes;
    private long _timestamp;
    private float _speed;

    public Network(String router_id, String network_id) {
        _routerId=router_id;
        _networkId=network_id;
    }

    public void setCustomName(String name) {
        _customName = name;
    }
    public void setDetails(String custom_name, long tx, long rx, long timestamp, float speed) {
        _customName=custom_name;
        _txBytes=tx;
        _rxBytes=rx;
        _timestamp=timestamp;
        _speed=speed;
    }
    public void setSpeed(float speed) {_speed = speed;}
//    public void setTrafficStats(long tx, long rx, long timestamp) {
//        if (tx>0 && rx>0 && timestamp>_timestamp) { //ensure valid values
//            if ((timestamp - _timestamp) > SPEED_THRESHOLD) {
//                if (tx >=_txBytes && rx >= _rxBytes) {  //if traffic stats were reset, don't calculate speed
//                    long traffic = (tx - _txBytes) + (rx - _rxBytes);
//                    long time = (timestamp - _timestamp);
//                    _speed = traffic / time;
//                }
//                _txBytes = tx;
//                _rxBytes = rx;
//                _timestamp = timestamp;
//            }
//        }
//    }


    public String routerId() {return _routerId;}
    public String networkId() {return _networkId;}
    public String name() {
        if (_customName==null || _customName.length()<=0)
            return _networkId;
        else
            return _customName;
    }
    public String customName() {return _customName;}
    public long txBytes() {return _txBytes;}
    public long rxBytes() {return _rxBytes;}
    public long timestamp() {return _timestamp;}
    public float speed() {return _speed;}

}

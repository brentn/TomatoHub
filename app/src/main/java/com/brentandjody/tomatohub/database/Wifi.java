package com.brentandjody.tomatohub.database;

/**
 * Created by brentn on 09/12/15.
 * Wifi access point
 */
public class Wifi {
    private String mSSID;
    public Wifi(String ssid) {
        mSSID=ssid;
    }
    public String SSID() {return mSSID;}
}

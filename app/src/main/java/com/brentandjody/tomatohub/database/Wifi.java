package com.brentandjody.tomatohub.database;

/**
 * Created by brentn on 09/12/15.
 * Wifi access point
 */
public class Wifi {
    private String mSSID;
    private String mPassword;

    public Wifi(String ssid) {
        mSSID=ssid;
    }

    public void setPassword(String password) {mPassword = password;}
    public String SSID() {return mSSID;}
    public String password() {return mPassword;}
}

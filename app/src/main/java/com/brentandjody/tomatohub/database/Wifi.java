package com.brentandjody.tomatohub.database;

/**
 * Created by brentn on 09/12/15.
 * Wifi access point
 */
public class Wifi {
    private String mSSID;
    private boolean mBroadcast=true;
    private boolean mEnabled=true;
    private String mPassword;

    public Wifi(String ssid) {
        if (ssid==null) ssid="<unspecified>";
        mSSID=ssid;
    }

    public void setPassword(String password) {mPassword = password;}
    public void setBroadcast(boolean bcast) {mBroadcast = bcast;}
    public void setEnabled(boolean enabled) {mEnabled = enabled;
    }
    public String SSID() {return mSSID;}
    public boolean broadcast() {return mBroadcast;}
    public boolean enabled() {return mEnabled;}
    public String password() {return mPassword;}

    @Override
    public boolean equals(Object that) {
        return (that instanceof Wifi && this.mSSID.equals(((Wifi)that).SSID()));
    }

    @Override
    public int hashCode() {
        return mSSID.hashCode();
    }
}

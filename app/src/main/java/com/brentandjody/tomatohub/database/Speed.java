package com.brentandjody.tomatohub.database;

/**
 * Created by brentn on 22/01/16.
 * Speed object
 */
public class Speed {
    private String mRouterId;
    private long mTimestamp;
    private double mLanSpeed;
    private double mWanSpeed;

    public Speed(String routerId, long timestamp, double lanSpeed, double wanSpeed) {
        mRouterId = routerId;
        mTimestamp = timestamp;
        mLanSpeed = lanSpeed;
        mWanSpeed = wanSpeed;
    }

    public String routerId() { return mRouterId; }
    public long timestamp() { return mTimestamp; }
    public double lanSpeed() { return mLanSpeed; }
    public double wanSpeed() { return mWanSpeed; }
}

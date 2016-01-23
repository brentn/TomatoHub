package com.brentandjody.tomatohub.database;

/**
 * Created by brentn on 22/01/16.
 * Speed object
 */
public class Speed {
    private String mRouterId;
    private long mTimestamp;
    private float mLanSpeed;
    private float mWanSpeed;

    public Speed(String routerId, long timestamp, float lanSpeed, float wanSpeed) {
        mRouterId = routerId;
        mTimestamp = timestamp;
        mLanSpeed = lanSpeed;
        mWanSpeed = wanSpeed;
    }

    public String routerId() { return mRouterId; }
    public long timestamp() { return mTimestamp; }
    public float lanSpeed() { return mLanSpeed; }
    public float wanSpeed() { return mWanSpeed; }
}

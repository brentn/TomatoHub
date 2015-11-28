package com.brentandjody.tomatohub.classes;

/**
 * Created by brent on 28/11/15.
 */
public class Device {
    private String _name;
    private DeviceType _type;
    private NetworkInterface _interface;

    private class DeviceType {
        public static final int Mobile = 1;
        public static final int Computer = 2;
    }
}

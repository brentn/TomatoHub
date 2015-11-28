package com.brentandjody.tomatohub.classes;

/**
 * Created by brent on 28/11/15.
 */
public class NetworkInterface {
    private String _macaddress;
    private String _ipaddress;
    private InterfaceType _type;
    private boolean _enabled;
    private boolean _virtual;

    private class InterfaceType {
        public static final int Wired = 1;
        public static final int Wireless = 2;
    }
}

package com.brentandjody.tomatohub.routers;

/**
 * Created by brentn on 12/12/15.
 */
public class RouterType {
    public static final int COUNT=5;

    public static final int TOMATO=0;
    public static final int DDWRT=1;
    public static final int OPENWRT=2;
    public static final int LINKSYS=3;
    public static final int CISCO=4;

    public static String defaultValue="0";

    public static String name(int type) {
        switch (type) {
            case TOMATO:return "Tomato";
            case DDWRT:return "DD-Wrt";
            case OPENWRT: return "OpenWRT";
            case LINKSYS: return "Linksys";
            case CISCO: return "Cisco";
            default: return "<unknown>";
        }
    }

    public static int value(String value) {
        try {return Integer.parseInt(value);}
        catch (Exception ex) {return 0;}
    }

    public static CharSequence[] getEntryValues() {
        CharSequence[] result = new CharSequence[COUNT];
        for (int x=0; x<COUNT; x++) {
            result[x] = String.valueOf(x);
        }
        return result;
    }

    public static CharSequence[] getEntries() {
        CharSequence[] result = new CharSequence[COUNT];
        for (int x=0; x<COUNT; x++) {
            result[x]=name(x);
        }
        return result;
    }
}

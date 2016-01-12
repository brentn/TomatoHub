package com.brentandjody.tomatohub.routers;

/**
 * Created by brentn on 12/12/15.
 * Define the various router types supported by the app
 */
public class RouterType {
    public static final int COUNT=3;

    public static final int TOMATO=0;
    public static final int DDWRT=1;
    public static final int FAKE=2;

    public static String defaultValue="2";

    public static String name(int type) {
        switch (type) {
            case TOMATO:return "Tomato";
            case DDWRT:return "DD-Wrt";
            case FAKE:return "DEMO MODE";
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

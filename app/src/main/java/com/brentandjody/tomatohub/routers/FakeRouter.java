package com.brentandjody.tomatohub.routers;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.brentandjody.tomatohub.database.Device;
import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Wifi;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by brentn on 17/12/15.
 */
public class FakeRouter extends Router {
    private static final String TAG = FakeRouter.class.getName();

    final Random rnd = new Random();
    final Handler handler = new Handler();

    private String[] mNetworkIds=null;
    private Devices mDevices;

    public FakeRouter(Context context) {
        super(context);
        setupFakeDevices();
    }

    @Override
    public void disconnect() { }
    @Override
    public String getUrlToTest() {return "http://google.com";}
    @Override
    public void cleanUpAfterTest() {}

    @Override
    public String[] command(String command) {
        return new String[0];
    }

    @Override
    public float getSpeedTestResult() {
        final float MAX_ROUTER_SPEED=54.4F;
        float result = rnd.nextFloat()*MAX_ROUTER_SPEED;
        Log.d(TAG, "Speed:"+result);
        return result;
    }


    @Override
    public long getBootTime() {
        final long LONGEST_TIME = 30*24*60*60*1000;
        long now = System.currentTimeMillis();
        long ago = rnd.nextLong()%LONGEST_TIME;
        long result = (now-ago)/1000;
        Log.d(TAG, "Boot time:"+result);
        return result;
    }

    @Override
    public String getExternalIP() { return "8.8.8.7"; }
    @Override
    public int getMemoryUsage() { return rnd.nextInt(100); }
    @Override
    public int[] getCPUUsage() {  return new int[] {rnd.nextInt(100), rnd.nextInt(100), rnd.nextInt(100)}; }
    @Override
    public String getRouterId() { return "FakeRouter";  }
    @Override
    public String getRouterType() { return "Fake Router"; }

    @Override
    public String getMacForIp(String ip) { return "00:00:00:00:00:00"; }

    @Override
    public List<Wifi> getWifiList() {
        final int MAX_WIFIS=4;
        List<Wifi> result = new ArrayList();
        for (int i=0; i<rnd.nextInt(MAX_WIFIS); i++) {
            String ssid = randomString(15);
            Wifi wifi = new Wifi(ssid);
            wifi.setPassword("MyWifiPassword");
            result.add(wifi);
        }
        return result;
    }

    @Override
    public String[] getNetworkIds() {
        if (mNetworkIds==null) {
            final int MAX_NETWORKS = 6;
            int num_networks = rnd.nextInt(MAX_NETWORKS - 1) + 1;
            String[] result = new String[num_networks];
            for (int i = 0; i < num_networks; i++) {
                result[i] = "br" + i;
            }
            mNetworkIds = result;
        }
        Log.d(TAG, "Number of networks:"+mNetworkIds.length);
        return mNetworkIds;
    }

    @Override
    public int getTotalDevices() {
        int result = 0;
        for (String network : mNetworkIds) {
            result += getTotalDevicesOn(network);
        }
        return result;
    }

    @Override
    public int getTotalDevicesOn(String network_id) {
        return mDevices.getDevicesOnNetwork(getRouterId(), network_id).size();
    }

    @Override
    public void connect() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mListener.onRouterActivityComplete(ACTIVITY_CONNECTED, ACTIVITY_STATUS_SUCCESS);
            }
        }, 700);
    }
    @Override
    public void initialize() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mListener.onRouterActivityComplete(ACTIVITY_INTIALIZE, ACTIVITY_STATUS_SUCCESS);
            }
        }, 500);
    }
    @Override
    public void updateDevices() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mListener.onRouterActivityComplete(ACTIVITY_DEVICES_UPDATED, ACTIVITY_STATUS_SUCCESS);
            }
        }, 300);
    }
    @Override
    public void updateTrafficStats() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mListener.onRouterActivityComplete(ACTIVITY_TRAFFIC_UPDATED, ACTIVITY_STATUS_SUCCESS);
            }
        }, 300);
    }
    @Override
    public void internetSpeedTest() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mListener.onRouterActivityComplete(ACTIVITY_INTERNET_10MDOWNLOAD, ACTIVITY_STATUS_SUCCESS);
            }
        }, 7000);
    }
    @Override
    public void wifiSpeedTest(String url_on_router) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mListener.onRouterActivityComplete(ACTIVITY_WIFI_SPEED_TEST, ACTIVITY_STATUS_SUCCESS);
            }
        }, 1500);
    }

    private void setupFakeDevices() {
        mDevices = new Devices(mContext);
        mDevices.removeFakeDevices(getRouterId());
        getNetworkIds();
        int total_devices = rnd.nextInt(40)+3;
        Log.d(TAG, "Generating "+total_devices+" fake devices");
        for (int i=0; i<total_devices; i++) {
            Device device = new Device(getRouterId(), randomMACAddress(), randomString(10));
            device.setCurrentIP("192.168.1."+(rnd.nextInt(253)+1));
            device.setCurrentNetwork(mNetworkIds[rnd.nextInt(mNetworkIds.length)]);
            device.setActive(rnd.nextInt(100)<70);
            mDevices.insertOrUpdate(device);
        }
    }


    private String randomMACAddress(){
        Random rand = new Random();
        byte[] macAddr = new byte[6];
        rand.nextBytes(macAddr);

        macAddr[0] = (byte)(macAddr[0] & (byte)254);  //zeroing last 2 bytes to make it unicast and locally adminstrated

        StringBuilder sb = new StringBuilder(18);
        for(byte b : macAddr){

            if(sb.length() > 0)
                sb.append(":");

            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    String randomString( int len ){
        StringBuilder sb = new StringBuilder( len );
        for( int i = 0; i < len; i++ )
            sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        return sb.toString();
    }
}

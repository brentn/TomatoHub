package com.brentandjody.tomatohub.routers;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.brentandjody.tomatohub.database.Device;
import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Wifi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by brentn on 17/12/15.
 * A fake router, to demo the app, or test
 */
public class FakeRouter extends Router {
    private static final String TAG = FakeRouter.class.getName();

    final Random rnd = new Random();
    final Handler handler = new Handler();

    private List<Wifi> mWifis=null;
    private String[] mNetworkIds=null;
    private Devices mDevices;
    private long mTimestamp;
    private Collection<Priority> priorities =new HashSet<>(30);

    public FakeRouter(Context context) {
        super(context);
        setupFakeDevices();
    }

    @Override
    public void disconnect() { }
    @Override
    public void prioritize(String ip, long until) {
        priorities.add(new Priority(ip, until));
    }

    @Override
    public boolean isPrioritized(String ip) {
        for (Priority p : priorities) {
            if (p._ip.equals(ip))
                return true;
        }
        return false;
    }

    @Override
    public long isPrioritizedUntil(String ip) {
        for (Priority p : priorities) {
            if (p._ip.equals(ip))
                return p._until;
        }
        return Device.NOT_PRIORITIZED;
    }

    @Override
    public String[] command(String command) {
        if (command.endsWith("hwaddr")) return new String[] {"00:00:00:00:00:00"};
        if (command.startsWith("df ")) return new String[] {"2000"};
        return new String[0];
    }

    @Override
    public void reboot() {
    }

    @Override
    public long getBootTime() {
        int LONGEST_TIME = 30*24*60*60;
        long now = System.currentTimeMillis()/1000;
        long ago = rnd.nextInt(LONGEST_TIME);
        long result = (now-ago);
        Log.d(TAG, "Boot time:"+result);
        return result;
    }

    @Override
    public String getExternalIP() { return "8.8.8.8"; }
    @Override
    public int getMemoryUsage() { return rnd.nextInt(100); }
    @Override
    public int[] getCPUUsage() {  return new int[] {rnd.nextInt(100), rnd.nextInt(100), rnd.nextInt(100)}; }
    @Override
    public String getRouterId() { return "FakeRouter";  }
    @Override
    public String getRouterType() { return RouterType.name(RouterType.FAKE); }
    @Override
    public boolean isQOSEnabled() { return true; }

    @Override
    public void enableWifi(String ssid, boolean enabled) {
        for (Wifi w : mWifis) {
            if (w.SSID().equals(ssid)) w.setEnabled(enabled);
        }
        mListener.onRouterActivityComplete(ACTIVITY_WIFI_UPDATED,ACTIVITY_STATUS_SUCCESS);
    }

    @Override
    public void broadcastWifi(String ssid, boolean broadcast) {
        for (Wifi w : mWifis) {
            if (w.SSID().equals(ssid)) w.setBroadcast(broadcast);
        }
        mListener.onRouterActivityComplete(ACTIVITY_WIFI_UPDATED,ACTIVITY_STATUS_SUCCESS);
    }

    @Override
    public String getMacForIp(String ip) { return "00:00:00:00:00:00"; }

    @Override
    public List<Wifi> getWifiList() {
        if (mWifis==null) {
            final int MAX_WIFIS = 3;
            mWifis = new ArrayList<>();
            for (int i = 0; i < rnd.nextInt(MAX_WIFIS)+1; i++) {
                String ssid = randomWifiName();
                Wifi wifi = new Wifi(ssid);
                wifi.setPassword(randomString(12));
                mWifis.add(wifi);
            }
        }
        return mWifis;
    }

    @Override
    public void setWifiPassword(Wifi wifi, String newPassword) {
        if (! newPassword.isEmpty()) {
            wifi.setPassword(newPassword);
            mListener.onRouterActivityComplete(ACTIVITY_WIFI_UPDATED, ACTIVITY_STATUS_SUCCESS);
        }
    }

    @Override
    public String[] getNetworkIds() {
        if (mNetworkIds==null) {
            final int MAX_NETWORKS = 3;
            int num_networks = rnd.nextInt(MAX_NETWORKS - 1) + 1;
            String[] result = new String[num_networks];
            for (int i = 0; i < num_networks; i++) {
                result[i] = "br" + i;
            }
            mNetworkIds = result;
        }
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
        int total=0;
        for (Device d : mDevices.getDevicesOnNetwork(getRouterId(), network_id)) {
            if (d.isActive()) total++;
        }
        return total;
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
        long now = System.currentTimeMillis();
        for (String networkId:mNetworkIds) {
            for (Device d : mDevices.getDevicesOnNetwork(getRouterId(), networkId)) {
                if (d.isActive()) {
                    long elapsed_time = (now - mTimestamp)/1000;
                    long bytes = Math.round(Math.pow((rnd.nextInt(5000)/10000F),-1.7)*10240);
                    long traffic = Math.round((float) bytes / elapsed_time);
                    d.setTrafficStats(traffic,traffic,now);
                    mDevices.insertOrUpdate(d);
                }
            }
        }
        mTimestamp=now;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mListener.onRouterActivityComplete(ACTIVITY_TRAFFIC_UPDATED, ACTIVITY_STATUS_SUCCESS);
            }
        }, 300);
    }
    @Override
    public void internetSpeedTest(boolean limitedSpace) {
        new InternetDownloader().execute();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mListener.onRouterActivityComplete(ACTIVITY_DOWNLOAD_COMPLETE, ACTIVITY_STATUS_SUCCESS);
//            }
//        }, 7000);
    }
    @Override
        public void wifiSpeedTest(int port) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mListener.onRouterActivityComplete(ACTIVITY_WIFI_SPEED_TEST, ACTIVITY_STATUS_SUCCESS);
            }
        }, 1500);
    }

    @Override
    public float getConnectionSpeed() {
        return rnd.nextFloat()*54;
    }

    private void setupFakeDevices() {
        mDevices = new Devices(mContext);
        mDevices.removeFakeDevices(getRouterId());
        mTimestamp = System.currentTimeMillis();
        getNetworkIds();
        int total_devices = rnd.nextInt(40)+3;
        Log.d(TAG, "Generating "+total_devices+" fake devices");
        for (int i=0; i<total_devices; i++) {
            Device device = new Device(getRouterId(), randomMACAddress(), randomDeviceName());
            device.setCurrentIP("192.168.1."+(rnd.nextInt(253)+1));
            device.setCurrentNetwork(mNetworkIds[rnd.nextInt(mNetworkIds.length)]);
            device.setActive(rnd.nextInt(100)<25);
            device.setTrafficStats(0,0,mTimestamp-60000); //1 min ago
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

    String randomDeviceName() {
        String[] people = new String[] {"Dave", "Monica", "Ralph", "Julie", "Guido", "Alfons", "Sarah", "Jean", "Kelly", "David", "Maryanne", "Joel", "Dannika", "Lorrie", "Stephen",
        "Harlow", "Pixie", "Donna", "Darth Vader", "Lindsay", "Norm", "Pinky", "Dillon", "Eugene", "Sam", "Ronald", "Ice man", "Wellington", "Tuna", "Nice lady"};
        String[] devices = new String[] {"Main Computer", "[NAME]'s laptop", "[NAME]'s MacBook Pro", "[NAME]'s iPhone", "[NAME]'s phone", "[NAME]'s Android", "Kid's tablet",
        "Telephone", "Thermostat", "Smart TV", "Bedroom TV", "Apple TV", "Kindle Fire TV", "Work computer", "[NAME]'s computer", "Printer", "Color printer", "Print server",
        "[NAME]'s iPad", "XBox", "[NAME]'s Wii", "Old iPad", "RPi", "Cisco [ID]", "android-[ID]", "generic game console", "Google Glass" } ;
        boolean duplicate = true;
        String name = people[rnd.nextInt(people.length)];
        String devicename="";
        while (duplicate) {
            String id = randomString(rnd.nextInt(5)+5);
            devicename = devices[rnd.nextInt(devices.length)].replace("[NAME]", name).replace("[ID]", id);
            duplicate = false;
            for (String n : getNetworkIds()) {
                for (Device d : mDevices.getDevicesOnNetwork(getRouterId(), n)) {
                    if (d.name().equals(devicename)) {
                        duplicate = true;
                        break;
                    }
                }
            }
        }
        return devicename;
    }

    String randomWifiName() {
        String[] wifis = new String[] {"Wifi", "Guest Network", "guest", "ddwrt", "tomato26", "tomato5", "Downstairs", "Backyard wifi", "insecure", "Police Surviellance Van #4",
        "42", "My wifi", "My Neighbor's Wifi", "Telus2095", "Dickie's Wifi"};
        return wifis[rnd.nextInt(wifis.length)];
    }

    class Priority {
        String _ip;
        long _until;
        public Priority(String ip, long until) {
            _ip=ip;
            _until=until;
        }
    }

    private class InternetDownloader extends AsyncTask<Void, Integer, Integer> {
        private long TIMELIMIT = 10000; //10 seconds
        boolean finished=false;
        boolean success=false;
        long runUntil;
        int size=0;
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        @Override
        protected void onPreExecute() {
            runUntil = System.currentTimeMillis()+TIMELIMIT;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            finished=false;
            while (! finished) {
                try {
                    Thread.sleep(1000);
                    size += rnd.nextInt(8000000);
                    publishProgress(size);
                    finished = (System.currentTimeMillis() > runUntil);

                }
                catch (InterruptedException e) { finished = true; }
            }
            success=true;
            return size;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            mListener.onRouterActivityComplete(ACTIVITY_DOWNLOAD_PROGRESS, values[0]);
        }

        @Override
        protected void onPostExecute(Integer size) {
            super.onPostExecute(size);
            if (!isCancelled())
                mListener.onRouterActivityComplete(ACTIVITY_DOWNLOAD_COMPLETE, success?size:ACTIVITY_STATUS_FAILURE);
        }
    }

}

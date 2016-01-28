package com.brentandjody.tomatohub;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.brentandjody.tomatohub.database.DBContract;
import com.brentandjody.tomatohub.database.Device;
import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Network;
import com.brentandjody.tomatohub.database.Networks;
import com.brentandjody.tomatohub.database.Speed;
import com.brentandjody.tomatohub.database.Speeds;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Random;

/**
 * Created by brentn on 27/01/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DatabaseTests extends InstrumentationTestCase {
    private Context mContext;

    @Before
    public void setup() throws Exception{
        super.setUp();
        mContext = InstrumentationRegistry.getTargetContext();
    }


    @Test
    public void Speeds_test_insert_and_get_and_deleteAllHistory() {
        String routerId = "lkjrjnn";
        TestableSpeeds speeds = new TestableSpeeds(mContext);
        speeds.deleteHistoryFor(routerId);
        Collection<Speed> x=speeds.get(routerId);
        assertEquals(0, speeds.get(routerId).size());
        speeds.insert(new Speed(routerId, 0, 0, 0));
        assertEquals(1, speeds.get(routerId).size());
        assertEquals(0, speeds.get(routerId + "x").size());
        speeds.insert(new Speed(routerId + "abc", 0, 0, 0));
        assertEquals(1, speeds.get(routerId).size());
        assertEquals(0, speeds.get(routerId + "x").size());
        speeds.deleteAllHistory();
        assertEquals(0, speeds.get(routerId).size());
    }

    @Test
    public void Speeds_avgSpeed_calculates_correct_average() {
        String routerId = "902lldkf";
        Random rnd = new Random();
        double totalLAN=0;
        double totalWAN=0;
        TestableSpeeds speeds = new TestableSpeeds(mContext);
        speeds.deleteHistoryFor(routerId);
        for (int i=0; i<20; i++) {
            double LAN = rnd.nextFloat()*10000;
            double WAN = rnd.nextFloat()*10000;
            speeds.insert(new Speed(routerId, 0, LAN, WAN));
            totalLAN += LAN;
            totalWAN += WAN;
            assertEquals(totalLAN/(i+1), speeds.avgLANSpeed(routerId));
            assertEquals(totalWAN/(i+1), speeds.avgWANSpeed(routerId));
        }
    }

    @Test
    public void Speeds_stdDev_and_isextreme_calculate_correctly() {
        String routerId="d993d99";
        Random rnd = new Random();
        TestableSpeeds speeds = new TestableSpeeds(mContext);
        speeds.deleteHistoryFor(routerId);
        int count=rnd.nextInt(50)+10;
        for (int i=0; i<count; i++) {
            double LAN = rnd.nextFloat()*10000;
            double WAN = rnd.nextFloat()*10000;
            speeds.insert(new Speed(routerId, 0, LAN, WAN));
        }
        double squared_diff =0;
        double avg = speeds.avgLANSpeed(routerId);
        for (Speed s : speeds.get(routerId)) {
            squared_diff += Math.pow(avg-s.lanSpeed(), 2);
        }
        double variance = squared_diff/(count-1);
        double stdDev = Math.sqrt(variance);
        assertEquals(stdDev, speeds.stdDevLAN(routerId, avg));
        assertEquals(0, speeds.isExtreme(routerId, Network.LAN, avg));
        assertEquals(0, speeds.isExtreme(routerId, Network.LAN, avg + stdDev - .00001));
        assertEquals(0, speeds.isExtreme(routerId, Network.LAN, avg - stdDev + .00001));
        assertEquals(1, speeds.isExtreme(routerId, Network.LAN, avg+stdDev+.00001));
        assertEquals(-1, speeds.isExtreme(routerId, Network.LAN, avg-stdDev-.00001));
        assertEquals(2, speeds.isExtreme(routerId, Network.LAN, avg+stdDev+stdDev+.00001));
        assertEquals(-2, speeds.isExtreme(routerId, Network.LAN, avg-stdDev-stdDev-.00001));
    }

    @Test
    public void Networks_insert_update() {
        String routerId="Router99930lkdd";
        String networkId="networkid";
        TestableNetworks networks = new TestableNetworks(mContext);
        networks.deleteNetworksFor(routerId);
        assertEquals(0, networks.countNetworks(routerId));
        networks.insertOrUpdate(new Network(routerId, networkId));
        assertEquals(1, networks.countNetworks(routerId));
        networks.insertOrUpdate(new Network(routerId, networkId + "1"));
        assertEquals(2, networks.countNetworks(routerId));
        networks.insertOrUpdate(new Network(routerId, networkId));
        assertEquals(2, networks.countNetworks(routerId));
    }

    @Test
    public void Devices_insert_updates_if_mac_exists() {
        String routerId="routerId--d0j2n";
        String networkId="networkId999aadkfn";
        String mac="MAC ADDRESS";
        Devices devices = new Devices(mContext);
        devices.removeFakeDevices(routerId);
        assertEquals(0, devices.getDevicesOnNetwork(routerId, networkId).size());
        devices.insertOrUpdate(new Device(routerId, mac, networkId, null, null, false, false, 0));
        assertEquals(1, devices.getDevicesOnNetwork(routerId, networkId).size());
        devices.insertOrUpdate(new Device(routerId, mac + "1", networkId, null, null, false, false, 0));
        assertEquals(2, devices.getDevicesOnNetwork(routerId, networkId).size());
        devices.insertOrUpdate(new Device(routerId, mac, networkId, null, null, false, false, 0));
        assertEquals(2, devices.getDevicesOnNetwork(routerId, networkId).size());
    }

    @Test
    public void Devices_resetAll_resets_blocked_prioritized_and_active() {
        String routerId="routllasaasnnvdkjl";
        Random rnd = new Random();
        String network1 = "Network1";
        String network2 = "Network2";
        Devices devices = new Devices(mContext);
        devices.removeFakeDevices(routerId);
        int count = rnd.nextInt(20)+10;
        for (int i=0; i<count; i++) {
            Device d = new Device(routerId, "MAC"+i, null);
            d.setCurrentNetwork(rnd.nextBoolean()?network1:network2);
            d.setActive(rnd.nextBoolean());
            d.setBlocked(rnd.nextBoolean());
            d.setPrioritizedUntil(rnd.nextLong());
            devices.insertOrUpdate(d);
        }
        devices.resetAll();
        for (Device d : devices.getDevicesOnNetwork(routerId, network1)) {
            assertFalse(d.isActive());
            assertFalse(d.isBlocked());
            assertEquals(Device.NOT_PRIORITIZED, d.prioritizedUntil());
        }
        for (Device d : devices.getDevicesOnNetwork(routerId, network2)) {
            assertFalse(d.isActive());
            assertFalse(d.isBlocked());
            assertEquals(Device.NOT_PRIORITIZED, d.prioritizedUntil());
        }

    }

    @Test
    public void Devices_get_creates_new_device_if_not_found() {
        String routerId="a;ldnnng;";
        String networkId="naaldn";
        Devices devices = new Devices(mContext);
        devices.removeFakeDevices(routerId);
        assertEquals(0, devices.getDevicesOnNetwork(routerId, networkId).size());
        devices.insertOrUpdate(new Device(routerId, "mac1", networkId, null, null, false, false,0));
        assertEquals(1, devices.getDevicesOnNetwork(routerId, networkId).size());
        devices.insertOrUpdate(new Device(routerId, "mac2", networkId, null, null, false, false,0));
        assertEquals(2, devices.getDevicesOnNetwork(routerId, networkId).size());
        devices.insertOrUpdate(new Device(routerId, "mac3", networkId, null, null, false, false,0));
        assertEquals(3, devices.getDevicesOnNetwork(routerId, networkId).size());
    }

    @Test
    public void Devices_test_get_returns_correct_device() {
        String routerId="3902nnnsk";
        String networkId="llannv";
        String[] macs = new String[] {"mac1","mac2","mac3","mac4"};
        Devices devices = new Devices(mContext);
        devices.removeFakeDevices(routerId);
        for (String mac : macs) {
            devices.insertOrUpdate(new Device(routerId, mac, networkId, null, null, false, false, 0));
        }
        for (String mac : macs) {
            Device d = devices.get(routerId, mac);
            assertEquals(networkId, d.lastNetwork());
        }
    }

    @Test
    public void Devices_update_name_updates_all_instances_of_that_mac_address_on_all_routers_and_networks() {
        String[] routers = new String[] {"router1","router2","router3"};
        String[] networks = new String[] {"network1","network2","network3"};
        String[] macs = new String[] {"mac1","mac2","mac3"};
        TestableDevices devices = new TestableDevices(mContext);
        for (String r : routers) {
            devices.removeFakeDevices(r);
            for (String n : networks) {
                for (String m : macs) {
                    devices.insertOrUpdate(new Device(r,m,n,null,null,false,false,0));
                }
            }
        }
        int total=0;
        for (String r : routers) {
            total += devices.count(r);
        }
        assertEquals(macs.length*routers.length, total);
        String new_name="new name";
        devices.updateName(macs[2], new_name);
        for (String r : routers) {
            assertEquals(new_name, devices.get(r,macs[2]).name());
        }
    }


    // Testable classes
    public class TestableDevices extends Devices {

        public TestableDevices(Context context) {
            super(context);
        }
        public int count(String router_id) {
            int result=0;
            SQLiteDatabase db = getReadableDatabase();
            try {
                result = db.query(DBContract.DeviceEntry.TABLE_NAME, PROJECTION, DBContract.DeviceEntry.COLUMN_ROUTER_ID+"=?",new String[] {router_id},null,null,null).getCount();
            }finally {
                db.close();
            }
            return result;
        }
    }
    public class TestableNetworks extends Networks {

        public TestableNetworks(Context context) {
            super(context);
        }

        public int countNetworks(String router_id) {
            SQLiteDatabase db = getReadableDatabase();
            int result=0;
            try {
                Cursor c = db.query(DBContract.NetworkEntry.TABLE_NAME, PROJECTION, DBContract.NetworkEntry.COLUMN_ROUTER_ID + "=?", new String[]{router_id}, null, null, null);
                result = c.getCount();
            } finally {
                db.close();
            }
            return result;
        }

        public void deleteNetworksFor(String router_id) {
            SQLiteDatabase db = getWritableDatabase();
            try {
                db.delete(DBContract.NetworkEntry.TABLE_NAME, DBContract.NetworkEntry.COLUMN_ROUTER_ID+"=?", new String[] {router_id});
            } finally {
                db.close();
            }
        }
    }
    public class TestableSpeeds extends Speeds {

        public TestableSpeeds(Context context) {
            super(context);
        }


        public void deleteHistoryFor(String router_id) {
            SQLiteDatabase db = getWritableDatabase();
            try {
                db.delete(DBContract.SpeedEntry.TABLE_NAME, DBContract.SpeedEntry.COLUMN_ROUTER_ID+"=?", new String[] {router_id});
            } finally {
                db.close();
            }
        }

        public double avgWANSpeed(String router_id) {
            return super.avgSpeed(router_id, Network.WAN);
        }
        public double avgLANSpeed(String router_id) {
            return super.avgSpeed(router_id, Network.LAN);
        }
        public double stdDevLAN(String router_id, double avg) {
            return super.stdDev(router_id, Network.LAN, avg);
        }

        public double stdDev(String router_id, int wan_or_lan, double average_speed) {
            return super.stdDev(router_id, wan_or_lan, average_speed);
        }
    }
}



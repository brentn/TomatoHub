package com.brentandjody.tomatohub;

import android.text.TextUtils;

import com.brentandjody.tomatohub.database.Device;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


import static org.junit.Assert.*;

/**
 * Created by brentn on 08/12/15.
 * Test various database objects
 */

@RunWith(MockitoJUnitRunner.class)
public class ObjectIntegrityTest {

    @Test
    public void device_constructors_set_appropriate_fields() {
        String ROUTERID = "RouterID";
        String MAC = "MacAddress";
        String NAME = "DeviceName";
        Device device = new Device(ROUTERID, MAC,NAME);
        assertTrue(device.router_id().equals(ROUTERID));
        assertTrue(device.mac().equals(MAC));
        assertTrue(device.originalName().equals(NAME));

        String NETWORK = "202020";
        String IP = "20.20.200.1";
        boolean ACTIVE=true;
        boolean BLOCKED=false;
        device = new Device(ROUTERID,MAC,NETWORK,IP,NAME,ACTIVE,BLOCKED);
        assertTrue(device.router_id().equals(ROUTERID));
        assertTrue(device.mac().equals(MAC));
        assertTrue(device.originalName().equals(NAME));
        assertTrue(device.lastNetwork().equals(NETWORK));
        assertTrue(device.lastIP().equals(IP));
        assertTrue(device.isActive()==ACTIVE);
        assertTrue(device.isBlocked()==BLOCKED);
    }

    @Test
    public void device_name_returns_custom_name_if_available() {
        String NAME = "theName";
        String CUSTOM = "customName";
        Device device = new Device("R","M",NAME);
        assertTrue(device.name().equals(NAME));
        assertTrue(device.customName()==null);
        device.setCustomName(CUSTOM);
        assertTrue(device.name().equals(CUSTOM));
        assertTrue(device.customName().equals(CUSTOM));
        assertTrue(device.originalName().equals(NAME));
        device.setCustomName("");
        assertTrue(device.name().equals(NAME));
    }

    @Test
    public void device_name_is_truncated_to_20_characters() {
        String NAME = "123456789012345678901234567890";
        Device device = new Device("R", "M", NAME);
        assertTrue(device.originalName().equals(NAME));
        assertTrue(device.name().equals(NAME.substring(0,20)));
    }

    @Test
    public void device_getters_and_setters() {
        String IP = "123.123.123.213";
        String NETWORK = "abc";
        Device device = new Device("", "", "");
        device.setCurrentIP(IP);
        assertTrue(device.lastIP().equals(IP));
        device.setCurrentNetwork(NETWORK);
        assertTrue(device.lastNetwork().equals(NETWORK));
        device.setActive(true);
        assertTrue(device.isActive() == true);
        device.setActive(false);
        assertTrue(device.isActive() == false);
        device.setBlocked(false);
        assertTrue(device.isBlocked() == false);
        device.setBlocked(true);
        assertTrue(device.isBlocked() == true);
    }

    @Test
    public void device_setTraffic_sets_values() {
        long TX=938798;
        long RX=29920917;
        long TIMESTAMP = System.currentTimeMillis();
        Device device = new Device("", "", "");
        device.setTrafficStats(TX, RX, TIMESTAMP);
        assertEquals(device.rxTraffic(), RX);
        assertEquals(device.txTraffic(), TX);
        assertEquals(device.timestamp(), TIMESTAMP);
    }

    @Test
    public void device_setTraffic_doesnt_set_anything_if_invalid_values() {
        long TX=5432345;
        long RX=498902;
        long TIMESTAMP = System.currentTimeMillis();
        Device device = new Device("", "", "");
        device.setTrafficStats(TX, RX, TIMESTAMP);
        device.setTrafficStats(-1, 123, TIMESTAMP+1);
        device.setTrafficStats(123, -1, TIMESTAMP+2);
        device.setTrafficStats(123, 123, TIMESTAMP-1);
        assertEquals(device.rxTraffic(), RX);
        assertEquals(device.txTraffic(), TX);
        assertEquals(device.timestamp(), TIMESTAMP);
    }

    @Test
    public void device_setTraffic_updates_speed() {
        long TX=5432345;
        long RX=498902;
        long TIMESTAMP = System.currentTimeMillis();
        Device device = new Device("", "", "");
        device.setTrafficStats(TX, RX, TIMESTAMP);
        float speed = (TX+RX)/TIMESTAMP;
        assertEquals(device.lastSpeed(), speed, 0);
        device.setTrafficStats(TX+100,RX+100,TIMESTAMP+100);
        assertEquals(device.lastSpeed(), 2, 0);
    }

}

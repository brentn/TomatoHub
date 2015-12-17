package com.brentandjody.tomatohub;

import com.brentandjody.tomatohub.database.Device;
import com.brentandjody.tomatohub.routers.LinuxRouter;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by brentn on 08/12/15.
 * Test various database objects
 */
public class ObjectIntegrityTests {

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
        assertTrue(device.customName().isEmpty());
        device.setCustomName(CUSTOM);
        assertTrue(device.name().equals(CUSTOM));
        assertTrue(device.customName().equals(NAME));
        device.setCustomName("");
        assertTrue(device.name().equals(NAME));
    }

    @Test
    public void device_getters_and_setters() {
        Device device = new Device("", "", "");
    }

}

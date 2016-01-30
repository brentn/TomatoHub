package com.brentandjody.tomatohub;

import com.brentandjody.tomatohub.database.Device;
import com.brentandjody.tomatohub.database.Network;
import com.brentandjody.tomatohub.database.Speed;
import com.brentandjody.tomatohub.database.Wifi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by brentn on 08/12/15.
 * Test various database objects
 */

public class ObjectIntegrityTest {

    @Test
    public void device_constructors_set_appropriate_fields() {
        String ROUTERID = "RouterID";
        String MAC = "MacAddress";
        String NAME = "DeviceName";
        long UNTIL = 209384092;
        Device device = new Device(ROUTERID, MAC, NAME);
        assertTrue(device.router_id().equals(ROUTERID));
        assertTrue(device.mac().equals(MAC));
        assertTrue(device.originalName().equals(NAME));

        String NETWORK = "202020";
        String IP = "20.20.200.1";
        boolean ACTIVE = true;
        boolean BLOCKED = false;
        device = new Device(ROUTERID, MAC, NETWORK, IP, NAME, ACTIVE, BLOCKED, UNTIL);
        assertTrue(device.router_id().equals(ROUTERID));
        assertTrue(device.mac().equals(MAC));
        assertTrue(device.originalName().equals(NAME));
        assertTrue(device.lastNetwork().equals(NETWORK));
        assertTrue(device.lastIP().equals(IP));
        assertTrue(device.isActive() == ACTIVE);
        assertTrue(device.isBlocked() == BLOCKED);
        assertTrue(device.prioritizedUntil() == UNTIL);
    }

    @Test
    public void device_name_returns_custom_name_if_available() {
        String NAME = "theName";
        String CUSTOM = "customName";
        Device device = new Device("R", "M", NAME);
        assertTrue(device.name().equals(NAME));
        assertTrue(device.customName() == null);
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
        assertTrue(device.name().equals(NAME.substring(0, 20)));
    }

    @Test
    public void device_getters_and_setters() {
        String IP = "123.123.123.213";
        String NETWORK = "abc";
        long UNTIL = 2093280;
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
        assertTrue(device.prioritizedUntil() == Device.NOT_PRIORITIZED);
        device.setPrioritizedUntil(UNTIL);
        assertEquals(device.prioritizedUntil(), UNTIL);
    }

    @Test
    public void device_setTraffic_sets_values() {
        long TX = 938798;
        long RX = 29920917;
        long TIMESTAMP = System.currentTimeMillis();
        Device device = new Device("", "", "");
        device.setTrafficStats(TX, RX, TIMESTAMP);
        assertEquals(device.rxTraffic(), RX);
        assertEquals(device.txTraffic(), TX);
        assertEquals(device.timestamp(), TIMESTAMP);
    }

    @Test
    public void device_setTraffic_doesnt_set_anything_if_invalid_values() {
        long TX = 5432345;
        long RX = 498902;
        long TIMESTAMP = System.currentTimeMillis();
        Device device = new Device("", "", "");
        device.setTrafficStats(TX, RX, TIMESTAMP);
        device.setTrafficStats(-1, 123, TIMESTAMP + 1);
        device.setTrafficStats(123, -1, TIMESTAMP + 2);
        device.setTrafficStats(123, 123, TIMESTAMP - 1);
        assertEquals(device.rxTraffic(), RX);
        assertEquals(device.txTraffic(), TX);
        assertEquals(device.timestamp(), TIMESTAMP);
    }

    @Test
    public void device_setTraffic_updates_speed() {
        long TX = 5432345;
        long RX = 498902;
        long TIMESTAMP = System.currentTimeMillis();
        Device device = new Device("", "", "");
        device.setTrafficStats(TX, RX, TIMESTAMP);
        float speed = (TX + RX) / TIMESTAMP;
        assertEquals(device.lastSpeed(), speed, 0);
        device.setTrafficStats(TX + 100, RX + 100, TIMESTAMP + 100);
        assertEquals(device.lastSpeed(), 2, 0);
    }

    @Test
    public void network_static_values_exist() {
        assertNotNull(Network.LAN);
        assertNotNull(Network.WAN);
    }
    @Test
    public void network_constructor_sets_proper_fields() {
        String ROUTERID = "lskdfj";
        String NETWORKID = "29hhfhla;l";
        Network network = new Network(ROUTERID, NETWORKID);
        assertEquals(ROUTERID, network.routerId());
        assertEquals(NETWORKID, network.networkId());
    }

    @Test
    public void network_name_returns_custom_name_if_available() {
        String NAME = "theName";
        String CUSTOM = "customName";
        Network network = new Network("R", NAME);
        assertTrue(network.name().equals(NAME));
        assertTrue(network.customName() == null);
        network.setCustomName(CUSTOM);
        assertTrue(network.name().equals(CUSTOM));
        assertTrue(network.customName().equals(CUSTOM));
        assertTrue(network.networkId().equals(NAME));
        network.setCustomName("");
        assertTrue(network.name().equals(NAME));
    }

    @Test
    public void network_set_details_sets_correct_properties() {
        String NAME = "22930";
        long TX = 23904009;
        long RX = 22893883;
        long TIME = System.currentTimeMillis();
        float SPEED = 89.3F;
        Network network = new Network("", "");
        network.setDetails(NAME, TX, RX, TIME, SPEED);
        assertEquals(NAME, network.customName());
        assertEquals(TX, network.txBytes());
        assertEquals(RX, network.rxBytes());
        assertEquals(TIME, network.timestamp());
        assertEquals(SPEED, network.speed(), 0);
    }

    @Test
    public void wifi_constructor_sets_proper_fields() {
        String SSID = "klkakff";
        Wifi wifi = new Wifi(SSID);
        assertEquals(SSID, wifi.SSID());
    }

    @Test
    public void wifi_setters_and_getters_work() {
        String PASSWORD = "spazworkd";
        Wifi wifi = new Wifi("");
        wifi.setPassword(PASSWORD);
        assertEquals(PASSWORD, wifi.password());
    }

    @Test
    public void speed_setters_and_getters_work() {
        String routerId="RouterId29003";
        long timestamp = System.currentTimeMillis();
        long lanSpeed = 983909;
        long wanSpeed = 3939029;
        Speed speed = new Speed(routerId, timestamp, lanSpeed, wanSpeed);
        assertEquals(routerId, speed.routerId());
        assertEquals(timestamp, speed.timestamp());
        assertEquals(lanSpeed, speed.lanSpeed(), 0);
        assertEquals(wanSpeed, speed.wanSpeed(), 0);
    }

}


package com.brentandjody.tomatohub;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;
import android.test.InstrumentationTestCase;

import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Networks;
import com.brentandjody.tomatohub.routers.LinuxRouter;
import com.brentandjody.tomatohub.routers.Router;
import com.brentandjody.tomatohub.routers.connection.IConnection;
import com.brentandjody.tomatohub.routers.connection.SshConnection;
import com.brentandjody.tomatohub.routers.connection.TelnetConnection;
import com.brentandjody.tomatohub.routers.connection.TestableConnection;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created by brentn on 29/12/15.
 * Basic unit tests that don't require an emulator and are generic for linux routers
 */



@RunWith(MockitoJUnitRunner.class)
public class LinuxRouterTests extends InstrumentationTestCase {
    private static final String IP_ADDRESS = "1.2.3.3";
    private static final String USERNAME = "UserName";
    private static final String PASSWORD = "PaSsWoRd";
    private static final String ROUTERID = "89:44:25:D9:90:3A";
    Activity fakeActivity;
    SharedPreferences fakePreferences;
    TestableConnection fakeConnection;
    CountDownLatch initialized;

    @Before
    public void setup() {
        fakeActivity = mock(MainActivity.class);

        when(fakeActivity.getString(R.string.sharedPreferences_name)).thenReturn("wrtHub");
        when(fakeActivity.getString(R.string.pref_key_ip_address)).thenReturn("ip_address");
        when(fakeActivity.getString(R.string.pref_key_username)).thenReturn("username");
        when(fakeActivity.getString(R.string.pref_key_password)).thenReturn("password");
        when(fakeActivity.getString(R.string.pref_key_protocol)).thenReturn("protocol");
    }
    public void setupPreferences() {
        fakePreferences = mock(SharedPreferences.class);
        when(fakePreferences.getString(eq("ip_address"), anyString())).thenReturn(IP_ADDRESS);
        when(fakePreferences.getString(eq("username"), anyString())).thenReturn(USERNAME);
        when(fakePreferences.getString(eq("password"), anyString())).thenReturn(PASSWORD);
        when(fakePreferences.getString(eq("protocol"), anyString())).thenReturn("ssh");
        when(fakeActivity.getSharedPreferences("wrtHub", Context.MODE_PRIVATE)).thenReturn(fakePreferences);
    }
    public void setupConnection() {
        fakeConnection = mock(TestableConnection.class);
        when(fakeConnection.execute("arp")).thenReturn(new String[] {"myComputer (192.168.1.24) at C4:D9:11:22:33:44 [ether]  on br0"});
        when(fakeConnection.execute("nvram show")).thenReturn(new String[] {"wan_hwaddr="+ROUTERID});
        when(fakeConnection.execute("bctrl show")).thenReturn(new String[] {"bridge name	bridge id		STP enabled	interfaces","br0		8000.0020deca1447	no		vlan0","							eth1","							wl0.1"});
        when(fakeConnection.execute("for x in 0 1 2 3 4 5 6 7; do wl ssid -C $x 2>/dev/null; done")).thenReturn(new String[] {"Current SSID: \"home\""});
        when(fakeConnection.execute("cat /etc/motd")).thenReturn(new String[] {"","DD-WRT v24-sp2"," http://www.dd-wrt.com"});
        when(fakeConnection.execute("iptables -t filter -nL")).thenReturn(new String[] {"Chain INPUT (policy ACCEPT)","target     prot opt source               destination         ","ACCEPT     0    --  0.0.0.0/0            0.0.0.0/0           state RELATED,ESTABLISHED"});
    }
    @Test
    public void constructor_fails_without_onRouterActivityCompleteListener() {
        Activity activity = mock(Activity.class);
        try {
            LinuxRouter router = new LinuxRouter(activity, null, null);
            Assert.fail();
        } catch (RuntimeException ex) {
            Assert.assertTrue(ex.getMessage().contains("OnRouterActivityCompleteListener"));
        }
    }

    @Test
    public void constructor_loads_preferences() {
        setupPreferences();
        LinuxRouter router = new LinuxRouter(fakeActivity, null, null);
        assertEquals(router.getmIpAddress(), IP_ADDRESS);
        assertEquals(router.getmUser(), USERNAME);
        assertEquals(router.getmPassword(), PASSWORD);
    }

    @Test
    public void constructor_initiates_ssh_connection() {
        setupPreferences();
        when(fakePreferences.getString(eq("protocol"), anyString())).thenReturn("ssh");
        LinuxRouter router = new LinuxRouter(fakeActivity, null, null);
        assertTrue(router.getmConnection() instanceof SshConnection);
    }

    @Test
    public void constructor_initiates_telnet_connection() {
        setupPreferences();
        when(fakePreferences.getString(eq("protocol"), anyString())).thenReturn("telnet");
        LinuxRouter router = new LinuxRouter(fakeActivity, null, null);
        assertTrue(router.getmConnection() instanceof TelnetConnection);
    }

    @Test
    public void constructor_handles_invalid_protocol() {
        setupPreferences();
        when(fakePreferences.getString(eq("protocol"), anyString())).thenReturn("mush");
        LinuxRouter router = new LinuxRouter(fakeActivity, null, null);
        assertNull(router.getmConnection());
    }

    @Test
    public void constructor_sets_devices_and_networks() {
        Devices devices = new Devices(fakeActivity);
        Networks networks = new Networks(fakeActivity);
        LinuxRouter router = new LinuxRouter(fakeActivity, devices, networks);
        assertEquals(devices, router.getmDevicesDB());
        assertEquals(networks, router.getmNetworksDB());
    }

    @Test
    public void connect_sets_up_connection() {
        setupPreferences();
        TestableConnection fakeConnection = mock(TestableConnection.class);
        LinuxRouter router = new LinuxRouter(fakeActivity, null, null);
        router.setmConnection(fakeConnection);
        router.connect();
        verify(router.getmConnection(), times(1)).connect(IP_ADDRESS, USERNAME, PASSWORD);
    }

    @Test
    public void disconnect_tears_down_connnection() {
        TestableConnection fakeConnection = mock(TestableConnection.class);
        LinuxRouter router = new LinuxRouter(fakeActivity, null, null);
        router.setmConnection(fakeConnection);
        router.disconnect();
        verify(router.getmConnection(), times(1)).disconnect();
    }

    @Test
    public void disconnect_cancels_all_running_tasks() {
        int ITEMS=5;
        List<AsyncTask> tasklist = new ArrayList<>();
        for(int i=0; i<ITEMS; i++) {
            tasklist.add(mock(AsyncTask.class));
        }
        LinuxRouter router = new LinuxRouter(fakeActivity, null, null);
        router.setmRunningTasks(tasklist);
        router.disconnect();
        for(int i=0; i<ITEMS; i++) {
            verify(tasklist.get(i), times(1)).cancel(true);
        }
    }

    @Test
    public void initialize_gets_values_for_all_caches() throws Throwable {
        InstrumentationTestCase itc = new InstrumentationTestCase();
        itc.runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                initialized=new CountDownLatch(1);
                class TestableMain extends MainActivity {
                    @Override
                    public void onRouterActivityComplete(int activity_id, int status) {
                        switch (activity_id) {
                            case Router.ACTIVITY_INTIALIZE:initialized.countDown();break;
                        }
                    }
                }
                fakeActivity = new TestableMain();
                setupConnection();
                final LinuxRouter router = new LinuxRouter(fakeActivity, null, null);
                router.setmConnection(fakeConnection);
                assertNull(router.getCacheNVRam());
                assertNull(router.getCacheBrctl());
                assertNull(router.getCacheMotd());
                assertNull(router.getCacheIptables());
                router.initialize();
                try {
                    initialized.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                assertTrue(router.getCacheNVRam().length>0);
                assertTrue(router.getCacheBrctl().length>0);
                assertTrue(router.getCacheMotd().length>0);
                assertTrue(router.getCacheIptables().length>0);
            }
        });
    }
}

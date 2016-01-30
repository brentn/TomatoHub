package com.brentandjody.tomatohub;

import android.content.Context;
import android.test.mock.MockContext;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.brentandjody.tomatohub.overview.OverviewFragment;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;

import static org.mockito.Mockito.mock;

/**
 * Created by brentn on 28/01/16.
 */

@RunWith(MockitoJUnitRunner.class)
public class OverviewFragmentTests extends TestCase {
    TestableOverviewFragment mOverviewFragment;
    MainActivity fakeActivity;

    @Before
    public void setup() {
        fakeActivity = mock(MainActivity.class);
        mOverviewFragment = new TestableOverviewFragment();
        mOverviewFragment.onAttach((Context)fakeActivity);
    }

    @Test
    public void static_values_exist() {
        assertNotNull(OverviewFragment.SIGNAL_LOADED);
        assertNotNull(OverviewFragment.SIGNAL_REFRESH);
        assertNotNull(OverviewFragment.SIGNAL_REBOOT);
        assertNotNull(OverviewFragment.SIGNAL_BLOCK);
        assertNotNull(OverviewFragment.SIGNAL_UNBLOCK);
        assertNotNull(OverviewFragment.SIGNAL_PRIORITIZE);
    }

    @Test
    public void onAttach_requires_onSignalListener() {
        mOverviewFragment = new TestableOverviewFragment();
        try {
            mOverviewFragment.onAttach(new MockContext());
            fail();
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("OnSignalListener"));
        }
        mOverviewFragment.onAttach((Context)fakeActivity);
    }

    @Test
    public void initialize_resets_everything() {
        mOverviewFragment.setQOSEnabled(true);
        mOverviewFragment.setWifiMessage("Wifi");
        mOverviewFragment.setStatusMessage("Status");
        mOverviewFragment.showRouter(true);
        mOverviewFragment.showSpeedTestButton(true);
        mOverviewFragment.showDetailView("");

        mOverviewFragment.initialize();

        assertFalse(mOverviewFragment.isQOSEnabled());
        assertEquals("", mOverviewFragment.getWifiMessage());
        assertEquals("", mOverviewFragment.getStatusMessage());
        assertEquals(":", mOverviewFragment.getDevicesMessage());
        assertFalse(mOverviewFragment.ismRouterVisible());
        assertFalse(mOverviewFragment.isSpeedTestButtonVisible());
        assertFalse(mOverviewFragment.areNetworksVisible());
        assertFalse(mOverviewFragment.isDetailViewVisible());
        fail();
    }

    @Test
    public void test_setters() {
        fail();
    }

    @Test
    public void showRouter_affects_icon_line_and_label() {
        fail();
    }

    @Test
    public void showRouter_calls_hideAllNetworkIcons_when_hiding() {
        fail();
    }

    @Test
    public void showSpeedTestButton_affects_visibility_of_FAB() {
        fail();
    }

    @Test
    public void hideAllNetworkIcons_affects_icons_lines_and_labels() {
        fail();
    }

    @Test
    public void hideAllNetworkIcons_hides_all_five_networks() {
        fail();
    }

    @Test
    public void showNetwork_shows_network_line_and_label() {
        fail();
    }

    @Test
    public void showNetwork_sets_device_count_correctly() {
        fail();
    }

    @Test
    public void showNetwork_sets_tag_to_networkId() {
        fail();
    }

    @Test
    public void showNetwork_sets_label_to_network_name_or_custom_name() {
        fail();
    }

    @Test
    public void onCreateView_loads_devices_and_networks() {
        fail();
    }

    @Test
    public void clicking_anywhere_on_view_closes_detailView() {
        fail();
    }

    @Test
    public void clicking_on_FAB_launches_speedTest() {
        fail();
    }

    public class TestableOverviewFragment extends OverviewFragment {
        private boolean mRouterVisible;
        private boolean mSpeedTesstButtonVisible;

        public TestableOverviewFragment() {
            mWifiMessage = new TextView(fakeActivity);
            mStatusMessage = new TextView(fakeActivity);
            mDevicesMessage = new TextView[] {new TextView(fakeActivity), new TextView(fakeActivity)};
            mSpeedTesstButtonVisible=false;
            mNetworkLabels = new ArrayList<>();
            mNetworkIcons = new TextView[] {new TextView(fakeActivity)};
            mNetworkLines = new View[0];
            mDetailView = new LinearLayout(fakeActivity);
        }

        @Override
        public void showRouter(boolean visible) {
            mRouterVisible=visible;
        }

        @Override
        public void showSpeedTestButton(boolean visible) {
            mSpeedTesstButtonVisible=visible;
        }

        @Override
        protected void showDetailView(String network_name) {
            mDetailViewVisible=true;
        }

        public boolean isQOSEnabled() {return mQOSEnabled;}
        public boolean ismRouterVisible() {return mRouterVisible;}
        public boolean isSpeedTestButtonVisible() {return mSpeedTesstButtonVisible;}
        public boolean areNetworksVisible() {return mNetworkIcons[0].getVisibility() == View.VISIBLE;}
        public String getWifiMessage() {return mWifiMessage.getText().toString();}
        public String getStatusMessage() {return mStatusMessage.getText().toString();}
        public String getDevicesMessage() {return mDevicesMessage[0].getText().toString()+":"+mDevicesMessage[1].getText().toString();}
    }

}

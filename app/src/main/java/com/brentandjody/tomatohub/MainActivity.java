package com.brentandjody.tomatohub;

import android.content.Intent;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;

import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.brentandjody.tomatohub.database.Device;
import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Network;
import com.brentandjody.tomatohub.overview.OverviewFragment;
import com.brentandjody.tomatohub.routers.Router;
import com.brentandjody.tomatohub.routers.TomatoRouter;
import com.brentandjody.tomatohub.database.Networks;
import com.brentandjody.tomatohub.dummy.DummyContent;
import com.brentandjody.tomatohub.wifi.WifiFragment;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements Router.OnRouterActivityCompleteListener,
                    OverviewFragment.OnLoadedListener,
                    WifiFragment.OnListFragmentInteractionListener {

    private static final String TAG = MainActivity.class.getName();
    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;
    private Router mRouter;
    private Networks mNetworks;
    private Devices mDevices;
    private OverviewFragment mOverviewFragment;
    private WifiFragment mWifiFragment;

    public MainActivity() {
        mNetworks = new Networks(this);
        mDevices = new Devices(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mRouter = new TomatoRouter(this, mDevices, mNetworks);
    }

    @Override
    public void onLoaded() {
        mOverviewFragment.setStatusMessage(getString(R.string.searching_for_router));
    }

    @Override
    public void onRouterActivityComplete(int activity_id, int status) {
        switch (activity_id) {
            case Router.ACTIVITY_LOGON: {
                if (status==Router.ACTIVITY_STATUS_SUCCESS) {
                    mOverviewFragment.showRouter(true);
                    mOverviewFragment.setStatusMessage(getString(R.string.scanning_network));
                    mRouter.initialize();
                } else {
                    mOverviewFragment.showRouter(false);
                    mOverviewFragment.setStatusMessage(getString(R.string.connection_failure));
                    Log.i(TAG, "Redirecting to Welcome screen");
                    Intent intent = new Intent(this, WelcomeActivity.class);
                    intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                    this.startActivity(intent);
                }
                break;
            }
            case Router.ACTIVITY_INTIALIZE: {
                if (status==Router.ACTIVITY_STATUS_SUCCESS) {
                    mOverviewFragment.setRouterId(mRouter.getRouterId());
                    mOverviewFragment.setStatusMessage(getString(R.string.everything_looks_good));
                    String wifiMessage =
                            "'" + TextUtils.join("'"+getString(R.string.is_on)+",  '", mRouter.getWIFILabels())
                            + "'" + getString(R.string.is_on);
                    mOverviewFragment.setWifiMessage(wifiMessage);
                    mOverviewFragment.setDevicesMessage(mRouter.getTotalDevices()+" "+getString(R.string.devices), getString(R.string.are_connected));
                    mRouter.updateDevices();
                    mRouter.updateTrafficStats();
                } else {
                    mOverviewFragment.setStatusMessage(getString(R.string.scan_failure));
                }
                break;
            }
            case Router.ACTIVITY_DEVICES_UPDATED: {
                String router_id = mRouter.getRouterId();
                String[] networks = mRouter.getNetworkIds();
                for (int i=0; i<networks.length; i++) {
                    Network network = mNetworks.get(router_id, networks[i]);
                    List<Device> devices = mDevices.getDevicesOnNetwork(router_id, network.networkId());
                    mOverviewFragment.showNetwork(i, network.name(), devices.size());
                    mOverviewFragment.setupClickListener(i);
                }
                break;
            }
            case Router.ACTIVITY_TRAFFIC_UPDATED: {
                String[] network_ids = mRouter.getNetworkIds();
                for (int i=0; i<network_ids.length; i++) {
                    mOverviewFragment.setupClickListener(i);
                }
                if (status==Router.ACTIVITY_STATUS_SUCCESS) {
//                    if (network_ids.length > 1) {
//                        float total_traffic = 0;
//                        Network[] networks = new Network[mRouter.getNetworkIds().length];
//                        for (int i = 0; i < network_ids.length; i++) {
//                            networks[i] = mNetworks.get(mRouter.getRouterId(), network_ids[i]);
//                            total_traffic += networks[i].speed();
//                        }
//                        for (int i = 0; i < network_ids.length; i++) {
//                            mOverviewFragment.setNetworkTrafficColor(i, networks[i].speed() / total_traffic);
//                        }
//                    }
                }
                break;
            }
        }
    }


    @Override
    protected void onPause() {
        if (mOverviewFragment.isDetailViewVisible())
            mOverviewFragment.hideDetailView();
        if (mRouter!=null)
            mRouter.disconnect();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mOverviewFragment!=null) {
            mOverviewFragment.showRouter(false);
            mOverviewFragment.hideAllNetworkIcons();
            mOverviewFragment.setStatusMessage(getString(R.string.searching_for_router));
        }
        mRouter.connect();
    }

    @Override
    public void onListFragmentInteraction(DummyContent.DummyItem item) {
        //TODO:
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mViewPager.getCurrentItem()==0 && mOverviewFragment.isDetailViewVisible()) {
            mOverviewFragment.hideDetailView();
        } else {
            super.onBackPressed();
        }
    }


    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position==0) {
                mOverviewFragment = OverviewFragment.newInstance(mNetworks, mDevices);
                return mOverviewFragment;
            }
            else {
                mWifiFragment = WifiFragment.newInstance();
                return mWifiFragment;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.title_overview);
                case 1:
                    return getString(R.string.title_wifi_access);
            }
            return null;
        }
    }





}

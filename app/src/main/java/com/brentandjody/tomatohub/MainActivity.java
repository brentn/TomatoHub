package com.brentandjody.tomatohub;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Network;
import com.brentandjody.tomatohub.database.Networks;
import com.brentandjody.tomatohub.database.Wifi;
import com.brentandjody.tomatohub.overview.OverviewFragment;
import com.brentandjody.tomatohub.routers.DDWrtRouter;
import com.brentandjody.tomatohub.routers.FakeRouter;
import com.brentandjody.tomatohub.routers.LinuxRouter;
import com.brentandjody.tomatohub.routers.Router;
import com.brentandjody.tomatohub.routers.RouterType;
import com.brentandjody.tomatohub.routers.TomatoRouter;
import com.brentandjody.tomatohub.wifi.WifiFragment;

public class MainActivity extends AppCompatActivity
        implements Router.OnRouterActivityCompleteListener,
                    OverviewFragment.OnSignalListener,
                    WifiFragment.OnSignalListener {

    private static final String TAG = MainActivity.class.getName();
    private static final int SETTINGS_REQUEST_CODE = 38;
    private static final String OVERVIEW_FRAGMENT = "OVERVIEW_FRAGMENT";
    private static final String WIFI_FRAGMENT = "WIFI_FRAGMENT";
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private boolean mConnecting=false;
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

    public Devices getDevices() { return mDevices; }
    public Networks getNetworks() { return mNetworks; }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mOverviewFragment!=null)
            getSupportFragmentManager().putFragment(outState, OVERVIEW_FRAGMENT, mOverviewFragment);
        if (mWifiFragment!=null)
            getSupportFragmentManager().putFragment(outState, WIFI_FRAGMENT, mWifiFragment);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState!=null) {
            mOverviewFragment = (OverviewFragment) getSupportFragmentManager().getFragment(savedInstanceState, OVERVIEW_FRAGMENT);
            mWifiFragment = (WifiFragment) getSupportFragmentManager().getFragment(savedInstanceState, WIFI_FRAGMENT);
        }

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPreferences_name), MODE_PRIVATE);

        String MISSING_VALUE = "missing";
        if (prefs.getString(getString(R.string.pref_key_router_type), MISSING_VALUE).equals(MISSING_VALUE)) {
            startActivity(new Intent(MainActivity.this, FirstRun_Activity.class));
        }
    }

    @Override
    public void onSignal(int signal, String parameter) {
        switch (signal) {
            case OverviewFragment.SIGNAL_LOADED: {
                if (mOverviewFragment!=null) {
                    mOverviewFragment.setStatusMessage(getString(R.string.searching_for_router));
                }
                break;
            }
            case OverviewFragment.SIGNAL_REBOOT: {
                reboot();
                break;
            }
            case OverviewFragment.SIGNAL_REFRESH: {
                refresh();
                break;
            }
            case OverviewFragment.SIGNAL_BLOCK: {
                if (parameter!=null && parameter.length()==17) {
                    mRouter.command("if ! lsmod|grep mac; then insmod xt_mac; insmod ipt_mac; fi");
                    mRouter.command("iptables -I FORWARD -m mac --mac-source "+parameter + " -j DROP");
                    mRouter.command("iptables -I INPUT   -m mac --mac-source "+parameter + " -j DROP");
                    mRouter.updateDevices();
                }
                break;
            }
            case OverviewFragment.SIGNAL_UNBLOCK: {
                if (parameter!=null && parameter.length()==17) {
                    mRouter.command("iptables -D FORWARD -m mac --mac-source " + parameter + " -j DROP");
                    mRouter.command("iptables -D INPUT   -m mac --mac-source " + parameter + " -j DROP");
                    mRouter.updateDevices();
                }
                break;
            }
            case OverviewFragment.SIGNAL_PRIORITIZE: {
                if (parameter!=null && parameter.contains(":")) {
                    String[] fields = parameter.split(":");
                    String ip = fields[0];
                    try {
                        long until = System.currentTimeMillis() + Long.parseLong(fields[1]);
                        mRouter.prioritize(ip, until);
                    } catch (Exception ex) {
                        Log.e(TAG, "couldn't parse time value");
                    }
                }
                break;
            }
            case WifiFragment.SIGNAL_CHANGE_WIFI_PASSWORD: {
                if (parameter!=null && parameter.contains("\n")) {
                    String ssid = parameter.split("\n")[0];
                    String newPassword = parameter.split("\n")[1];
                    Wifi wifi = null;
                    for (Wifi w : mRouter.getWifiList()) {
                        if (w.SSID().equals(ssid)) wifi = w;
                    }
                    if (wifi != null) {
                        mRouter.setWifiPassword(wifi, newPassword);
                    }
                    finish();
                }
                break;
            }
            case WifiFragment.SIGNAL_DISABLE_WIFI: {
                mRouter.enableWifi(parameter, false);
                break;
            }
            case WifiFragment.SIGNAL_ENABLE_WIFI: {
                mRouter.enableWifi(parameter, true);
                break;
            }
            case WifiFragment.SIGNAL_HIDE_WIFI: {
                mRouter.broadcastWifi(parameter, false);
                break;
            }
            case WifiFragment.SIGNAL_SHOW_WIFI: {
                mRouter.broadcastWifi(parameter, true);
                break;
            }
        }
    }

    @Override
    public void onRouterActivityComplete(int activity_id, int status) {
        switch (activity_id) {
            case Router.ACTIVITY_CONNECTED: {
                if (status==Router.ACTIVITY_STATUS_SUCCESS) {
                    if (mOverviewFragment!=null) {
                        mOverviewFragment.showRouter(true);
                        mOverviewFragment.setStatusMessage(getString((mConnecting?R.string.scanning_network:R.string.rescannng_network)));
                    }
                    mRouter.initialize();
                } else {
                    if (mOverviewFragment!=null) {
                        mOverviewFragment.showRouter(false);
                        mOverviewFragment.showSpeedTestButton(false);
                        mOverviewFragment.setStatusMessage(getString(R.string.connection_failure));
                        mOverviewFragment.setupInternetClickListener("");
                    }
                }
                break;
            }
            case Router.ACTIVITY_INTIALIZE: {
                if (status==Router.ACTIVITY_STATUS_SUCCESS) {
                    if (mOverviewFragment!=null) {
                        mOverviewFragment.setupInternetClickListener(mRouter.getExternalIP());
                        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                        String myIp = Router.intToIp(wifi.getConnectionInfo().getIpAddress());
                        mOverviewFragment.setMyMac(mRouter.getMacForIp(myIp));
                        mOverviewFragment.setRouterId(mRouter.getRouterId());
                        mOverviewFragment.setQOSEnabled(mRouter.isQOSEnabled());
                        if (mRouter.getNetworkIds().length < 1) {
                            mOverviewFragment.setStatusMessage(getString(R.string.no_networks_found));
                            mOverviewFragment.setDevicesMessage("","");
                        } else {
                            mOverviewFragment.setStatusMessage(getString(R.string.everything_looks_good));
                            mOverviewFragment.setDevicesMessage(mRouter.getTotalDevices() + " " + getString(R.string.devices), getString(R.string.are_connected));
                        }
                        mOverviewFragment.setWifiMessage(buildWifiMessage());
                        mOverviewFragment.setupRouterClickListener(
                                mRouter.getRouterType(),
                                mRouter.getInternalIP(),
                                mRouter.getBootTime(),
                                mRouter.getMemoryUsage(),
                                mRouter.getCPUUsage()
                        );
                    }
                    if (mWifiFragment != null) {
                        mWifiFragment.setWifiList(mRouter.getWifiList());
                    }
                    mRouter.updateDevices();
                    mRouter.updateTrafficStats();
                } else {
                    if (mOverviewFragment!=null) {
                        mOverviewFragment.setStatusMessage(getString(R.string.scan_failure));
                    }
                }
                break;
            }
            case Router.ACTIVITY_DEVICES_UPDATED: {
                if (mOverviewFragment!=null) {
                    String router_id = mRouter.getRouterId();
                    String[] networks = mRouter.getNetworkIds();
                    for (int i = 0; i < networks.length; i++) {
                        Network network = mNetworks.get(router_id, networks[i]);
                        int total = mRouter.getTotalDevicesOn(networks[i]);
                        mOverviewFragment.showNetwork(i, network.networkId(), total);
                        mOverviewFragment.setupNetworkClickListener(i);
                    }
                    mOverviewFragment.showSpeedTestButton(true);
                }
                break;
            }
            case Router.ACTIVITY_TRAFFIC_UPDATED: {
                if (mOverviewFragment!=null) {
                    String[] network_ids = mRouter.getNetworkIds();
                    for (int i = 0; i < network_ids.length; i++) {
                        mOverviewFragment.setupNetworkClickListener(i);
                    }
                    if (status == Router.ACTIVITY_STATUS_SUCCESS) {
                        if (network_ids.length > 1) {
                            float total_traffic = 0;
                            Network[] networks = new Network[mRouter.getNetworkIds().length];
                            for (int i = 0; i < network_ids.length; i++) {
                                networks[i] = mNetworks.get(mRouter.getRouterId(), network_ids[i]);
                                total_traffic += networks[i].speed();
                            }
                            for (int i = 0; i < network_ids.length; i++) {
                                mOverviewFragment.setNetworkTrafficColor(i, networks[i].speed() / total_traffic);
                            }
                        }
                    }
                }
                break;
            }
            case Router.ACTIVITY_WIFI_UPDATED: {
                mWifiFragment.setWifiList(mRouter.getWifiList());
                mOverviewFragment.setWifiMessage(buildWifiMessage());
                break;
            }
            case Router.ACTIVITY_BACKGROUND_COMMAND: {
                Log.d(TAG, "Background activity complete");
                if (status==Router.ACTIVITY_STATUS_EXIT) {
                    Log.d(TAG, "Exiting");
                    finish();
                }
                break;
            }
        }
    }


    @Override
    protected void onPause() {
        if (mOverviewFragment!=null && mOverviewFragment.isDetailViewVisible())
            mOverviewFragment.hideDetailView();
        if (mRouter!=null)
            mRouter.disconnect();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mRouter.disconnect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectRouter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mConnecting)
            refresh();
        mConnecting=false;
    }

    private void connectRouter() {
        mConnecting=true;
        Log.d(TAG, "Instantiating router...");
        switch (getRouterType()) {
            case RouterType.TOMATO: mRouter = new TomatoRouter(this, mDevices, mNetworks); break;
            case RouterType.DDWRT: mRouter = new DDWrtRouter(this, mDevices, mNetworks); break;
            case RouterType.FAKE: mRouter = new FakeRouter(this);
                Toast.makeText(this, R.string.running_in_demo_mode, Toast.LENGTH_LONG).show();
                break;
            default: mRouter = new LinuxRouter(this, mDevices, mNetworks);
        }
        Log.d(TAG, "Connecting...");
        mRouter.connect();
    }

    private void refresh() {
        if (mOverviewFragment!=null) {
            mOverviewFragment.setWifiMessage("");
            mOverviewFragment.setDevicesMessage("","");
            mOverviewFragment.hideAllNetworkIcons();
            mOverviewFragment.showRouter(false);
            mOverviewFragment.setStatusMessage(getString(R.string.searching_for_router));
        }
        connectRouter();
        mRouter.initialize();
    }

    private void reboot() {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPreferences_name), MODE_PRIVATE);
        if (prefs.getBoolean(getString(R.string.pref_key_allow_changes), false)) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.confirm_reboot))
                    .setMessage(getString(R.string.reboot_explanation))
                    .setNeutralButton(getString(R.string.reboot), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mRouter.reboot();
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle(getString(R.string.router_is_rebooting))
                                    .setMessage(getString(R.string.wait_for_reboot))
                                    .setCancelable(false)
                                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                                    .show();
                        }
                    })
                    .show();
        }
    }

    private String buildWifiMessage() {
        String wifiMessage = "";
        for (Wifi w : mRouter.getWifiList()) {
            wifiMessage += "'"+w.SSID()+"'"
                    + getString(w.enabled()?R.string.is_on:R.string.is_off)
                    + getString((w.enabled() & !w.broadcast())?R.string.but_hidden:R.string.empty_string)
                    + ",  ";
        }
        wifiMessage = wifiMessage.replaceAll(", $", "");
        if (wifiMessage.isEmpty()) {
            wifiMessage=getString(R.string.wifi_not_enabled);
        }
        return wifiMessage;
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
            startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), SETTINGS_REQUEST_CODE);
            return true;
        }
        if (id == R.id.action_help) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(Uri.parse(getString(R.string.wrthub_website)));
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mViewPager.getCurrentItem()==0 && mOverviewFragment!=null && mOverviewFragment.isDetailViewVisible()) {
            mOverviewFragment.hideDetailView();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST_CODE) {
            mConnecting=true;
            if (mOverviewFragment!=null) {
                mOverviewFragment.initialize();
                mOverviewFragment.setStatusMessage(getString(R.string.rescannng_network));
            }
            mRouter.connect();
            if (resultCode == RESULT_OK) {
                if (data != null && data.hasExtra(SettingsActivity.REBOOT_AFTER_SETTINGS)) {
                    finish();
                }
            }
        }
    }

    public int getRouterType() {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPreferences_name), MODE_PRIVATE);
        return RouterType.value(prefs.getString(getString(R.string.pref_key_router_type), null));
    }



    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position==0) {
                mOverviewFragment = new OverviewFragment();
                return mOverviewFragment;
            }
            else {
                mWifiFragment = new WifiFragment();
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

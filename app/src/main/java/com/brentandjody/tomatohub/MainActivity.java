package com.brentandjody.tomatohub;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import android.view.View;

import android.widget.ImageView;
import android.widget.TextView;

import com.jcraft.jsch.*;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements OverviewFragment.OnFragmentInteractionListener {

    public static final String routerIPPref = "prefRouterIP";
    public static final  String routerPort = "prefRouterPort";
    public static final  String routerUserPref = "prefRouterUser";
    public static final String routerPasswordPref = "prefRouterPass";
    private static final String TAG = MainActivity.class.getName();

    private SharedPreferences mPrefs;
    private Session mSession;
    private String mIpAddress;
    private int mPort;
    private String mUser;
    private String mPassword;
    private boolean mStartActivityHasBeenRun;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefs = getSharedPreferences("Application", Context.MODE_PRIVATE);
        mStartActivityHasBeenRun=false;
        mIpAddress = mPrefs.getString(routerIPPref, "0.0.0.0");
        mPort = mPrefs.getInt(routerPort, 22);
        mUser = mPrefs.getString(routerUserPref, "root");
        mPassword = mPrefs.getString(routerPasswordPref, "");


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSession != null) {
            mSession.disconnect();
            mSession = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        new SSHLogon().execute();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
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

    private void setWifiMessage(String message) {
        TextView view = (TextView)mViewPager.findViewById(R.id.wifi_message);
        view.setText(message);
        view.setVisibility(message.isEmpty()?View.INVISIBLE:View.VISIBLE);
    }

    private void setStatusMessage(String message) {
        TextView view = (TextView)mViewPager.findViewById(R.id.status_message);
        view.setText(message);
        view.setVisibility(message.isEmpty()?View.INVISIBLE:View.VISIBLE);
    }

    private void setDevicesMessage(String numDevices, String deviceStatus) {
        TextView devices = (TextView)mViewPager.findViewById(R.id.devices);
        TextView are_connected = (TextView)mViewPager.findViewById(R.id.are_connected);
        devices.setText(numDevices);
        devices.setVisibility(numDevices.isEmpty()?View.INVISIBLE:View.VISIBLE);
        are_connected.setText(deviceStatus);
        are_connected.setVisibility(deviceStatus.isEmpty()?View.INVISIBLE:View.VISIBLE);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return OverviewFragment.newInstance();
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "OVERVIEW";
                case 1:
                    return "WI-FI ACCESS";
            }
            return null;
        }
    }

    private class SSHLogon extends AsyncTask<Void,Void,Void>
    {
        boolean success;
        @Override
        protected Void doInBackground(Void... voids) {
            JSch ssh = new JSch();
            try {
                if (mSession!=null) mSession.disconnect();
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                mSession = ssh.getSession(mUser, mIpAddress, mPort);
                mSession.setConfig(config);
                mSession.setPassword(mPassword);
                mSession.connect(8000);
                success = true;
            } catch (Exception ex) {
                success = false;
                if (mSession!=null)
                    mSession.disconnect();
                Log.e(TAG, ex.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                ImageView view = (ImageView) mViewPager.findViewById(R.id.router);
                if (view != null) view.setVisibility(success ? View.VISIBLE : View.INVISIBLE);
                if (success) {
                    setStatusMessage("Everything looks good.");
                    new ValueInitializer().execute();
                } else {
                    if (!mStartActivityHasBeenRun) {
                        mStartActivityHasBeenRun = true;
                        Log.i(TAG, "Redirecting to Welcome screen");
                        startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
                    } else {
                        setStatusMessage("Could not connect to router.");
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "SSHLogon.postExecute:"+ex.getMessage());
            }
        }
    }

    private class ValueInitializer extends AsyncTask<Void, Void, Void> {
        String mWAN;
        String[] mNetworks;
        String[] mWifi;
        String[][] mDevices;
        boolean success=false;
        String message = "";
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                // identify the WAN interface
                mWAN = sshCommand("nvram show|grep wan_iface|cut -d= -f2")[0];
                // identify various networks
                mNetworks = sshCommand("arp|cut -d' ' -f8|sort -u|grep -v " + mWAN);
                // identify various wifi networks
                mWifi = sshCommand("nvram show|grep _ssid|cut -d= -f2");
                // enumerate devices on each network
                mDevices = new String[mNetworks.length][];
                for (int i = 0; i < mNetworks.length; i++) {
                    mDevices[i] = sshCommand("arp|grep " + mNetworks[i] );
                }
                success=true;
            } catch(Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                if (success) {
                    int total = 0;
                    for (int i = 0; i < 5; i++) {
                        int id = -1;
                        switch (i) {
                            case 0:
                                id = R.id.lan_0;
                                break;
                            case 1:
                                id = R.id.lan_1;
                                break;
                            case 2:
                                id = R.id.lan_2;
                                break;
                            case 3:
                                id = R.id.lan_3;
                                break;
                            case 4:
                                id = R.id.lan_4;
                                break;
                        }
                        TextView view = (TextView) mViewPager.findViewById(id);
                        if (mNetworks != null && i < mNetworks.length) {
                            total += mDevices[i].length;
                            view.setVisibility(View.VISIBLE);
                            view.setText(String.valueOf(mDevices[i].length));
                        } else {
                            view.setVisibility(View.INVISIBLE);
                        }
                    }
                    setDevicesMessage(String.valueOf(total) + " devices", " are connected.");
                    setWifiMessage("'" + TextUtils.join("' is ON,  '", mWifi) + "' is ON");
                } else {
                    setStatusMessage("Could not scan the network.");
                    Log.e(TAG, message);
                }
            } catch (Exception ex) {
                Log.e(TAG, "ValueInitializer.postExecute:"+ex.getMessage());
            }
        }
    }

    private String[] sshCommand(String command) throws Exception {
        if (mSession!=null) {
            try {
                Channel channel = mSession.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                ByteArrayOutputStream sb = new ByteArrayOutputStream();
                channel.setOutputStream(sb);
                channel.connect();
                while (!channel.isClosed()) {
                    Thread.sleep(10);
                }
                channel.disconnect();
                List<String> lines = Arrays.asList(sb.toString().split("\n"));
                lines.removeAll(Arrays.asList("", null));
                return lines.toArray(new String[lines.size()]);
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
        }
        return new String[0];
    }

}

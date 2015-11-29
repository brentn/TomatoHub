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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.TextView;

import com.jcraft.jsch.*;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements OverviewFragment.OnFragmentInteractionListener {
    public static final String routerIPPref = "prefRouterIP";
    public static final  String routerPort = "prefRouterPort";
    public static final  String routerUserPref = "prefRouterUser";
    public static final String routerPasswordPref = "prefRouterPass";

    private SharedPreferences mPrefs;
    private String mIpAddress;
    private int mPort;
    private String mUser;
    private String mPassword;

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
    protected void onResume() {
        super.onResume();
        new updateStats().execute();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        //TODO:
    }

    private class updateStats extends AsyncTask<Void,Void,Void>
    {
        String mWanInterface;
        String[] mLanInterfaces;
        String[] mLanClients;
        @Override
        protected Void doInBackground(Void... voids) {
            JSch ssh = new JSch();
            try {
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                Session session = ssh.getSession(mUser, mIpAddress, mPort);
                session.setConfig(config);
                session.setPassword(mPassword);
                session.connect(5000);
                mWanInterface = runBySSH("nvram show|grep wan_iface|cut -d= -f2", session)[0];
                mLanInterfaces = runBySSH("arp|cut -d' ' -f8|sort -u|grep -v "+mWanInterface, session);
                mLanClients = new String[mLanInterfaces.length];
                for (int i=0;i<mLanInterfaces.length; i++ ) {
                    mLanClients[i] = runBySSH("arp|grep "+mLanInterfaces[i]+"|wc -l", session)[0];
                }
                session.disconnect();
            } catch(Exception ex) {
                startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            int total=0;
            for (int i=0;i<5;i++){
                int id=-1;
                switch (i){
                    case 0:id=R.id.lan_0;break;
                    case 1:id=R.id.lan_1;break;
                    case 2:id=R.id.lan_2;break;
                    case 3:id=R.id.lan_3;break;
                    case 4:id=R.id.lan_4;break;
                }
                TextView view = (TextView)mViewPager.findViewById(id);
                if (i<mLanClients.length) {
                    try {total += Integer.parseInt(mLanClients[i]);}
                    catch(Exception ex) {}
                    view.setVisibility(View.VISIBLE);
                    view.setText(mLanClients[i]);
                } else {
                    view.setVisibility(View.GONE);
                }
            }
            ((TextView)mViewPager.findViewById(R.id.devices)).setText(String.valueOf(total)+" devices");
        }

    }

    private String[] runBySSH(String command, Session session) throws Exception {
        Channel channel=session.openChannel("exec");
        ((ChannelExec)channel).setCommand(command);
        ByteArrayOutputStream sb=new ByteArrayOutputStream();
        channel.setOutputStream(sb);
        channel.connect();
        while (!channel.isClosed()) {
            Thread.sleep(10);
        }
        channel.disconnect();
        List<String> lines = Arrays.asList(sb.toString().split("\n"));
        lines.removeAll(Arrays.asList("", null));
        return lines.toArray(new String[lines.size()]);
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
}

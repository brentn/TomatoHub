package com.brentandjody.tomatohub;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import com.jcraft.jsch.*;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class MainActivity extends AppCompatActivity
        implements OverviewFragment.OnFragmentInteractionListener {
    public static final String routerIPPref = "prefRouterIP";
    public static final  String routerPort = "prefRouterPort";
    public static final  String routerUserPref = "prefRouterUser";
    public static final String routerPasswordPref = "prefRouterPass";

    private SharedPreferences mPrefs;
    private OverviewFragment mOverview;
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
        mOverview = new OverviewFragment();

        mIpAddress = mPrefs.getString(routerIPPref, "0.0.0.0");
        mPort = mPrefs.getInt(routerPort, 22);
        mUser = mPrefs.getString(routerUserPref, "root");
        mPassword = mPrefs.getString(routerPasswordPref, "");

        new verifySshConnection().execute();

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
    public void onFragmentInteraction(Uri uri) {
        //TODO:
    }

    private class verifySshConnection extends AsyncTask<Void,Void,Void>
    {
        @Override
        protected Void doInBackground(Void... voids) {
            JSch ssh = new JSch();
            try {
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                Session session = ssh.getSession(mUser, mIpAddress, mPort);
                session.setConfig(config);
                session.setPassword(mPassword);
                session.connect(2000);
                session.openChannel("exec");
                session.disconnect();
            } catch(Exception ex) {
                startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
            }
            return null;
        }
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

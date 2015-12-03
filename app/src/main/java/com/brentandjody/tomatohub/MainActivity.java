package com.brentandjody.tomatohub;

import android.content.Intent;

import android.graphics.Color;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;

import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.brentandjody.tomatohub.classes.Router;
import com.brentandjody.tomatohub.classes.TomatoRouter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements OverviewFragment.OnFragmentInteractionListener {

    private static final String TAG = MainActivity.class.getName();
    private Router mRouter;
    private List<TextView> mIconLabels;

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
        mIconLabels = new ArrayList<TextView>();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mRouter = new TomatoRouter(this);

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
        if (mRouter!=null)
            mRouter.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideAllIcons();
        mRouter.connect();
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

    @Override
    public void onBackPressed() {
        Fragment current_fragment = mSectionsPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
        if ( current_fragment instanceof OverviewFragment && ((OverviewFragment)current_fragment).isDetailViewVisible()) {
            ((OverviewFragment)current_fragment).hideDetailView();
        } else {
            super.onBackPressed();
        }
    }

    public void setWifiMessage(String message) {
        TextView view = (TextView)mViewPager.findViewById(R.id.wifi_message);
        view.setText(message);
        view.setVisibility(message.isEmpty()?View.INVISIBLE:View.VISIBLE);
    }

    public void setStatusMessage(String message) {
        TextView view = (TextView)mViewPager.findViewById(R.id.status_message);
        view.setText(message);
        view.setVisibility(message.isEmpty()?View.INVISIBLE:View.VISIBLE);
    }

    public void setDevicesMessage(String numDevices, String deviceStatus) {
        TextView devices = (TextView)mViewPager.findViewById(R.id.devices);
        TextView are_connected = (TextView)mViewPager.findViewById(R.id.are_connected);
        devices.setText(numDevices);
        devices.setVisibility(numDevices.isEmpty()?View.INVISIBLE:View.VISIBLE);
        are_connected.setText(deviceStatus);
        are_connected.setVisibility(deviceStatus.isEmpty()?View.INVISIBLE:View.VISIBLE);
    }

    public void setNetworkText(int i, String text) {
        switch(i) {
            case 0: showIcon(R.id.lan_0, !text.isEmpty());
                showIcon(R.id.lan_0_l, !text.isEmpty());
                setIconText(R.id.lan_0, text);
                break;
            case 1: showIcon(R.id.lan_1, !text.isEmpty());
                showIcon(R.id.lan_1_l, !text.isEmpty());
                setIconText(R.id.lan_1, text);
                break;
            case 2: showIcon(R.id.lan_2, !text.isEmpty());
                showIcon(R.id.lan_2_l, !text.isEmpty());
                setIconText(R.id.lan_2, text);
                break;
            case 3: showIcon(R.id.lan_3, !text.isEmpty());
                showIcon(R.id.lan_3_l, !text.isEmpty());
                setIconText(R.id.lan_3, text);
                break;
            case 4: showIcon(R.id.lan_4, !text.isEmpty());
                showIcon(R.id.lan_4_l, !text.isEmpty());
                setIconText(R.id.lan_4, text);
                break;
        }
    }

    public void hideNetwork(int id) {
        setNetworkText(id, "");
    }

    public void showIcon(int id, boolean show) {
        try {
            View icon = mViewPager.findViewById(id);
            icon.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        } catch (Exception ex) {}
    }

    public void setIconText(int id, String text) {
        try {
            TextView icon = (TextView) mViewPager.findViewById(id);
            icon.setText(text);
        } catch (Exception ex) {}
    }

    public void addIconLabel(int id, String text) {
        RelativeLayout layout = (RelativeLayout)mViewPager.findViewById(R.id.overview_layout);
        TextView label = new TextView(this);
        mIconLabels.add(label);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_TOP, id);
        params.addRule(RelativeLayout.RIGHT_OF, id);
        label.setLayoutParams(params);
        label.setTextSize(14);
        label.setTextColor(Color.parseColor("White"));
        label.setText(text);
        layout.addView(label,0); //add before detail_layout
    }

    public void hideAllIcons() {
        int[] icons = {R.id.router, R.id.lan_0, R.id.lan_1, R.id.lan_2, R.id.lan_3, R.id.lan_4, R.id.router_l, R.id.lan_0_l, R.id.lan_1_l, R.id.lan_2_l, R.id.lan_3_l, R.id.lan_4_l};
        for (int id : icons) {
            showIcon(id, false);
        }
        RelativeLayout layout = (RelativeLayout)mViewPager.findViewById(R.id.overview_layout);
        for (TextView label:mIconLabels) {
            layout.removeView(label);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return OverviewFragment.newInstance();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
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
                    return getString(R.string.title_overview);
                case 1:
                    return getString(R.string.title_wifi_access);
            }
            return null;
        }

        public Fragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }
    }





}

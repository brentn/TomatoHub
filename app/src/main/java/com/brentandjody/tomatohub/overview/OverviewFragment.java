package com.brentandjody.tomatohub.overview;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.brentandjody.tomatohub.R;
import com.brentandjody.tomatohub.database.Device;
import com.brentandjody.tomatohub.database.Devices;

import java.util.ArrayList;
import java.util.List;

public class OverviewFragment extends Fragment {

    private static final String TAG = OverviewFragment.class.getName();

    private static final int TAG_NETWORK_ID=2;

    private Devices mDevices;
    private boolean mDetailViewVisible;
    private String mRouterId;
    private View mView;
    private LinearLayout mDetailView;
    private TextView mWifiMessage;
    private TextView mStatusMessage;
    private TextView[] mDevicesMessage;
    private TextView[] mNetworkIcons;
    private View[] mNetworkLines;
    private List<TextView> mNetworkLabels;

    public OverviewFragment() {
        // Required empty public constructor
    }

    public static OverviewFragment newInstance(Devices devices) {
        OverviewFragment fragment = new OverviewFragment();
        fragment.setDevices(devices);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDetailViewVisible=false;
        mNetworkLabels = new ArrayList<>();

        mView= inflater.inflate(R.layout.fragment_overview, container, false);
        FloatingActionButton fab = (FloatingActionButton) mView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        mDetailView = (LinearLayout) mView.findViewById(R.id.detail_layout);
        mWifiMessage = (TextView)mView.findViewById(R.id.wifi_message);
        mStatusMessage = (TextView)mView.findViewById(R.id.status_message);
        mDevicesMessage = new TextView[] {(TextView)mView.findViewById(R.id.devices),
                (TextView)mView.findViewById(R.id.are_connected)};
        mNetworkIcons = new TextView[] {(TextView)mView.findViewById(R.id.lan_0),
                (TextView)mView.findViewById(R.id.lan_1),
                (TextView)mView.findViewById(R.id.lan_2),
                (TextView)mView.findViewById(R.id.lan_3),
                (TextView)mView.findViewById(R.id.lan_4)};
        mNetworkLines = new View[] {mView.findViewById(R.id.lan_0_l),
                mView.findViewById(R.id.lan_1_l),
                mView.findViewById(R.id.lan_2_l),
                mView.findViewById(R.id.lan_3_l),
                mView.findViewById(R.id.lan_4_l)};
        initialize();
        return mView;
    }

    public void initialize() {
        setWifiMessage("");
        setStatusMessage("");
        setDevicesMessage("","");
        showRouter(false);
        hideAllNetworkIcons();
        hideDetailView();
    }
    public void setRouterId(String router_id) {mRouterId = router_id;}
    public void setDevices(Devices devices) {mDevices = devices;}
    public void setWifiMessage(String message) { mWifiMessage.setText(message);}
    public void setStatusMessage(String message) {mStatusMessage.setText(message);}
    public void setDevicesMessage(String devices, String message) {
        mDevicesMessage[0].setText(devices);
        mDevicesMessage[1].setText(message);
    }
    public void showRouter(boolean visible) {
        mView.findViewById(R.id.router).setVisibility(visible?View.VISIBLE:View.INVISIBLE);
        mView.findViewById(R.id.router_l).setVisibility(visible?View.VISIBLE:View.INVISIBLE);
    }
    public void hideAllNetworkIcons() {
        for (View label:mNetworkLabels) {
            ((ViewGroup)mView).removeView(label);
        }
        for (View icon : mNetworkIcons) {
            icon.setVisibility(View.INVISIBLE);
        }
        for (View line : mNetworkLines) {
            line.setVisibility(View.INVISIBLE);
        }
    }
    public void showNetwork(int index, String network_id, int total) {
        try {
            mNetworkLines[index].setVisibility(View.VISIBLE);
            mNetworkIcons[index].setVisibility(View.VISIBLE);
            mNetworkIcons[index].setText(String.valueOf(total));
            mNetworkIcons[index].setTag(TAG_NETWORK_ID, network_id);
            addNetworkLabel(mNetworkIcons[index], network_id);
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    private void addNetworkLabel(View icon, String label) {
        TextView tvLabel = new TextView(getActivity());
        mNetworkLabels.add(tvLabel);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_TOP, icon.getId());
        params.addRule(RelativeLayout.RIGHT_OF, icon.getId());
        tvLabel.setLayoutParams(params);
        tvLabel.setTextSize(14);
        tvLabel.setTextColor(Color.parseColor("White"));
        tvLabel.setText(label);
        ((ViewGroup)mView).addView(tvLabel, 1); //add before detail_layout
    }

    public void setupNetworkClickListeners() {
        for (View icon : mNetworkIcons) {
            final View mIcon = icon;
            icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String network_id = (String)view.getTag(TAG_NETWORK_ID);
                    List<Device> devices = mDevices.getDevicesOnNetwork(mRouterId, network_id);
                    DeviceListAdapter adapter = new DeviceListAdapter(getActivity(), devices);
                    ListView detailList = (ListView)mDetailView.findViewById(R.id.network_device_list);
                    detailList.setAdapter(adapter);
                    showDetailView(network_id);
                }
            });
        }
    }

    public boolean isDetailViewVisible() {return mDetailViewVisible;}
    private void showDetailView(String network_name) {
        if (! isDetailViewVisible()) {
            TextView detailTitle = (TextView)mDetailView.findViewById(R.id.network_name);
            detailTitle.setText(getString(R.string.devices_on)+network_name);
            Animation bottomUp = AnimationUtils.loadAnimation(getActivity(), R.anim.bottom_up);
            mDetailView.startAnimation(bottomUp);
            mDetailView.setVisibility(View.VISIBLE);
            mDetailViewVisible = true;
        }
    }
    public void hideDetailView() {
        Animation bottomDown = AnimationUtils.loadAnimation(getActivity(), R.anim.bottom_down);
        mDetailView.startAnimation(bottomDown);
        mDetailView.setVisibility(View.INVISIBLE);
        mDetailViewVisible=false;
    }

}

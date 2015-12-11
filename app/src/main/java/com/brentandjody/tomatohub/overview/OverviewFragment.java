package com.brentandjody.tomatohub.overview;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.brentandjody.tomatohub.R;
import com.brentandjody.tomatohub.database.Device;
import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Network;
import com.brentandjody.tomatohub.database.Networks;

import java.util.ArrayList;
import java.util.List;

public class OverviewFragment extends Fragment {

    public static final int SIGNAL_LOADED = 1;
    public static final int SIGNAL_REFRESH = 2;
    
    private static final String TAG = OverviewFragment.class.getName();
    private OnSignalListener mListener;

    private Networks mNetworks;
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
    private List<Device>[] mDevicesList;

    public OverviewFragment() {
        // Required empty public constructor
    }

    public static OverviewFragment newInstance(Networks networks, Devices devices) {
        OverviewFragment fragment = new OverviewFragment();
        fragment.setDatabases(networks, devices);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnSignalListener)activity;
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnSignalListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDetailViewVisible=false;
        mNetworkLabels = new ArrayList<>();
        mDevicesList = new List[5];

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
        mListener.onSignal(SIGNAL_LOADED);
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
    public void setDatabases(Networks networks, Devices devices) {mDevices = devices; mNetworks = networks;}
    public void setWifiMessage(String message) { mWifiMessage.setText(message);}
    public void setStatusMessage(String message) {mStatusMessage.setText(message);}
    public void setDevicesMessage(String devices, String message) {
        mDevicesMessage[0].setText(devices);
        mDevicesMessage[1].setText(message);
    }
    public void showRouter(boolean visible) {
        mView.findViewById(R.id.router).setVisibility(visible?View.VISIBLE:View.INVISIBLE);
        mView.findViewById(R.id.router_l).setVisibility(visible?View.VISIBLE:View.INVISIBLE);
        if (visible) addNetworkLabel(mView.findViewById(R.id.router), getString(R.string.router));
    }

    public void hideAllNetworkIcons() {
        for (View label:mNetworkLabels) {
            ((ViewGroup)mView).removeView(label);
        }
        for (View icon : mNetworkIcons) {
            icon.setVisibility(View.INVISIBLE);
            icon.setOnClickListener(null);
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
            mNetworkIcons[index].setTag(network_id);
            addNetworkLabel(mNetworkIcons[index], network_id);
        } catch (Exception ex) {
            Log.e(TAG, "showNetwork: "+ex.getMessage());
        }
    }
    @TargetApi(16)
    public void setNetworkTrafficColor(int index, float percent) {
        if (Build.VERSION.SDK_INT >= 16) {
            Drawable circle = ContextCompat.getDrawable(getActivity(), R.drawable.circle);
            if (circle!= null) {
                circle.setColorFilter(new PorterDuffColorFilter(Color.HSVToColor(new float[] {0, percent, 1F}), PorterDuff.Mode.MULTIPLY));
                mNetworkIcons[index].setBackground(circle);
            }
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

    public void setupRefreshListener() {
        mView.findViewById(R.id.internet).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mView.findViewById(R.id.internet).setOnClickListener(null);
                initialize();
                setStatusMessage(getString(R.string.rescannng_network));
                mListener.onSignal(SIGNAL_REFRESH);
            }
        });
    }

    public void setupRouterClickListener(final String router_type, final String external_ip, final long bootTime, final int memory, final int[] cpu) {
        mView.findViewById(R.id.router).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 View routerView =  getLayoutInflater(null).inflate(R.layout.router_details, null);
                ((TextView)routerView.findViewById(R.id.router_type)).setText(router_type);
                ((TextView)routerView.findViewById(R.id.external_ip)).setText(external_ip);
                ((TextView)routerView.findViewById(R.id.uptime)).setText(uptimeSince(bootTime));
                ((ProgressBar)routerView.findViewById(R.id.memory_usage)).setProgress(memory);
                ((ProgressBar)routerView.findViewById(R.id.cpu_usage)).setProgress(cpu[0]);
                ((ProgressBar)routerView.findViewById(R.id.cpu_usage)).setSecondaryProgress(cpu[1]);
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle(getString(R.string.router_details));
                alert.setView(routerView);
                alert.show();
            }
        });
    }

    public void setupNetworkClickListener(final int index) {
        final View icon = mNetworkIcons[index];
        final String network_id = (String)icon.getTag();
        mDevicesList[index] = mDevices.getDevicesOnNetwork(mRouterId, network_id);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Network network = mNetworks.get(mRouterId, network_id);
                mDetailView.findViewById(R.id.network_name).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        final EditText editText = new EditText(getActivity());
                        editText.setHint(network.name());
                        editText.setText(network.customName());
                        editText.setSingleLine();
                        alert.setTitle(getString(R.string.modify_network_name));
                        alert.setView(editText);
                        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                            updateNetworkName(network, editText.getText().toString());
                            }
                        });
                        alert.show();
                    }
                });
                DeviceListAdapter adapter = new DeviceListAdapter(getActivity(), mDevicesList[index]);
                ListView detailList = (ListView)mDetailView.findViewById(R.id.network_device_list);
                detailList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        final EditText editText = new EditText(getActivity());
                        final Device device = mDevicesList[index].get(position);
                        editText.setHint(device.originalName());
                        editText.setText(device.customName());
                        editText.setSingleLine();
                        alert.setTitle(getString(R.string.modify_device_name));
                        alert.setView(editText);
                        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                            updateDeviceName(device, editText.getText().toString(), index);
                            }
                        });
                        alert.show();
                    }
                });
                detailList.setAdapter(adapter);
                showDetailView(network.name());
            }
        });
    }


    private void updateDeviceName(Device device, String custom_name, int list_index) {
        if (device.customName()==null || ! device.customName().equals(custom_name)) {
            device.setCustomName(custom_name);
            mDevices.updateName(device.mac(), custom_name);
            mDevicesList[list_index] = mDevices.getDevicesOnNetwork(mRouterId, device.lastNetwork());
            ((DeviceListAdapter) ((ListView) mDetailView.findViewById(R.id.network_device_list)).getAdapter()).notifyDataSetChanged();
        }
    }

    private void updateNetworkName(Network network, String custom_name) {
        if (network.customName()==null || ! network.customName().equals(custom_name)) {
            network.setCustomName(custom_name);
            mNetworks.insertOrUpdate(network);
            ((TextView)mDetailView.findViewById(R.id.network_name)).setText(custom_name);
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
        if (mDetailViewVisible) {
            Animation bottomDown = AnimationUtils.loadAnimation(getActivity(), R.anim.bottom_down);
            mDetailView.startAnimation(bottomDown);
            mDetailView.setVisibility(View.INVISIBLE);
            mDetailViewVisible = false;
        }
    }

    public interface OnSignalListener {
        void onSignal(int signal);
    }

    private String uptimeSince(long bootTime) {
        if (bootTime<0) return "??";
        long time = (System.currentTimeMillis()/1000)-bootTime;
        String days = time>86400?String.valueOf(time/86400)+" days ":"";
        String hours = String.valueOf((time%86400)/3600)+" hours ";
        String mins = String.valueOf(((time%86400)%3600)/60)+" mins ";
        String secs = String.valueOf(((time%86400)%3600)%60)+" secs";
        return days+hours+mins+secs;
    }

    public class DeviceListAdapter extends ArrayAdapter<Device> {
        Context mContext;
        float mTotalTraffic;
        public DeviceListAdapter(Context context, List<Device> devices) {
            super(context, 0, devices);
            mContext = context;
            mTotalTraffic=0;
            for (Device d : devices) if (d.isActive()) mTotalTraffic+=d.lastSpeed();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Device device = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_device_list, parent, false);
            }
            TextView tvName = (TextView)convertView.findViewById(R.id.device_name);
            TextView tvIP = (TextView)convertView.findViewById(R.id.device_ip);
            TextView tvTraffic = (TextView)convertView.findViewById(R.id.device_traffic);
            ProgressBar pbTrafficBar = (ProgressBar)convertView.findViewById(R.id.traffic_bar);
            tvName.setText(device.name());
            tvIP.setText(device.lastIP());
            if (device.isActive()) tvName.setTextColor(Color.BLACK);
            else tvName.setTextColor(Color.GRAY);
            if (device.isActive()) {
                tvTraffic.setText(String.format("%.2f", device.lastSpeed() / 1000) + " kb/s");
                if (mTotalTraffic>0)
                    pbTrafficBar.setProgress(Math.round(device.lastSpeed()/mTotalTraffic*100));
                else
                    pbTrafficBar.setProgress(0);
            } else {
                pbTrafficBar.setProgress(0);
            }
            return convertView;
        }

    }

}

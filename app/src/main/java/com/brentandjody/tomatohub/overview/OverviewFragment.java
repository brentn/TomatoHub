package com.brentandjody.tomatohub.overview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.brentandjody.tomatohub.MainActivity;
import com.brentandjody.tomatohub.R;
import com.brentandjody.tomatohub.SpeedTestActivity;
import com.brentandjody.tomatohub.database.Device;
import com.brentandjody.tomatohub.database.Devices;
import com.brentandjody.tomatohub.database.Network;
import com.brentandjody.tomatohub.database.Networks;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class OverviewFragment extends Fragment {

    public static final int SIGNAL_LOADED = 1;
    public static final int SIGNAL_REFRESH = 2;
    public static final int SIGNAL_REBOOT = 3;
    public static final int SIGNAL_BLOCK = 4;
    public static final int SIGNAL_UNBLOCK = 5;
    public static final int SIGNAL_PRIORITIZE = 6;
    
    private static final String TAG = OverviewFragment.class.getName();
    private OnSignalListener mListener;
    private MainActivity mActivity;

    private Networks mNetworks;
    private Devices mDevices;
    protected boolean mQOSEnabled;
    protected boolean mDetailViewVisible;
    private String mRouterId;
    private View mView;
    protected FloatingActionButton mSpeedTestFab;
    protected LinearLayout mDetailView;
    protected TextView mWifiMessage;
    protected TextView mStatusMessage;
    protected TextView[] mDevicesMessage;
    protected TextView[] mNetworkIcons;
    protected View[] mNetworkLines;
    protected ArrayList<View> mNetworkLabels;
    private List<Device>[] mDevicesList;
    private String myMacAddress;
    private String mLastNetworkId;

    public OverviewFragment() {
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnSignalListener)activity;
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnSignalListener");
        }
        try {
            this.mActivity = (MainActivity) activity;
        } catch (final ClassCastException ex) {
            throw new ClassCastException(activity.toString() + " must extend MainActivity");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDevices = mActivity.getDevices();
        mNetworks = mActivity.getNetworks();
        mDetailViewVisible=false;
        mNetworkLabels = new ArrayList<>();
        mDevicesList = new List[5];

        mView= inflater.inflate(R.layout.fragment_overview, container, false);
        mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideDetailView();
            }
        });
        mSpeedTestFab = (FloatingActionButton) mView.findViewById(R.id.fab);
        mSpeedTestFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SpeedTestActivity.class);
                mActivity.startActivityForResult(intent, 0);
            }
        });
        mSpeedTestFab.setVisibility(View.INVISIBLE);
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
        mListener.onSignal(SIGNAL_LOADED, null);
        showDemoMode(false);
        return mView;
    }

    public void initialize() {
        mQOSEnabled=false;
        setWifiMessage("");
        setStatusMessage("");
        setDevicesMessage("","");
        showRouter(false);
        showSpeedTestButton(false);
        hideAllNetworkIcons();
        hideDetailView();
    }
    public void setRouterId(String router_id) {mRouterId = router_id;}
    public void setQOSEnabled(boolean enabled) {mQOSEnabled=enabled;}
    public void setMyMac(String mac) {if (mac!=null) myMacAddress = mac.toUpperCase();}
    public void setWifiMessage(String message) { mWifiMessage.setText(message);}
    public void setStatusMessage(String message) {mStatusMessage.setText(message);}
    public void setDevicesMessage(String devices, String message) {
        mDevicesMessage[0].setText(devices);
        mDevicesMessage[1].setText(message);
    }
    public void showDemoMode(boolean visible) {
        View warning = mView.findViewById(R.id.demo_mode);
        if (warning!=null)
            warning.setVisibility(visible?View.VISIBLE:View.INVISIBLE);
    }
    public void showRouter(boolean visible) {
        mView.findViewById(R.id.router).setVisibility(visible?View.VISIBLE:View.INVISIBLE);
        mView.findViewById(R.id.router_label).setVisibility(visible?View.VISIBLE:View.INVISIBLE);
        mView.findViewById(R.id.router_l).setVisibility(visible?View.VISIBLE:View.INVISIBLE);
        if (! visible) {
            hideAllNetworkIcons();
        }
    }

    public void showSpeedTestButton(boolean visible) {
        mSpeedTestFab.setVisibility(visible?View.VISIBLE:View.INVISIBLE);
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
            addNetworkLabel(mNetworkIcons[index], mNetworks.get(mRouterId, network_id).name());
        } catch (Exception ex) {
            Log.e(TAG, "showNetwork: "+ex.getMessage());
        }
    }
    @TargetApi(16)
    public void setNetworkTrafficColor(int index, float percent) {
        if (Build.VERSION.SDK_INT >= 16) {
            if (mActivity!=null) {
                Drawable circle = ContextCompat.getDrawable(mActivity, R.drawable.circle);
                if (circle != null) {
                    circle.setColorFilter(new PorterDuffColorFilter(Color.HSVToColor(new float[]{0, percent, 1F}), PorterDuff.Mode.MULTIPLY));
                    mNetworkIcons[index].setBackground(circle);
                }
            }
        }
    }
//TODO:Test
    private void addNetworkLabel(View icon, String label) {
        try {
            TextView tvLabel = new TextView(mActivity);
            mNetworkLabels.add(tvLabel);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_TOP, icon.getId());
            params.addRule(RelativeLayout.RIGHT_OF, icon.getId());
            tvLabel.setLayoutParams(params);
            tvLabel.setTextSize(14);
            tvLabel.setTextColor(Color.parseColor("White"));
            tvLabel.setText(label);
            ((ViewGroup) mView).addView(tvLabel, 1); //add before detail_layout
        } catch (Exception ex) {
            Log.e(TAG, "addNetworkLabel: "+ex.getMessage());
        }
    }

    public void setupInternetClickListener(final String external_ip) {
        try {
            mView.findViewById(R.id.internet).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideDetailView();
                    if (external_ip.isEmpty()) {
                        mListener.onSignal(SIGNAL_REFRESH, null);
                    } else {
                        new AlertDialog.Builder(mActivity)
                                .setTitle("Internet")
                                .setMessage("External IP Address: " + external_ip)
                                .setPositiveButton(getString(R.string.refresh), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mListener.onSignal(SIGNAL_REFRESH, null);
                                    }
                                })
                                .show();
                    }
                }
            });
        } catch (Exception ex) {
            Log.e(TAG, "setupInternetClickListener: "+ex.getMessage());
        }
    }

    public void setupRouterClickListener(final String router_type, final String internal_ip, final long bootTime, final int memory, final int[] cpu) {
        try {
            mView.findViewById(R.id.router).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    hideDetailView();
                    SharedPreferences prefs = mActivity.getSharedPreferences(mActivity.getString(R.string.sharedPreferences_name), Context.MODE_PRIVATE);
                    View routerView = getLayoutInflater(null).inflate(R.layout.dialog_router_details, null);
                    ((TextView) routerView.findViewById(R.id.router_type)).setText(router_type);
                    ((TextView) routerView.findViewById(R.id.internal_ip)).setText(internal_ip);
                    ((TextView) routerView.findViewById(R.id.uptime)).setText(uptimeSince(bootTime));
                    ((ProgressBar) routerView.findViewById(R.id.memory_usage)).setProgress(memory);
                    if (cpu != null && cpu.length>0)
                        ((ProgressBar) routerView.findViewById(R.id.cpu_usage)).setProgress(cpu[0]);
                    if (cpu != null && cpu.length>1)
                        ((ProgressBar) routerView.findViewById(R.id.cpu_usage)).setSecondaryProgress(cpu[1]);
                    AlertDialog.Builder alert = new AlertDialog.Builder(mActivity)
                        .setTitle(getString(R.string.router_details))
                        .setView(routerView);
                    if (prefs.getBoolean(getString(R.string.pref_key_allow_changes), false)) {
                        alert.setNeutralButton(getString(R.string.reboot), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mListener.onSignal(SIGNAL_REBOOT, null);
                            }
                        });
                    }
                    alert.show();
                }
            });
        } catch (Exception ex) {
            Log.e(TAG, "setupRouterClickListener: "+ex.getMessage());
        }
    }

    public void setupNetworkClickListener(final int index) {
        try {
            final View icon = mNetworkIcons[index];
            final String network_id = (String)icon.getTag();
            mDevicesList[index] = mDevices.getDevicesOnNetwork(mRouterId, network_id);
            icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (! network_id.equals(mLastNetworkId)) hideDetailView();
                    mLastNetworkId = network_id;
                    final Network network = mNetworks.get(mRouterId, network_id);
                    mDetailView.findViewById(R.id.network_name).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                            final EditText editText = new EditText(mActivity);
                            editText.setHint(network.networkId());
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
                    // setupDeviceClickListeners
                    setupDeviceClickListeners(index);
                    showDetailView(network.name());
                }
            });
        } catch (Exception ex) {
            Log.e(TAG, "setupNetworkClickListener: "+ex.getMessage());
        }
    }

    private void setupDeviceClickListeners(final int device_list_index) {
        try {
            final DeviceListAdapter adapter = new DeviceListAdapter(mActivity, mDevicesList[device_list_index]);
            ListView detailList = (ListView) mDetailView.findViewById(R.id.network_device_list);
            detailList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final Device device = adapter.getItem(position);
                    View deviceView = getLayoutInflater(null).inflate(R.layout.dialog_device_details, null);
                    final EditText deviceName = (EditText) deviceView.findViewById(R.id.device_name);
                    deviceName.setHint(device.originalName());
                    deviceName.setText(device.customName());
                    AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                    alert.setTitle(mActivity.getString(R.string.manage_device));
                    alert.setView(deviceView);
                    SharedPreferences prefs = mActivity.getSharedPreferences(mActivity.getString(R.string.sharedPreferences_name), Context.MODE_PRIVATE);
                    if (prefs.getBoolean(mActivity.getString(R.string.pref_key_allow_changes), false)) {
                        if (mQOSEnabled) {
                            alert.setPositiveButton(mActivity.getString(R.string.prioritize), new DialogInterface.OnClickListener() {
                                AlertDialog popup;
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    View view = LayoutInflater.from(mActivity).inflate(R.layout.dialog_time_selector, null, false);
                                    RadioGroup timePicker = (RadioGroup)view.findViewById(R.id.time_choice);
                                    for (int i=0; i<timePicker.getChildCount(); i++) {
                                        final int index = i;
                                        timePicker.getChildAt(i).setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                // give feedback
                                                popup.dismiss();
                                                String time = mActivity.getResources().getStringArray(R.array.prioritize_times)[index];
                                                Toast.makeText(mActivity, "Prioritizing "+device.lastIP()+" for "+time, Toast.LENGTH_LONG).show();
                                                //
                                                String milliseconds = mActivity.getResources().getStringArray(R.array.prioritize_times_values)[index];
                                                long ms = 0; try { ms = Long.parseLong(milliseconds); }
                                                catch(Exception ex) {Log.e(TAG, "Could not parse milliseconds");}
                                                device.setPrioritizedUntil(System.currentTimeMillis()+ms);
                                                mDevices.insertOrUpdate(device);
                                                ((DeviceListAdapter) ((ListView) mDetailView.findViewById(R.id.network_device_list)).getAdapter()).notifyDataSetChanged();
                                                mListener.onSignal(SIGNAL_PRIORITIZE, device.lastIP()+":"+milliseconds);
                                            }
                                        });
                                    }
                                    popup = new  AlertDialog.Builder(mActivity)
                                            .setTitle(mActivity.getString(R.string.prioritize_device))
                                            .setMessage(mActivity.getString(R.string.prioritize_device_time))
                                            .setView(view)
                                            .show();
                                }
                            });
                        }
                        if (device.isBlocked()) {
                            alert.setNegativeButton(mActivity.getString(R.string.unblock), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    device.setBlocked(false);
                                    mDevices.insertOrUpdate(device);
                                    ((DeviceListAdapter) ((ListView) mDetailView.findViewById(R.id.network_device_list)).getAdapter()).notifyDataSetChanged();
                                    mListener.onSignal(SIGNAL_UNBLOCK, device.mac());
                                    Toast.makeText(mActivity, mActivity.getString(R.string.unblocking) + " " + device.mac(), Toast.LENGTH_LONG).show();
                                }
                            });

                        } else if (!device.mac().toUpperCase().equals(myMacAddress)) { //prevent blocking own device
                            alert.setNegativeButton(mActivity.getString(R.string.block), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Context context = mActivity;
                                    new AlertDialog.Builder(context)
                                            .setMessage(context.getString(R.string.block_confirm))
                                            .setCancelable(false)
                                            .setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    device.setBlocked(true);
                                                    mDevices.insertOrUpdate(device);
                                                    ((DeviceListAdapter) ((ListView) mDetailView.findViewById(R.id.network_device_list)).getAdapter()).notifyDataSetChanged();
                                                    mListener.onSignal(SIGNAL_BLOCK, device.mac());
                                                    Toast.makeText(mActivity, mActivity.getString(R.string.blocking) + " " + device.mac(), Toast.LENGTH_LONG).show();
                                                }
                                            })
                                            .setNegativeButton(context.getString(R.string.no), null)
                                            .show();
                                }
                            });
                        }
                    }
                    alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            updateDeviceName(device, deviceName.getText().toString(), device_list_index);
                        }
                    });
                    alert.show();
                }
            });
            detailList.setAdapter(adapter);
        } catch (Exception ex) {
            Log.e(TAG, "setupDeviceClickListener: "+ex.getMessage());
        }
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
            ((TextView)mDetailView.findViewById(R.id.network_name)).setText(getString(R.string.devices_on)+" "+custom_name);
        }
    }

    public boolean isDetailViewVisible() {return mDetailViewVisible;}
    protected void showDetailView(String network_name) {
        if (! isDetailViewVisible()) {
            TextView detailTitle = (TextView)mDetailView.findViewById(R.id.network_name);
            detailTitle.setText(getString(R.string.devices_on)+network_name);
            Animation bottomUp = AnimationUtils.loadAnimation(mActivity, R.anim.bottom_up);
            mDetailView.startAnimation(bottomUp);
            mDetailView.setVisibility(View.VISIBLE);
            mDetailViewVisible = true;
        }
    }
    public void hideDetailView() {
        if (mDetailViewVisible) {
            Animation bottomDown = AnimationUtils.loadAnimation(mActivity, R.anim.bottom_down);
            mDetailView.startAnimation(bottomDown);
            mDetailView.setVisibility(View.INVISIBLE);
            mDetailViewVisible = false;
        }
    }

    public interface OnSignalListener {
        void onSignal(int signal, String parameter);
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


// **DeviceListAdapter**
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
            View imgBlocked = convertView.findViewById(R.id.blocked);
            ProgressBar pbTrafficBar = (ProgressBar)convertView.findViewById(R.id.traffic_bar);
            tvName.setText(device.name());
            tvIP.setText(device.lastIP());
            imgBlocked.setVisibility(device.isBlocked()?View.VISIBLE:View.GONE);
            if (device.isActive()) {
                pbTrafficBar.setVisibility(View.VISIBLE);
                tvName.setTextColor(Color.BLACK);
                tvTraffic.setText(String.format("%.2f", device.lastSpeed() / 1000) + " kb/s");
                if (mTotalTraffic>0)
                    pbTrafficBar.setProgress(Math.round(device.lastSpeed()/mTotalTraffic*100));
                else
                    pbTrafficBar.setProgress(0);
                if (device.prioritizedUntil()==Device.NOT_PRIORITIZED) {
                    convertView.findViewById(R.id.priority).setVisibility(View.INVISIBLE);
                } else {
                    if (device.prioritizedUntil()==Device.INDETERMINATE_PRIORITY) {
                        ((TextView)convertView.findViewById(R.id.priority_until)).setText(R.string.indeterminite_priority_access);
                        convertView.findViewById(R.id.until).setVisibility(View.INVISIBLE);
                    } else {
                        convertView.findViewById(R.id.until).setVisibility(View.VISIBLE);
                        long now = System.currentTimeMillis() / 1000;
                        if (device.prioritizedUntil() < now) {
                            convertView.findViewById(R.id.priority).setVisibility(View.INVISIBLE);
                            ((TextView)convertView.findViewById(R.id.priority_until)).setText(R.string.indeterminite_priority_access);
                        } else {
                            convertView.findViewById(R.id.priority).setVisibility(View.VISIBLE);
                            Calendar undoTime = Calendar.getInstance();
                            undoTime.setTimeInMillis(device.prioritizedUntil());
                            String time = undoTime.get(Calendar.HOUR) + ":" + String.format("%02d", undoTime.get(Calendar.MINUTE));
                            ((TextView)convertView.findViewById(R.id.priority_until)).setText(R.string.priority_access_until);
                            ((TextView) convertView.findViewById(R.id.until)).setText(time);
                        }
                    }
                }
            } else {
                tvName.setTextColor(Color.GRAY);
                pbTrafficBar.setVisibility(View.GONE);
                tvTraffic.setText("");
                convertView.findViewById(R.id.priority).setVisibility(View.INVISIBLE);
            }
            return convertView;
        }

    }

}

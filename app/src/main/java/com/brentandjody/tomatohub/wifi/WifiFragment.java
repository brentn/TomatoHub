package com.brentandjody.tomatohub.wifi;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.brentandjody.tomatohub.R;
import com.brentandjody.tomatohub.database.Wifi;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 */
public class WifiFragment extends Fragment {

    private OnSignalListener mListener;
    private View mView;
    private List<Wifi> mWifiList;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WifiFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static WifiFragment newInstance() {
        return new WifiFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWifiList = new ArrayList<>();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnSignalListener) {
            mListener = (OnSignalListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnSignalListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_wifi_list, container, false);
        return mView;
    }

    public void setWifiList(List<Wifi> wifi_list) {
        mWifiList = wifi_list;
        ListView list = (ListView)mView.findViewById(R.id.wifi_list);
        list.setAdapter(new WifiListAdapter(getActivity(), mWifiList, mListener));
    }

    public interface OnSignalListener {
        void onSignal(int signal);
    }

    public class WifiListAdapter extends ArrayAdapter<Wifi> {

        private Context mContext;
        private final List<Wifi> mWifiList;
        private final OnSignalListener mListener;
        private SharedPreferences mPrefs;

        public WifiListAdapter(Context context, List<Wifi> items, OnSignalListener listener) {
            super(context, 0, items);
            mContext = context;
            mWifiList = items;
            mListener = listener;
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Wifi wifi = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_wifi_list, parent, false);
            }
            ImageView background = (ImageView) convertView.findViewById(R.id.wifi_background);
            float[] hsv = new float[] {0, 0.6F, 0.6F};
            hsv[0] = (float)(wifi.SSID().hashCode()%360);
            int backColor = Color.HSVToColor(hsv);
            background.setColorFilter(new PorterDuffColorFilter(backColor, PorterDuff.Mode.OVERLAY));
            ((TextView)convertView.findViewById(R.id.ssid)).setText(wifi.SSID());
            ((Switch) convertView.findViewById(R.id.enabled_switch)).setChecked(true);
            if (mPrefs.getBoolean(getString(R.string.pref_key_readonly),false)) {
                convertView.findViewById(R.id.enabled_switch).setEnabled(false);
            }
//            ((Button) convertView.findViewById(R.id.share_button));
            return convertView;
        }


        @Override
        public int getCount() {
            return mWifiList.size();
        }

    }

}

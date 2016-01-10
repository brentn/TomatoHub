package com.brentandjody.tomatohub.wifi;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
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
        if (wifi_list!= null) {
            mWifiList = wifi_list;
            ListView list = (ListView) mView.findViewById(R.id.wifi_list);
            list.setAdapter(new WifiListAdapter(getActivity(), mWifiList, mListener));
        }
    }

    public interface OnSignalListener {
        void onSignal(int signal, String parameter);
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
            mPrefs = context.getSharedPreferences(context.getString(R.string.sharedPreferences_name), Context.MODE_PRIVATE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Wifi wifi = getItem(position);
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
            boolean allow_changes = mPrefs.getBoolean(getString(R.string.pref_key_allow_changes),false);
            convertView.findViewById(R.id.enabled_switch).setVisibility(View.GONE); //hide this permanently, until implemented
            convertView.findViewById(R.id.share_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                    alert.setTitle(mContext.getString(R.string.wifi_password));
                    TextView message = new TextView(mContext);
                    message.setText(wifi.password());
                    message.setTextColor(getResources().getColor(R.color.colorAccent));
                    message.setTextSize(24);
                    message.setTypeface(null, Typeface.BOLD);
                    message.setPadding(0,20,0,0);
                    message.setGravity(Gravity.CENTER);
                    alert.setView(message);
                    alert.setPositiveButton(mContext.getString(R.string.share), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                            sharingIntent.setType("text/plain");
                            String body = mContext.getString(R.string.share_body).replace("[SSID]", wifi.SSID()).replace("[PASSWORD]", wifi.password());
                            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mContext.getString(R.string.share_subject));
                            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
                            startActivity(Intent.createChooser(sharingIntent, mContext.getString(R.string.how_do_you_want_to_share)));
                        }
                    });
                    alert.show();
                }
            });
            return convertView;
        }


        @Override
        public int getCount() {
            return mWifiList.size();
        }

    }

}

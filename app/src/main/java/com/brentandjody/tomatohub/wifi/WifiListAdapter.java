package com.brentandjody.tomatohub.wifi;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.brentandjody.tomatohub.database.Wifi;
import com.brentandjody.tomatohub.wifi.WifiFragment.OnSignalListener;
import com.brentandjody.tomatohub.R;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Wifi} and makes a call to the
 * specified {@link OnSignalListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class WifiListAdapter extends RecyclerView.Adapter<WifiListAdapter.ViewHolder> {

    private static final String TAG = WifiListAdapter.class.getName();

    private final List<Wifi> mWifiList;
    private final OnSignalListener mListener;

    public WifiListAdapter(List<Wifi> items, OnSignalListener listener) {
        mWifiList = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wifi_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Wifi item = mWifiList.get(position);
        holder.mItem = item;
        float[] hsv = new float[] {0, 40, 40};
        hsv[0] = (float)(item.SSID().hashCode()%360);
        int backColor = Color.HSVToColor(hsv);
        hsv[0] = (hsv[0]+=180)%360;
        int foreColor = Color.HSVToColor(hsv);
        holder.mBackground.setColorFilter(new PorterDuffColorFilter(backColor, PorterDuff.Mode.OVERLAY));
        holder.mSSID.setText(item.SSID());
        holder.mSSID.setTextColor(foreColor);
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onSignal(-1);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mWifiList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final ImageView mBackground;
        public final TextView mSSID;
        public final Switch mEnabled;
        public final TextView mMessage;
        public final Button mShareButton;
        public Wifi mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mBackground = (ImageView) view.findViewById(R.id.wifi_background);
            mSSID = (TextView) view.findViewById(R.id.ssid);
            mEnabled = (Switch) view.findViewById(R.id.enabled_switch);
            mMessage = (TextView) view.findViewById(R.id.wifi_details);
            mShareButton = (Button) view.findViewById(R.id.share_button);
        }

    }
}

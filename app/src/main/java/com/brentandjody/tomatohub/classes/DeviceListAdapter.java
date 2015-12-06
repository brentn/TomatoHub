package com.brentandjody.tomatohub.classes;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.brentandjody.tomatohub.R;
import com.brentandjody.tomatohub.database.Device;

import java.util.List;

/**
 * Created by brent on 03/12/15.
 */
public class DeviceListAdapter extends ArrayAdapter<Device> {
    Context mContext;
    public DeviceListAdapter(Context context, List<Device> devices) {
        super(context, 0, devices);
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Device device = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_device_list, parent, false);
        }
        View listItem = convertView.findViewById(R.id.device_item);
        TextView tvName = (TextView)convertView.findViewById(R.id.device_name);
        TextView tvIP = (TextView)convertView.findViewById(R.id.device_ip);
        TextView tvTraffic = (TextView)convertView.findViewById(R.id.device_traffic);
        tvName.setText(device.name());
        tvIP.setText(device.lastIP());
        if (device.isActive()) tvName.setTextColor(Color.WHITE);
        else tvName.setTextColor(Color.GRAY);
        if (device.isActive()) {
            tvTraffic.setText(String.format("%.2f", device.lastSpeed() / 1000) + " kb/s");
            int red = Math.min(255, Math.round(device.lastSpeed() / 50));
            listItem.setBackgroundColor(Color.argb(128, red, 0, 0));
        } else {
            listItem.setBackgroundColor(Color.argb(128, 0, 0, 0));
        }
        return convertView;
    }

}

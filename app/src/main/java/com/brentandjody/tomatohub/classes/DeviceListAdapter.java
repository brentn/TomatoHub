package com.brentandjody.tomatohub.classes;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.brentandjody.tomatohub.R;
import com.brentandjody.tomatohub.database.DBContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brent on 03/12/15.
 */
public class DeviceListAdapter extends ArrayAdapter<Device> {
    public DeviceListAdapter(Context context, List<Device> devices) {
        super(context, 0, devices);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Device device = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_device_list, parent, false);
        }
        TextView tvName = (TextView)convertView.findViewById(R.id.device_name);
        TextView tvIP = (TextView)convertView.findViewById(R.id.device_ip);
        if (TextUtils.isEmpty(device.customName()))
            tvName.setText(device.name());
        else
            tvName.setText(device.customName());
        tvIP.setText(device.lastIP());
        if (device.isActive()) tvName.setTextColor(Color.WHITE);
        else tvName.setTextColor(Color.GRAY);

        return convertView;
    }

}

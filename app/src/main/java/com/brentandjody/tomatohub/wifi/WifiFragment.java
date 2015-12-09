package com.brentandjody.tomatohub.wifi;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_wifi_list, container, false);
        return mView;
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

    public void setWifiList(List<Wifi> wifi_list) {
        mWifiList = wifi_list;
        // Set the adapter
        if (mView instanceof RecyclerView) {
            Context context = mView.getContext();
            RecyclerView recyclerView = (RecyclerView) mView;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(new WifiListAdapter(mWifiList, mListener));
        }
    }

    public interface OnSignalListener {
        void onSignal(int signal);
    }
}

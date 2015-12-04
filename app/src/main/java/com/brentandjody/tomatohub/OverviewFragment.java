package com.brentandjody.tomatohub;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.brentandjody.tomatohub.classes.Device;
import com.brentandjody.tomatohub.classes.DeviceListAdapter;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OverviewFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OverviewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OverviewFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private boolean mDetailViewVisible;
    private View mView;

    public OverviewFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     */
    public static OverviewFragment newInstance() {
        return new OverviewFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mDetailViewVisible=false;
        mView= inflater.inflate(R.layout.fragment_overview, container, false);
        FloatingActionButton fab = (FloatingActionButton) mView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        return mView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void setupNetworkClickListeners() {
        int[] icons = new int[]{R.id.lan_0,R.id.lan_1,R.id.lan_2,R.id.lan_3,R.id.lan_4};
        for (int i=0; i< icons.length; i++) {
            final int j = i;
            mView.findViewById(icons[i]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String label = (String)view.getTag();
                    MainActivity activity = (MainActivity)getActivity();
                    List<Device> devices = activity.getDevicesDB()
                            .getDevicesOnNetwork(activity.getNetworkId(j));
                    DeviceListAdapter adapter = new DeviceListAdapter(activity, devices);
                    ListView detailList = (ListView)mView.findViewById(R.id.network_device_list);
                    detailList.setAdapter(adapter);
                    showDetails(label);
                }
            });
        }
    }

    private void showDetails(String network_name) {
        if (! isDetailViewVisible()) {
            ((TextView)mView.findViewById(R.id.network_name))
                    .setText(getString(R.string.devices_on)+network_name);
            Animation bottomUp = AnimationUtils.loadAnimation(getActivity()
                    , R.anim.bottom_up);
            LinearLayout l = (LinearLayout) mView.findViewById(R.id.detail_layout);
            l.startAnimation(bottomUp);
            l.setVisibility(View.VISIBLE);
            mDetailViewVisible = true;
        }
    }

    public boolean isDetailViewVisible() {return mDetailViewVisible;}

    public void hideDetailView() {
        Animation bottomDown = AnimationUtils.loadAnimation(getActivity()
                , R.anim.bottom_down);
        LinearLayout l = (LinearLayout)mView.findViewById(R.id.detail_layout);
        l.startAnimation(bottomDown);
        l.setVisibility(View.INVISIBLE);
        mDetailViewVisible=false;
    }
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


}

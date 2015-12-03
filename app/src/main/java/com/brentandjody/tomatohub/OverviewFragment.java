package com.brentandjody.tomatohub;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


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
        OverviewFragment fragment = new OverviewFragment();

        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mDetailViewVisible=false;
        mView= inflater.inflate(R.layout.fragment_overview, container, false);
        ImageView i = (ImageView)mView.findViewById(R.id.internet);
        i.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (! isDetailViewVisible()) {
                    Animation bottomUp = AnimationUtils.loadAnimation(getActivity()
                            , R.anim.bottom_up);
                    LinearLayout l = (LinearLayout) mView.findViewById(R.id.detail_layout);
                    l.startAnimation(bottomUp);
                    l.setVisibility(View.VISIBLE);
                    mDetailViewVisible = true;
                }
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

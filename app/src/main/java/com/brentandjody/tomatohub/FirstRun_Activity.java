package com.brentandjody.tomatohub;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.brentandjody.tomatohub.routers.RouterType;
import com.viewpagerindicator.CirclePageIndicator;


public class FirstRun_Activity extends AppCompatActivity implements PageTurnListener{

    public static int SCREEN_WIDTH;
    private View mContentView;
    private View mGetStartedButton;
    private View mDemoModeButton;
    private FirstRunPager mPager;
    private CirclePageIndicator mPagerIndicator;
    private FirstRunPagerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        SCREEN_WIDTH=size.x;

        setContentView(R.layout.activity_first_run_);
        mContentView = findViewById(R.id.background);
        mGetStartedButton = findViewById(R.id.btnGetStarted);
        mDemoModeButton = findViewById(R.id.btnDemoMode);

        mGetStartedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mDemoModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPreferences_name), MODE_PRIVATE);
                prefs.edit().putString(getString(R.string.pref_key_router_type), Integer.toString(RouterType.FAKE)).commit();
                finish();
            }
        });

        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        mPager = new FirstRunPager(this);
        mPagerIndicator = new CirclePageIndicator(this);
        LinearLayout pagerContainer = (LinearLayout) findViewById(R.id.pager_container);
        pagerContainer.addView(mPager, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        pagerContainer.addView(mPagerIndicator, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 40, 1));

        mAdapter = new FirstRunPagerAdapter();
        mPager.setAdapter(mAdapter);

        mPagerIndicator.setViewPager(mPager);

        mAdapter.delayedReveal(mGetStartedButton, 5000);
        mAdapter.delayedReveal(mDemoModeButton, 5500);
        mAdapter.delayedReveal(mPagerIndicator,6500);
    }


    public void onPageTurning(int position, int offset) {
        mAdapter.moveIcons(position, offset);
    }

    public class FirstRunPager extends ViewPager {

        private PageTurnListener mListener;

        public FirstRunPager(Context context) {
            super(context);
            mListener = (FirstRun_Activity) context;
        }

        public FirstRunPager(Context context, AttributeSet attrs) {
            super(context, attrs);
            mListener = (FirstRun_Activity) context;
        }

        @Override
        protected void onPageScrolled(int position, float offset, int offsetPixels) {
            mListener.onPageTurning(position, offsetPixels);
            super.onPageScrolled(position, offset, offsetPixels);
        }
    }

}

interface PageTurnListener {
    void onPageTurning(int position, int offset);
}
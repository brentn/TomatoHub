package com.brentandjody.tomatohub;

import android.content.Context;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

/**
 * Created by brentn on 18/01/16.
 * Adapter for FirstRun
 */
public class FirstRunPagerAdapter extends PagerAdapter {

    private final Handler mHandler = new Handler();
    private ViewGroup mActivePages;
    private boolean mHidden;

    public FirstRunPagerAdapter() {
        super();
        mHidden=true;
    }

    public int getCount() {
        return 4;
    }

    public Object instantiateItem(ViewGroup collection, int position) {
        LayoutInflater inflater = (LayoutInflater) collection.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = null;
        switch (position) {
            case 0:
                view = inflater.inflate(R.layout.firstrun_page1, null);
                if (mHidden) {
                    delayedReveal(view.findViewById(R.id.imgLogo), 1000);
                    delayedReveal(view.findViewById(R.id.pg1line1), 2000);
                    delayedReveal(view.findViewById(R.id.imgWifi), 3000, 2000);
                    mHidden=false;
                }
                break;
            case 1:
                view = inflater.inflate(R.layout.firstrun_page2, null);

                break;
            case 2:
                view = inflater.inflate(R.layout.firstrun_page3, null);
                final Context page3 = view.getContext();
                final View spy = view.findViewById(R.id.spy);
                spy.setTranslationX(10000);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        spy.setTranslationX(0);
                        spy.startAnimation(AnimationUtils.loadAnimation(page3, R.anim.slide_in_from_right));
                    }
                }, 3000);
                break;
            case 3:
                view = inflater.inflate(R.layout.firstrun_page4, null);
                final Context page4 = view.getContext();
                final View guest3 = view.findViewById(R.id.guest3);
                final View guest4 = view.findViewById(R.id.guest4);
                guest3.setTranslationX(10000);
                guest4.setTranslationX(10000);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        guest3.setTranslationX(0);
                        guest3.startAnimation(AnimationUtils.loadAnimation(page4, R.anim.slide_in_from_right));
                    }
                }, 2000);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        guest4.setTranslationX(0);
                        guest4.startAnimation(AnimationUtils.loadAnimation(page4, R.anim.slide_in_from_left));
                    }
                }, 1300);
                break;
        }

        collection.addView(view, 0);
        mActivePages = collection;
        return view;
    }

    @Override
    public void destroyItem(ViewGroup arg0, int arg1, Object arg2) {
        arg0.removeView((View) arg2);

    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == arg1;
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    public void moveIcons(int position, int offset) {
        final float TEXT_SPEED=4.5f;
        final float ICON_SPEED=1.3f;
        final float ICON2_SPEED=.5f;
        View leftText = null;
        View rightText = null;
        View leftIcon = null;
        View leftIcon2 = null;
        View rightIcon = null;
        View rightIcon2 = null;
        switch (position) {
            case 0:
                leftText = mActivePages.findViewById(R.id.pg1line1);
                rightText = mActivePages.findViewById(R.id.pg2line1);
                leftIcon = mActivePages.findViewById(R.id.imgWifi);
                rightIcon = mActivePages.findViewById(R.id.car);
                rightIcon2 = null;
                break;
            case 1:
                leftText = mActivePages.findViewById(R.id.pg2line1);
                rightText = mActivePages.findViewById(R.id.pg3line1);
                leftIcon = mActivePages.findViewById(R.id.car);
                leftIcon2 = null;
                rightIcon = mActivePages.findViewById(R.id.woman);
                rightIcon2 = mActivePages.findViewById(R.id.man);
                break;
            case 2:
                leftText = mActivePages.findViewById(R.id.pg3line1);
                rightText = mActivePages.findViewById(R.id.pg4line1);
                leftIcon = mActivePages.findViewById(R.id.woman);
                leftIcon2 = mActivePages.findViewById(R.id.man);
                rightIcon = mActivePages.findViewById(R.id.guest1);
                rightIcon2 = mActivePages.findViewById(R.id.guest2);
        }
        if (leftText!=null) leftText.setTranslationX(-Math.round(offset*TEXT_SPEED));
        if (rightText!=null) rightText.setTranslationX(Math.round((FirstRun_Activity.SCREEN_WIDTH-offset)*TEXT_SPEED));
        if (leftIcon!=null) leftIcon.setTranslationX(-Math.round(offset*ICON_SPEED));
        if (leftIcon2!=null) leftIcon2.setTranslationX(-Math.round(offset*ICON2_SPEED));
        if (rightIcon!=null) rightIcon.setTranslationX(Math.round((FirstRun_Activity.SCREEN_WIDTH-offset)*ICON_SPEED));
        if (rightIcon2!=null) rightIcon2.setTranslationX(Math.round((FirstRun_Activity.SCREEN_WIDTH-offset)*ICON2_SPEED));
    }


    public void delayedReveal(View item, int delay) { delayedReveal(item, delay, 1000);  }
    public void delayedReveal(final View item, int delay, final int duration) {
        if (item==null) return;
        Runnable fadein = new Runnable() {
            @Override
            public void run() {
                Animation myFadeInAnimation = AnimationUtils.loadAnimation(item.getContext(), R.anim.fadein);
                myFadeInAnimation.setDuration(duration);
                myFadeInAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        item.setVisibility(View.VISIBLE);
                    }
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                item.startAnimation(myFadeInAnimation);
            }
        };
        item.setVisibility(View.INVISIBLE);
        mHandler.postDelayed(fadein, delay);
    }

    @Override
    public int getItemPosition(Object object) {
        return super.getItemPosition(object);
    }
}
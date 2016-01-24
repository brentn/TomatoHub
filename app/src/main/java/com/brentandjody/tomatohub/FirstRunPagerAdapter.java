package com.brentandjody.tomatohub;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

/**
 * Created by brentn on 18/01/16.
 * Adapter for FirstRun
 */
public class FirstRunPagerAdapter extends PagerAdapter {

    private final Handler mHandler = new Handler();
    private ViewGroup mActivePages;
    private boolean mPg1Drawn, mPg2Drawn, mPg3Drawn, mPg4Drawn;

    public FirstRunPagerAdapter() {
        super();
        mPg1Drawn=false;
        mPg2Drawn=false;
        mPg3Drawn=false;
        mPg4Drawn=false;
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
                break;
            case 1:
                view = inflater.inflate(R.layout.firstrun_page2, null);
                hideItem(view.findViewById(R.id.hint));
                if (!mPg2Drawn) hideItem(view.findViewById(R.id.pg2line2));
                break;
            case 2:
                view = inflater.inflate(R.layout.firstrun_page3, null);
                if (!mPg3Drawn) {
                    hideItem(view.findViewById(R.id.pg3line2));
                    hideItem(view.findViewById(R.id.spy));
                    hideItem(view.findViewById(R.id.no_spy));
                }
                break;
            case 3:
                view = inflater.inflate(R.layout.firstrun_page4, null);
                if (!mPg4Drawn) {
                    hideItem(view.findViewById(R.id.pg4line2));
                    hideItem(view.findViewById(R.id.guest3));
                    hideItem(view.findViewById(R.id.guest4));
                    hideItem(view.findViewById(R.id.guest5));
                }
                break;
        }
        if (!mPg1Drawn && view!=null) {
            delayedReveal(view.findViewById(R.id.imgLogo), 1000);
            delayedReveal(view.findViewById(R.id.pg1line1), 2000);
            delayedReveal(view.findViewById(R.id.imgWifi), 3000, 2000);
            delayedReveal(view.findViewById(R.id.hint), 5500);
            delayedAnimation(view.findViewById(R.id.hint), R.anim.pulse, 5500, 1000);
            mPg1Drawn=true;
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
        final float TEXT_SPEED=5.5f;
        final float TEXT2_SPEED=2.5f;
        final float ICON_SPEED=1.3f;
        final float ICON2_SPEED=.5f;
        View leftText = null;
        View leftText2 = null;
        View rightText = null;
        View rightText2 = null;
        View leftIcon = null;
        View leftIcon2 = null;
        View rightIcon = null;
        View rightIcon2 = null;
        switch (position) {
            case 0:
                leftText = mActivePages.findViewById(R.id.pg1line1);
                rightText = mActivePages.findViewById(R.id.pg2line1);
                rightText2 = mActivePages.findViewById(R.id.pg2line2);
                leftIcon = mActivePages.findViewById(R.id.imgWifi);
                rightIcon = mActivePages.findViewById(R.id.car);
                rightIcon2 = null;
                break;
            case 1:
                leftText = mActivePages.findViewById(R.id.pg2line1);
                leftText2 = mActivePages.findViewById(R.id.pg2line2);
                rightText = mActivePages.findViewById(R.id.pg3line1);
                rightText2 = mActivePages.findViewById(R.id.pg3line2);
                leftIcon = mActivePages.findViewById(R.id.car);
                leftIcon2 = null;
                rightIcon = mActivePages.findViewById(R.id.woman);
                rightIcon2 = mActivePages.findViewById(R.id.man);

                if (offset==0 && !mPg2Drawn) {
                    delayedAnimation(mActivePages.findViewById(R.id.pg2line2), R.anim.slide_in_from_right, 1800, 300);
                    mPg2Drawn=true;
                }
                break;
            case 2:
                leftText = mActivePages.findViewById(R.id.pg3line1);
                leftText2 = mActivePages.findViewById(R.id.pg3line2);
                rightText = mActivePages.findViewById(R.id.pg4line1);
                rightText2 = mActivePages.findViewById(R.id.pg4line2);
                leftIcon = mActivePages.findViewById(R.id.woman);
                leftIcon2 = mActivePages.findViewById(R.id.man);
                rightIcon = mActivePages.findViewById(R.id.guest1);
                rightIcon2 = mActivePages.findViewById(R.id.guest2);
                if (offset==0 && !mPg3Drawn) {
                    delayedAnimation(mActivePages.findViewById(R.id.pg3line2), R.anim.slide_in_from_right, 2200, 300);
                    delayedAnimation(mActivePages.findViewById(R.id.spy), R.anim.slide_in_from_right, 1000, 1000);
                    delayedAnimation(mActivePages.findViewById(R.id.no_spy), R.anim.zoomout, 3000, 200);
                    mPg3Drawn=true;
                }
                break;
            case 3:
                if (offset==0) {
                    if (mPg4Drawn) {
                        ((ImageView) mActivePages.findViewById(R.id.guest1)).setColorFilter(Color.parseColor("#2eb82e"));
                        ((ImageView) mActivePages.findViewById(R.id.guest5)).setColorFilter(Color.parseColor("#2eb82e"));
                    } else {
                        delayedAnimation(mActivePages.findViewById(R.id.pg4line2), R.anim.slide_in_from_right, 2000, 300);
                        delayedAnimation(mActivePages.findViewById(R.id.guest3), R.anim.slide_in_from_left, 600, 900);
                        delayedAnimation(mActivePages.findViewById(R.id.guest4), R.anim.slide_in_from_right, 200, 2300);
                        delayedAnimation(mActivePages.findViewById(R.id.guest5), R.anim.slide_in_from_left, 500, 1300);
                        final ImageView girl = (ImageView) mActivePages.findViewById(R.id.guest1);
                        final ImageView boy = (ImageView) mActivePages.findViewById(R.id.guest5);
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                girl.setColorFilter(Color.parseColor("#2eb82e"));
                            }
                        }, 3000);
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                boy.setColorFilter(Color.parseColor("#2eb82e"));
                            }
                        }, 3600);
                        mPg4Drawn = true;
                    }
                }
                break;
        }
        if (leftText!=null) leftText.setTranslationX(-Math.round(offset*TEXT_SPEED));
        if (leftText2!=null) leftText2.setTranslationX(-Math.round(offset*TEXT2_SPEED));
        if (rightText!=null) rightText.setTranslationX(Math.round((FirstRun_Activity.SCREEN_WIDTH-offset)*TEXT_SPEED));
        if (rightText2!=null) rightText2.setTranslationX(Math.round((FirstRun_Activity.SCREEN_WIDTH-offset)*TEXT2_SPEED));
        if (leftIcon!=null) leftIcon.setTranslationX(-Math.round(offset*ICON_SPEED));
        if (leftIcon2!=null) leftIcon2.setTranslationX(-Math.round(offset*ICON2_SPEED));
        if (rightIcon!=null) rightIcon.setTranslationX(Math.round((FirstRun_Activity.SCREEN_WIDTH-offset)*ICON_SPEED));
        if (rightIcon2!=null) rightIcon2.setTranslationX(Math.round((FirstRun_Activity.SCREEN_WIDTH-offset)*ICON2_SPEED));
    }

    private void hideItem(View view) {
        if (view!=null)
            view.setVisibility(View.INVISIBLE);
    }

    public void delayedAnimation(final View item, final int animation_id, int delay, final int duration) {
        if (item==null) {
            return;
        }
        Runnable animation_runner = new Runnable() {
            @Override
            public void run() {
                Animation animation = AnimationUtils.loadAnimation(item.getContext(), animation_id);
                animation.setDuration(duration);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        item.setVisibility(View.VISIBLE);
                    }
                    @Override
                    public void onAnimationEnd(Animation animation) {}
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                item.startAnimation(animation);
            }
        };
        item.setVisibility(View.INVISIBLE);
        mHandler.postDelayed(animation_runner, delay);
    }

    public void delayedReveal(View item, int delay) { delayedReveal(item, delay, 1000);  }
    public void delayedReveal(View item, int delay, int duration) {
        delayedAnimation(item, R.anim.fadein, delay, duration);
    }

}
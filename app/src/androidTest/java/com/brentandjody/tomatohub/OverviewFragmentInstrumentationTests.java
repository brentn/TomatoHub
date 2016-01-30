package com.brentandjody.tomatohub;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.brentandjody.tomatohub.overview.OverviewFragment;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by brentn on 28/01/16.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class OverviewFragmentInstrumentationTests {
    Instrumentation mInstrumentation;
    TestableActivity mActivity;
    OverviewFragment mOverviewFragment;

    @Before
    public void setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mActivity = new TestableActivity();
        mOverviewFragment = mActivity.getOverviewFragment();
        assertNotNull(mOverviewFragment);
    }

    @Test
    public void onCreateView_fires_SIGNAL_LOADED() {
        mOverviewFragment.onCreateView(LayoutInflater.from(mActivity), new FrameLayout(mActivity), null);
        assertTrue(mActivity.signalLoadedCalled);
    }

    @Test
    public void onCreate_calls_initialize() {
//        assertFalse(mOverviewFragment.isInitialized());
//        mOverviewFragment.onCreateView(null, null, null);
//        assertTrue(mOverviewFragment.isInitialized());
        Assert.fail();
    }

    private static class TestableActivity extends MainActivity {
        public boolean signalLoadedCalled=false;
        public OverviewFragment getOverviewFragment() {return mOverviewFragment;}

        @Override
        public void onSignal(int signal, String parameter) {
            super.onSignal(signal, parameter);
            switch (signal) {
                case OverviewFragment.SIGNAL_LOADED: signalLoadedCalled=true; break;
            }
        }
    }
}

package com.brentandjody.tomatohub;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.FrameLayout;

import com.brentandjody.tomatohub.overview.OverviewFragment;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by brentn on 28/01/16.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class OverviewFragmentInstrumentationTests {
    @Test
    public void onCreateView_fires_SIGNAL_LOADED() {
//        mOverviewFragment.onCreateView(, new FrameLayout(fakeActivity), null);
//        verify(fakeActivity).onSignal(OverviewFragment.SIGNAL_LOADED, null);
        Assert.fail();
    }

    @Test
    public void onCreate_calls_initialize() {
//        assertFalse(mOverviewFragment.isInitialized());
//        mOverviewFragment.onCreateView(null, null, null);
//        assertTrue(mOverviewFragment.isInitialized());
        Assert.fail();
    }
}

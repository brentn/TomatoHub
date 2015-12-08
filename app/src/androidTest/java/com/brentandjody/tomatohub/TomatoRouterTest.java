package com.brentandjody.tomatohub;

import android.app.Activity;

import com.brentandjody.tomatohub.routers.TomatoRouter;


import org.junit.Assert;
import org.junit.Test;

import dalvik.annotation.TestTargetClass;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by brentn on 08/12/15.
 *
 */
public class TomatoRouterTest {

    @Test
    public void throws_error_if_activity_doesnt_implement_OnRouterActivityCompleteListener() {
        try {
            new TomatoRouter(new MainActivity(), null, null);
            Assert.fail();
        } catch (RuntimeException ex) {
            assertTrue(ex.getMessage().contains("OnRouterActivityCompleteListener"));
        }
    }
}

package com.brentandjody.tomatohub;

import com.brentandjody.tomatohub.routers.LinuxRouter;


import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by brentn on 08/12/15.
 *
 */
public class LinuxRouterTest {

    @Test
    public void throws_error_if_activity_doesnt_implement_OnRouterActivityCompleteListener() {
        try {
            new LinuxRouter(new MainActivity(), null, null);
            Assert.fail();
        } catch (RuntimeException ex) {
            assertTrue(ex.getMessage().contains("OnRouterActivityCompleteListener"));
        }
    }
}

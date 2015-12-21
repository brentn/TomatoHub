package com.brentandjody.tomatohub;

import android.os.Handler;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.brentandjody.tomatohub.routers.connection.TestableConnection;

import org.junit.Test;

import java.io.IOException;
import java.net.Socket;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class TestableConnectionTests extends InstrumentationTestCase {

    @Test
    public void listen_sets_up_listener() {
        final int PORT = 8989;
        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Connection connection = new Connection();
                    connection.listen(PORT);
                    System.out.println("TEST");
                    assertTrue(true);
                }
            });
        } catch (Throwable ex) {
            throw new AssertionError(ex.getMessage());
        }
    }

    @Test
    public void listen_calls_onSpeedTestComplete_after_file_sent() {

    }

    @Test
    public void stop_listener_shuts_down_listener() {

    }

    @Test
    public void disconnect_shuts_down_listener() {

    }

}

class Connection extends TestableConnection {
    boolean _complete;

    public Connection() {
        _complete =false;
    }

    public boolean is_complete() {return _complete; }

    @Override
    public void onSpeedTestComplete(boolean success) {
        _complete =true;
    }

    @Override
    public void connect(String ipAddress, String username, String password) {

    }

    @Override
    public String[] execute(String command) {
        return new String[0];
    }




}
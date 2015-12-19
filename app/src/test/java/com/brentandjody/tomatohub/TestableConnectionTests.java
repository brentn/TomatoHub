package com.brentandjody.tomatohub;

import com.brentandjody.tomatohub.routers.connection.TestableConnection;

import org.junit.Test;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class TestableConnectionTests {

    @Test
    public void listen_sets_up_listener() {
        int PORT=4902;
        Connection connection = new Connection();
        connection.listen(PORT);
        try {
            Thread.sleep(1000);
            Socket socket = new Socket("127.0.0.1", PORT);
        } catch (IOException ex) {
            throw new AssertionError(ex.getMessage());
        } catch (InterruptedException ex) {

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

    @Override
    public void onSpeedTestComplete(boolean success) {

    }

    @Override
    public void connect(String ipAddress, String username, String password) {

    }

    @Override
    public String[] execute(String command) {
        return new String[0];
    }

    @Override
    public void executeInBackground(String command) {

    }
}
package com.brentandjody.tomatohub.routers.connection;

/**
 * Created by brentn on 13/12/15.
 * Interface for generic connection to router
 */
public interface IConnection {
    int ACTION_LOGON = 1;
    int ACTION_SPEED_TEST = 2;

    void connect(String ipAddress, String username, String password);
    void disconnect();
    String[] execute(String command);
    void speedTest();
    float getSpeedTestResult();

    interface OnConnectionActionCompleteListener {
        void onActionComplete(int action, boolean success);
    }

}

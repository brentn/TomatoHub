package com.brentandjody.tomatohub.routers.connection;

/**
 * Created by brentn on 13/12/15.
 * Interface for generic connection to router
 */
public interface IConnection {
    void connect(String ipAddress, String username, String password);
    void disconnect();
    String[] execute(String command);

    interface OnLogonCompleteListener {
        void onLogonComplete(boolean success);
    }
}

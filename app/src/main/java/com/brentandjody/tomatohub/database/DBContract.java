package com.brentandjody.tomatohub.database;

import android.provider.BaseColumns;

/**
 * Created by brent on 28/11/15.
 */
public final class DBContract {
    public DBContract() {}

    public static abstract class DeviceEntry implements BaseColumns {
        public static final String TABLE_NAME = "devices";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_MAC = "mac";
        public static final String COLUMN_LAST_IP = "last_ip";
        public static final String COLUMN_ACTIVE = "active";
        public static final String COLUMN_CUSTOM_NAME = "custom_name";
        public static final String COLUMN_TX_BYTES = "tx_bytes";
        public static final String COLUMN_RX_BYTES = "rx_bytes";
        public static final String COLUMN_TRAFFIC_TIMESTAMP = "timestamp";
    }
}

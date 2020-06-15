package com.nitramite.nitspec;

class ConnectionState {

    // Constants that indicate the current connection state of BluetoothService or WifiService
    static final int STATE_NONE = 0;            // Doing nothing
    static final int STATE_LISTEN = 1;          // Listening for incoming connections
    static final int STATE_CONNECTING = 2;      // Initializing outgoing connection
    static final int STATE_CONNECTED = 3;       // Connected to remote device
    static final int STATE_NULL = -1;           // Service sate is null

} // End of class
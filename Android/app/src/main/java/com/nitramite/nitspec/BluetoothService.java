package com.nitramite.nitspec;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

// https://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3/18786701#18786701
@SuppressWarnings("FieldCanBeLocal")
public class BluetoothService extends Service {

    // Logging
    private final static String TAG = BluetoothService.class.getSimpleName();

    // UUID for connection (Stock: 00001101-0000-1000-8000-00805F9B34FB)
    private String BT_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    // Threads
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    // Connection states
    private int connectionState;


    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    // ---------------------------------------------------------------------------------------------

    // Broadcast update to main activity
    private void broadcastUpdate(final String action, final String message) {
        final Intent intent = new Intent(action);
        intent.putExtra(action, message);
        sendBroadcast(intent);
    }

    // ---------------------------------------------------------------------------------------------

    // Set connection state
    private synchronized void setState(int state) {
        connectionState = state;
        //broadcastUpdate();
    }


    // Get current connection state
    public synchronized int getState() {
        return connectionState;
    }



    // Connect to selected device
    public void connect(BluetoothDevice bluetoothDevice) {
        // Cancel any thread attempting to make a connection
        if (connectionState == ConnectionState.STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(bluetoothDevice);
        mConnectThread.start();
        setState(ConnectionState.STATE_CONNECTING);
        broadcastUpdate(ActionState.ACTION_STATE_CONNECTING, "");
    }


    // Stop all threads
    public synchronized void disconnect() {
        setState(ConnectionState.STATE_NONE);
        if (mConnectThread != null) { // Connect thread
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) { // Connected thread
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(ConnectionState.STATE_NONE);
        broadcastUpdate(ActionState.ACTION_STATE_NONE, "");
    }

    // ---------------------------------------------------------------------------------------------

    // Thread that tries to connect to selected device
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket bluetoothSocket = null;
            try {
                final UUID CONSTRUCTED_BT_UUID = UUID.fromString(BT_UUID);
                bluetoothSocket = device.createRfcommSocketToServiceRecord(CONSTRUCTED_BT_UUID);
            } catch (IOException e) {
                Log.i(TAG, "*****: " + e.toString());
            }
            mmSocket = bluetoothSocket;
        }
        public void run() {
            broadcastUpdate(ActionState.ACTION_CANCEL_DISCOVERY, ""); // Always cancel discovery because it will slow down a connection
            try { // Make a connection to the BluetoothSocket
                mmSocket.connect();
            } catch (IOException e) { // Close the socket
                Log.i(TAG, "*e1 Connection failed, trying with fallback " + e.getMessage());
                try {
                    mmSocket = (BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket",
                            new Class[] {int.class}).invoke(mmDevice,1);
                    mmSocket.connect();
                    Log.i(TAG,"Connected");
                } catch (Exception e2) {
                    try {
                        Class<?> clazz = mmSocket.getRemoteDevice().getClass();
                        Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
                        Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                        Object[] params = new Object[]{Integer.valueOf(1)};
                        mmSocket = (BluetoothSocket) m.invoke(mmSocket.getRemoteDevice(), params);
                        mmSocket.connect();
                        Log.i(TAG,"Connected");
                    } catch (Exception e3) {
                        Log.i(TAG, "*e3 " + e3.getMessage());
                        Log.e(TAG, "Couldn't establish Bluetooth connection!");
                        try {
                            mmSocket.close();
                        } catch (IOException e4) {
                            Log.i(TAG, "*e4 " + e4.getMessage());
                        }
                        broadcastUpdate(ActionState.ACTION_CONNECTION_FAILED, e2.toString());
                        return;
                    }
                }
            } catch (NullPointerException e) {
                broadcastUpdate(ActionState.ACTION_CONNECTION_FAILED, e.toString());
                return;
            }
            synchronized (BluetoothService.this) { // Reset the ConnectThread because we're done
                mConnectThread = null;
            }
            connected(mmSocket, mmDevice); // Start the connected thread
        }
        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException ignored) {
            } catch (NullPointerException e) {
                setState(ConnectionState.STATE_NONE);
            }
        }
    }



    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (mConnectThread != null) { // Cancel the thread that completed the connection
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) { // Cancel any thread currently running a connection
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectedThread = new ConnectedThread(socket); // Start the thread to manage the connection and perform transmissions
        mConnectedThread.start();
        setState(ConnectionState.STATE_CONNECTED);
        broadcastUpdate(ActionState.ACTION_STATE_CONNECTED, "Connected to " + device.getName());
    }

    // ---------------------------------------------------------------------------------------------

    // Runs during it's connected to handle data transmissions
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try { // Get the BluetoothSocket input and output streams
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException ignored) {
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer;
            ArrayList<Integer> arr_byte = new ArrayList<>();
            while (true) { // Keep listening to the InputStream while connected
                try {
                    int data = mmInStream.read();
                    if (data == 0x0A) {
                    } else if (data == 0x0D) {
                        buffer = new byte[arr_byte.size()];
                        for (int i = 0; i < arr_byte.size(); i++) {
                            buffer[i] = arr_byte.get(i).byteValue();
                        }
                        if (buffer.length > 0) {
                            Log.i(TAG, "### Bluetooth service in raw data: " + new String(buffer));
                            broadcastUpdate(ActionState.ACTION_SCOPE_MESSAGE_IN, new String(buffer));
                        }
                        arr_byte = new ArrayList<>();
                    } else {
                        try {
                            arr_byte.add(data);
                        } catch (OutOfMemoryError e) {
                            arr_byte = new ArrayList<>();
                        }
                    }
                } catch (IOException e) {
                    broadcastUpdate(ActionState.ACTION_CONNECTION_LOST, e.toString());
                    try {
                        mmSocket.close(); // Close socket
                    } catch (Exception ignored) {
                    }
                    break;
                }
            }
        }
        void write(byte[] buffer) { // Write to the connected OutStream.
            try {
                mmOutStream.write(buffer); // Maybe should return sent message back to ui ?
                mmOutStream.flush(); // 11.5.2018 experiment
            } catch (IOException ignored) {
            }
        }
        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException ignored) {
            } catch (NullPointerException e) {
                setState(ConnectionState.STATE_NONE);
            }
        }
    }


    // Write data to connected bluetooth socket
    public void write(String input, boolean CRLF) {
        try {
            ConnectedThread r; // Create temporary object
            synchronized (this) {
                if (connectionState != ConnectionState.STATE_CONNECTED) return;
                r = mConnectedThread;
            }
            if (CRLF) {
                input += "\r\n";
            }
            r.write(input.getBytes());
        } catch (NullPointerException ignored) {
        }
    }

    // ---------------------------------------------------------------------------------------------

} // End of class
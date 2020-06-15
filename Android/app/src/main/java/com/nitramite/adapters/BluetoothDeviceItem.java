package com.nitramite.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

public class BluetoothDeviceItem {

    // Variables
    private Context context;
    private String deviceName, deviceMacAddress;
    private BluetoothDevice bluetoothDevice;

    // Constructor
    public BluetoothDeviceItem(Context context, BluetoothDevice bluetoothDevice) {
        this.context = context;
        this.bluetoothDevice = bluetoothDevice;
        this.deviceName = bluetoothDevice.getName();
        this.deviceMacAddress = bluetoothDevice.getAddress();
    }

    public Context getContext() {
        return this.context;
    }

    public BluetoothDevice getBluetoothDevice() {
        return this.bluetoothDevice;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceMacAddress() {
        return deviceMacAddress;
    }

} // End of class
package com.hugh.blelib;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import java.io.UnsupportedEncodingException;


interface BleListener {
    interface OnLeScanListener {
        void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord);
    }

    interface OnConnectionStateChangeListener {
        void onConnectionStateChange(BluetoothGatt gatt, int status, int newState);
    }

    interface OnServicesDiscoveredListener {
        void onServicesDiscovered(BluetoothGatt gatt, int status);
    }

    interface OnDataAvailableListener {
        void onCharacteristicRead(BluetoothGatt gatt,
                                  BluetoothGattCharacteristic characteristic, int status);

        void onCharacteristicChanged(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic) throws UnsupportedEncodingException;

        void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status);
    }

    interface OnReadRemoteRssiListener {
        void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status);
    }

    interface OnMtuChangedListener {
        void onMtuChanged(BluetoothGatt gatt, int mtu, int status);
    }
}

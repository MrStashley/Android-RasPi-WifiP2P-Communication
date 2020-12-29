package com.isopod.wifidirecttestapp2;

// BluetoothGattCallback handler for connecting to wifi direct

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

public class ConnectionHandlerGATTCallback extends BluetoothGattCallback {
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";


    private MainActivity mainActivity;
    private ConnectionBLEHandler connectionBLEHandler;

    public ConnectionHandlerGATTCallback(MainActivity mainActivity, ConnectionBLEHandler connectionBLEHandler){
        this.connectionBLEHandler = connectionBLEHandler;
        this.mainActivity = mainActivity;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if(newState == BluetoothProfile.STATE_CONNECTED) {
            // there was a successful connection
            mainActivity.addTextToScreen("Connected. discovering services");
            gatt.discoverServices(); // start service discovery

        } else if(newState == BluetoothProfile.STATE_DISCONNECTED){
            // either gatt.disconnect() was called, or there wsa an issue connecting
            mainActivity.addTextToScreen("Disconnected from BLE");
            gatt.close(); // free resources
            connectionBLEHandler.freeBluetoothGatt();
        }

    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if(status == BluetoothGatt.GATT_SUCCESS){
            mainActivity.addTextToScreen("Services discovered");
            getChars(gatt); // extract the two chars we need
        } else{
            mainActivity.addTextToScreen("Service discovery failed");
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
        //mainActivity.addTextToScreen("Characteristic write: " + characteristic.getUuid());
        //mainActivity.addTextToScreen("characteristic value: " + characteristic.getStringValue(0));
        if(characteristic.getUuid().equals(connectionBLEHandler.UUID_DEVICE_NAME_CHAR) && status == BluetoothGatt.GATT_SUCCESS){
            mainActivity.addTextToScreen("Sent device name success");
            connectionBLEHandler.sentDeviceName();;
        } else if(characteristic.getUuid().equals(connectionBLEHandler.UUID_START_CONNECT_CHAR) && status == BluetoothGatt.GATT_SUCCESS){
            mainActivity.addTextToScreen("Sent start connect success");
            connectionBLEHandler.checkConnection();
        }
    }

    public void getChars(BluetoothGatt gatt){
        BluetoothGattService connectService = gatt.getService(connectionBLEHandler.UUID_CONNECT_SERVICE);

        if(connectService == null){ // error
            mainActivity.addTextToScreen("service not found");
        } else {
            // we found the service we want
            BluetoothGattCharacteristic deviceNameChar = connectService.getCharacteristic(ConnectionBLEHandler.UUID_DEVICE_NAME_CHAR);
            BluetoothGattCharacteristic startConnectChar = connectService.getCharacteristic(ConnectionBLEHandler.UUID_START_CONNECT_CHAR);

            if(deviceNameChar == null || startConnectChar == null){ // error
                mainActivity.addTextToScreen("One of the characteristics needed was " +
                        "not able to be found. Error.");
            } else {
                // we have found the two chars we need and have references to them, now
                // pass them to the main object so that they can be used later
                connectionBLEHandler.setCharObjects(deviceNameChar, startConnectChar);
            }
        }
    }


}

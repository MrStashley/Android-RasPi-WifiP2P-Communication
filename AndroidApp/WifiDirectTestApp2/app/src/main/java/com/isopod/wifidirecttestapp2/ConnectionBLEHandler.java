package com.isopod.wifidirecttestapp2;

// handles the BLE connection needed in the wifi direct connection protocol
// the protocol uses BLE to exchange device name information and to tell the
// peripheral to attempt to connect via wifi direct


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.LogRecord;

public class ConnectionBLEHandler {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;

    public static final UUID UUID_CONNECT_SERVICE = UUID.fromString("19B10010-E8F2-537E-4F6C-D104768A1214");
    public static final UUID UUID_DEVICE_NAME_CHAR = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_START_CONNECT_CHAR = UUID.fromString("00001102-0000-1000-8000-00805f9b34fb");

    private String deviceName;

    private MainActivity mainActivity;
    private WifiDirectConnector wifiDirectConnector;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean mScanning;
    private Handler handler;

    private List<ScanFilter> BTLEScanFilterList;
    private ScanSettings BTLEScanSettings;

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic deviceNameChar;
    private BluetoothGattCharacteristic startConnectChar;

    private ConnectionHandlerGATTCallback gattCallback;

    // scan callback
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // scan has found an available device
            super.onScanResult(callbackType, result);
            mainActivity.addTextToScreen("Device Found: " + result.getDevice().getName());
            BluetoothDevice bluetoothDevice = result.getDevice();
            if(bluetoothGatt == null && bluetoothDevice != null && bluetoothDevice.getName() != null && bluetoothDevice.getName().equals(deviceName)){
                // this is the device we're looking for, attempt connection
                mainActivity.addTextToScreen("Device found. Attempting to Connect...");
                bluetoothGatt = bluetoothDevice.connectGatt(mainActivity, false, gattCallback);
                bluetoothLeScanner.stopScan(this);
                mScanning = false;
            }

            // if it get's here without entering the if statement, this is either not the device we're
            // looking for or there was some kind of error. Considering the scan filters, it's probably
            // much more likely that there is an error.
            //
            // There was an error where the device was found, but result.getDevice().getName() was null
            // This happened every time and continued to happen after restarting the android device
            // and the peripheral. It was fixed after using the app nRF connect to initiate a connection
            // with the peripheral and then disconnect. I need to look into this error more. It appears
            // something was configured wrong, and my code is not doing the same thing that nRF connect is
            // to initiate a connection
        }
    };

    public ConnectionBLEHandler(MainActivity mainActivity, WifiDirectConnector wifiDirectConnector){
        this.mainActivity = mainActivity;
        this.wifiDirectConnector = wifiDirectConnector;
        mainActivity.addTextToScreen("ConnectionBLEHandler contructor");

        bluetoothManager = (BluetoothManager) mainActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        mScanning = false;
        handler = new Handler();

        deviceName = "";

        gattCallback = new ConnectionHandlerGATTCallback(mainActivity, this);

        bluetoothGatt = null;
        deviceNameChar = null;
        startConnectChar = null;

        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mainActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    public void setCharObjects(BluetoothGattCharacteristic deviceNameChar, BluetoothGattCharacteristic startConnectChar){
        // stores references to the two chars that are going to be used to initiate the connection
        // this method is called by the ConnectionHandlerGATTCallback object when it successfully
        // discovers services
        this.deviceNameChar = deviceNameChar;
        this.startConnectChar = startConnectChar;

        // at this point we are successfully connected and ready to proceed to the next steps
        // letting the WifiDirectConnector object know this so that it can proceed
        wifiDirectConnector.connectedToBluetooth();
    }

    // start scan, which evokes the scan callback, which connects,
    // which gets references to the characteristic objects needed to
    // send data later
    // Why tf android programming gotta be like making a rube goldberg machine
    public void acquireChars(){
        if(!mScanning){
            // handler stops scanning after scan period
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(mScanning) {
                        mScanning = false;
                        bluetoothLeScanner.stopScan(leScanCallback);
                        mainActivity.addTextToScreen("Scan ended");
                    }
                }
            }, SCAN_PERIOD);

            mScanning = true;
            // start scan with the proper filters
            bluetoothLeScanner.startScan(BTLEScanFilterList, BTLEScanSettings, leScanCallback);
            mainActivity.addTextToScreen("Starting scan");
        } else {
            mScanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    // initiate connection
    public void connect(String deviceName){
        this.deviceName = deviceName; // name of peripheral
        mainActivity.addTextToScreen("Starting scan for chars");
        // build scan filter settings to only search for devices with the given deviceName
        BTLEScanFilterList = new ArrayList<ScanFilter>();
        ScanFilter curFilter = new ScanFilter.Builder().setDeviceName(deviceName).build();
        BTLEScanFilterList.add(curFilter);

        BTLEScanSettings = new ScanSettings.Builder()
                .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH).build();

        acquireChars();
    }

    // sends device name to the BLE characteristic on the peripheral
    public void sendDeviceName(String deviceName){
        byte[] to_send = deviceName.getBytes(); // device name in null terminated bytes form
        deviceNameChar.setValue(to_send);
        boolean writeStatus = bluetoothGatt.writeCharacteristic(deviceNameChar);
        mainActivity.addTextToScreen("Sending device name success: " + writeStatus);

        // if write status is true, the operation succeeded. otherwise, the operation failed
    }

    // sends a truthy value to the start_connect BLE characteristic to tell the peripheral to connect
    // over wifi direct
    public void startConnect(){
        byte[] to_send = {1}; // one byte that is nonzero
        startConnectChar.setValue(to_send);
        boolean writeStatus = bluetoothGatt.writeCharacteristic(startConnectChar);
        mainActivity.addTextToScreen("Starting connect success: " + writeStatus);

        // unresolved bug here: the second characteristic written seems to always fail.
        // I am going to try adding some delay
    }

    public void disconnect(){
        mainActivity.addTextToScreen("disconnecting naturally from BLE");
        if(bluetoothGatt != null) {
            mainActivity.addTextToScreen("Found gatt object");
            bluetoothGatt.disconnect();
        }
    }

    public void freeBluetoothGatt(){
        bluetoothGatt = null;
    }

    public void sentDeviceName(){
        // passes the message from the ConnectionHandlerGATTCallback to the WifiDirectConnector
        wifiDirectConnector.sentDeviceName();
    }

    public void checkConnection(){
        // passes the message from the ConnectionHandlerGATTCallback to the WifiDirectConnector
        wifiDirectConnector.checkConnection();
    }

}

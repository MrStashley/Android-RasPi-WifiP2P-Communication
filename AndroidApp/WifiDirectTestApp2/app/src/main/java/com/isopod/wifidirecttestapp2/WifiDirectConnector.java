package com.isopod.wifidirecttestapp2;

// handles connection to a wifi direct peripheral

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

public class WifiDirectConnector {
    private String localDeviceName;
    private String peripheralDeviceName;
    private MainActivity mainActivity;
    private ConnectionBLEHandler connectionBLEHandler;

    WifiP2pManager wifiP2pManager;
    WifiP2pManager.Channel channel;
    WifiDirectBroadcastReceiver wifiP2pReceiver;

    IntentFilter intentFilter;

    WifiP2pDevice peripheralDevice;

    public WifiDirectConnector(MainActivity mainActivity){

        this.mainActivity = mainActivity;
        this.connectionBLEHandler = new ConnectionBLEHandler(mainActivity, this);

        localDeviceName = "";

        wifiP2pManager = (WifiP2pManager) mainActivity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(mainActivity, mainActivity.getMainLooper(), null);

        wifiP2pReceiver = new WifiDirectBroadcastReceiver(wifiP2pManager, channel, mainActivity, this);
        registerIntents();
        mainActivity.setBroadcastReceiver(wifiP2pReceiver, intentFilter);

        peripheralDevice = null;
    }

    public void registerIntents(){
        // these actions need to be registered before they can be seen in the BroadCast Receiver

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    public void setLocalDeviceName(String localDeviceName){
        // allows the broadcast receiver or other classes to set the local device name
        this.localDeviceName = localDeviceName;
    }

    public String getPeripheralDeviceName(){
        // gives the peripheral device name to the broadcast receiver
        return peripheralDeviceName;
    }

    public void discover(){
        // wifi direct P2P_FIND
    }

    public void startConnection(String deviceName){
        // Connect to deviceName with BLE
        this.peripheralDeviceName = "DIRECT-" + deviceName;
        connectionBLEHandler.connect(deviceName);
    }

    public void connectedToBluetooth(){
        // called by the ConnectionBLEHandler when a successful BLE connection is made

        p2p_find();
    }

    public void p2p_find(){
        // discover peers
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mainActivity.addTextToScreen("p2p_find success");
            }

            @Override
            public void onFailure(int reason) {
                mainActivity.addTextToScreen("p2p_find failed");
            }
        });

    }

    public void setDeviceObject(WifiP2pDevice peripheralDevice){
        // broadcast receiver handles discovery, looks for device,
        // and then calls this method to give it to this object so it can connect
        this.peripheralDevice = peripheralDevice;
        mainActivity.addTextToScreen("set device object");
    }

    public void sendDeviceName(){
        //send local device name to proper BLE char
        //localDeviceName = "moto g(7) play";

        mainActivity.addTextToScreen(localDeviceName);
        connectionBLEHandler.sendDeviceName(localDeviceName);
    }

    public void sentDeviceName(){
        //initiate connection to device over wifi direct
        // - wifi direct device name = "DIRECT-" + deviceName
        WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
        wifiP2pConfig.deviceAddress = peripheralDevice.deviceAddress;

        wifiP2pManager.connect(channel, wifiP2pConfig, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mainActivity.addTextToScreen("Wifi direct connection success");
                // there is a bug here: android successfully invites peripheral, peripheral says that it is connected, but no actual connection is made
                bluetoothStartConnect();
            }

            @Override
            public void onFailure(int reason) {
                mainActivity.addTextToScreen("Wifi direct connection failure");
            }
        });
    }

    public void bluetoothStartConnect(){
        //send START_CONNECT to proper BLE char
        connectionBLEHandler.startConnect();
    }

    public void checkConnection(){
        // check if connected
        // if connected:
        connectionBLEHandler.disconnect();
        mainActivity.wifiDirectConnected();
        // else:
        // do some error handling, try to figure out what went wrong
        // maybe mainActivity.wifiDirectFailed();

    }


}

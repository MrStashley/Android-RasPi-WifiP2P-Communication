package com.isopod.wifidirecttestapp2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import java.util.Collection;

public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private MainActivity mainActivity;
    private WifiDirectConnector wifiDirectConnector;

    private boolean connected = false;

    WifiP2pManager.PeerListListener peerListListener;

    public WifiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       MainActivity mainActivity, WifiDirectConnector wifiDirectConnector){
        this.manager = manager;
        this.channel = channel;
        this.mainActivity = mainActivity;
        this.wifiDirectConnector = wifiDirectConnector;
        peerListListener = new WifiDirectPeerListListener(mainActivity, wifiDirectConnector);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();


        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

            if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                mainActivity.addTextToScreen("Wifi p2p enabled");
            } else {
                mainActivity.addTextToScreen("Wifi p2p not enabled");
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers

            if(manager != null){
                manager.requestPeers(channel, peerListListener);
                // gets peers, this callback is handled in onPeersAvailable of the WifiDirectPeerListListener
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            WifiP2pInfo groupInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
            if(!connected && groupInfo != null && groupInfo.groupFormed) {
                connected = true;
                wifiDirectConnector.connectionMade();
            }
            else
                connected = false;

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            // getting this action allows the broadcast receiver to tell the wifiDirectConnector what
            // the local devices' wifi direct device name is
            WifiP2pDevice localDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            mainActivity.addTextToScreen("Found local device name");
            wifiDirectConnector.setLocalDeviceName(localDevice.deviceName);

        }

    }
}

class WifiDirectPeerListListener implements WifiP2pManager.PeerListListener{

    MainActivity mainActivity;
    WifiDirectConnector wifiDirectConnector;

    private final int P2P_FIND_LIMIT = 1;
    private int p2p_find_num = 0;

    public WifiDirectPeerListListener(MainActivity mainActivity, WifiDirectConnector wifiDirectConnector){
        this.mainActivity = mainActivity;
        this.wifiDirectConnector = wifiDirectConnector;
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {

        if(p2p_find_num < P2P_FIND_LIMIT) {
            p2p_find_num++;
            findDevice(peers);
        }
    }

    public void findDevice(WifiP2pDeviceList peers){
        // iterate through peers found and find peer

        String deviceName = wifiDirectConnector.getPeripheralDeviceName();
        Collection<WifiP2pDevice> deviceList = peers.getDeviceList();

        boolean found = false;

        for (WifiP2pDevice device : deviceList) {
            mainActivity.addTextToScreen("Device: " + device.deviceName);
            if (device.deviceName.equals(deviceName)) {
                mainActivity.addTextToScreen("Found device: " + deviceName);
                wifiDirectConnector.setDeviceObject(device);
                found = true;
                break;
            }
        }

        if (found)
            wifiDirectConnector.sendDeviceName();
        else {
            mainActivity.addTextToScreen("device Not Found");
        }
    }
}

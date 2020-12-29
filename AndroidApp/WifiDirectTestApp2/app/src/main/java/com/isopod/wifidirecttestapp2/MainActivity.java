package com.isopod.wifidirecttestapp2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    public class WifiDirectConnListener implements WifiP2pManager.ConnectionInfoListener {
        private MainActivity curActivity;

        public WifiDirectConnListener(MainActivity curActivity){
            this.curActivity = curActivity;
        }

        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            if(info == null){
                showMessage("Group info available but group is null", getApplicationContext());
                return;
            }
            showMessage("Group Data is available!", getApplicationContext());
            String curGroupString = "Group: \n";
            curGroupString += "\t" + "groupFormed: " + info.groupFormed + "\n";
            curGroupString += "\t" + "isGroupOwner: " + info.isGroupOwner + "\n";
            curGroupString += "\t" + "groupOwnerAddress: " + info.groupOwnerAddress + "\n";
            if(deviceList == null){
                deviceList = findViewById(R.id.devicelist);
                if(deviceList == null) {
                    showMessage(info.toString(), getApplicationContext());
                    return;
                }
            }
            String oldString = (String) deviceList.getText();
            if(oldString == null)
                oldString = "";
            deviceList.setText(oldString + curGroupString);
            if(info.groupFormed){
                /*TCPSocketThread tcpThread = new TCPSocketThread(info.isGroupOwner, info.groupOwnerAddress, PORT, curActivity);
                tcpThread.start();*/
                // using the tcpSocketManager allows the tcp thread to safely send text to the UI using a handler, but you can also
                //start the thread straight from here if you only require one thread, uncomment the above code and comment the code below to do so
                TCPSocketManager tcpSocketManager = new TCPSocketManager(PORT,info.isGroupOwner, info.groupOwnerAddress, curActivity);
                tcpSocketManager.startSocketThreads();
            }
        }

    }

    private static final int REQUEST_ACCESS_FINE_LOCATION = 1;

    public static final int PORT = 4444;
    public static final String DEVICE_NAME = "SILQ_1";
    private Context context;
    private boolean ready = false;
    private WifiManager wifiManager;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private IntentFilter intentFilter;
    private WifiDirectBroadcastReceiver wifiDirectBroadcastReceiver;
    private boolean inProgress;
    private TextView deviceList;
    private WifiDirectConnector wifiDirectConnector;
    private Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceList = findViewById(R.id.devicelist);
        context = getApplicationContext();
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        showMessage(Looper.getMainLooper().toString(), context);
        mChannel = wifiP2pManager.initialize(context, Looper.getMainLooper(), null);

        wifiDirectConnector = new WifiDirectConnector(this);
        wifiDirectConnector.discover();
    }

    public void setBroadcastReceiver(WifiDirectBroadcastReceiver wifiDirectBroadcastReceiver, IntentFilter intentFilter){
        this.wifiDirectBroadcastReceiver = wifiDirectBroadcastReceiver;
        this.intentFilter = intentFilter;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(wifiDirectBroadcastReceiver, intentFilter);
    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiDirectBroadcastReceiver);
    }

    public void SearchButtonPressed(View view){
        checkSystem();
    }

    public void checkSystem(){
        // check perms and system requirements
        // if any requirement is not met, the program must attempt to meet the requirement
        // and then return here to check again, until it makes it to the else statement, in which
        // the system is considered checked
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_DENIED){
            addTextToScreen("Need fine location permission");
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_FINE_LOCATION);
        } else {
            addTextToScreen("System checked!");
            systemChecked();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_ACCESS_FINE_LOCATION){
            if(grantResults.length > 0 && grantResults.length == PackageManager.PERMISSION_GRANTED){
                checkSystem();
            }
        }
    }

    public void systemChecked(){
        wifiDirectConnector.startConnection(DEVICE_NAME);
    }

    public void wifiDirectConnected(){
        final MainActivity mainActivity = this;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                wifiP2pManager.requestConnectionInfo(mChannel, new WifiDirectConnListener(mainActivity));
            }
        }, 10000);
    }

    public static String showMessage(String message, Context curContext){
        String toReturn = "success";
        try {
            Toast.makeText(curContext, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e){
            toReturn = e.getMessage();
        }
        return toReturn;
    }

    public void addTextToScreen(String messageToAdd){
        if(deviceList == null){
            deviceList = findViewById(R.id.devicelist);
            if(deviceList == null) {
                showMessage(messageToAdd, getApplicationContext());
                return;
            }
        }
        String oldString = (String) deviceList.getText();
        if(oldString == null)
            oldString = "";
        deviceList.setText(oldString + messageToAdd + "\n");
    }

}

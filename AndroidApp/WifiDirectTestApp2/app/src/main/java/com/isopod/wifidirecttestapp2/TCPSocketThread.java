package com.isopod.wifidirecttestapp2;

import android.os.StrictMode;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class TCPSocketThread extends Thread{
    private boolean isGroupOwner;
    private InetAddress groupOwnerAddress;
    private int port;
    private Socket socket;
    private ServerSocket serverSocket;
    private TCPSocketManager tcpSocketManager;

    public TCPSocketThread(boolean isGroupOwner,InetAddress groupOwnerAddress, int port){
        this.isGroupOwner = isGroupOwner;
        this.groupOwnerAddress = groupOwnerAddress;
        this.port = port;
    }

    public void setTcpSocketManager(TCPSocketManager tcpSocketManager){
        this.tcpSocketManager = tcpSocketManager;
    }

    @Override
    public void run(){
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Log.d(null, "running TCPSocketThread");
        if(isGroupOwner){
            try {
                runGO();
            } catch (Exception e) {
                Log.d(null,"Exception Thrown: " + e.getMessage());
                StackTraceElement[] trace = e.getStackTrace();
                for(StackTraceElement curTrace : trace)
                    Log.d(null,curTrace.toString());
            }
        } else {
            try {
                runClient();
            } catch (Exception e) {
                Log.d(null,"Exception Thrown: " + e.getMessage());
                StackTraceElement[] trace = e.getStackTrace();
                for(StackTraceElement curTrace : trace)
                    Log.d(null,curTrace.toString());
            }
        }
    }

    public void runGO() throws IOException, ClassNotFoundException {
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        tcpSocketManager.addTextToScreen("Starting Server Socket " + port);
        socket = serverSocket.accept();
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        Object object = ois.readObject();
        tcpSocketManager.addTextToScreen("Message Received from " + socket.getInetAddress() + ": " + object);
    }

    public void runClient() throws IOException {
        socket = new Socket();
        socket.setReuseAddress(true);
        tcpSocketManager.addTextToScreen("Starting Client Socket " + groupOwnerAddress + " " + port);
        socket.connect((new InetSocketAddress(groupOwnerAddress, port)), 1000);
        OutputStream os = socket.getOutputStream();
        os.write("Whats up!".getBytes());
        //ObjectOutputStream oos = new ObjectOutputStream(os);
        //oos.writeObject(new String("Whats up! Test 1!!"));

        //oos.close();
        os.close();
        socket.close();
    }

    /*public void start(){
        run();
    }*/
}

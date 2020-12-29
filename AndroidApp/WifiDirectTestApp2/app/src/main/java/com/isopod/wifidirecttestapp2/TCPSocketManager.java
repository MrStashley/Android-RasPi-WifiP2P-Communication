package com.isopod.wifidirecttestapp2;

import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

enum MessageType {
    CLOSE;
}

class ThreadMessage{
    // Type of thread message
    // can extend this class and add different types of thread message with different data
    // or could add a data field to the base class
    // add values to MessageType to make new messages as well
    private MessageType message;

    public ThreadMessage(MessageType message){
        this.message = message;
    }

    public MessageType getMessage(){
        return message;
    }
}

class SocketThreadCommunicationQueue{
    // give all communicators a reference to communication queues
    // inherently thread safe since the recipient only reads from their queue and the sender
    // only adds to it
    public Queue<ThreadMessage> toInputThread;
    public Queue<ThreadMessage> toOutputThread;

    public SocketThreadCommunicationQueue(){
        toInputThread = new LinkedList<ThreadMessage>();
        toOutputThread = new LinkedList<ThreadMessage>();
    }

    public void sendToInputThread(ThreadMessage toSend){
        toInputThread.add(toSend);
    }

    public ThreadMessage getNextToInputThread(){
        return toInputThread.poll();
    }

    public void sendToOutputThread(ThreadMessage toSend){
        toOutputThread.add(toSend);
    }

    public ThreadMessage getNextToOutputThread(){
        return toOutputThread.poll();
    }


}

class TCPConnectionThread extends Thread{
    private int port;
    private boolean isGroupOwner;
    private InetAddress groupOwnerAddress;
    private Socket socket;
    private ServerSocket serverSocket;
    private TCPSocketManager tcpSocketManager;
    private SocketThreadCommunicationQueue threadCommQueue;

    public TCPConnectionThread(int port, boolean isGroupOwner, InetAddress groupOwnerAddress, TCPSocketManager tcpSocketManager){
        this.groupOwnerAddress = groupOwnerAddress;
        this.tcpSocketManager = tcpSocketManager;
        this. isGroupOwner = isGroupOwner;
        this.port = port;
        threadCommQueue = new SocketThreadCommunicationQueue();
    }

    @Override
    public void run(){
        /*Uncomment this code and comment the other code in this method
        to run the Input Thread and output thread. Keep this code commented to run the
        simpler single thread socket example in TCPSocketThread.java
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
        }*/
        TCPSocketThread tcpThread = new TCPSocketThread(isGroupOwner,groupOwnerAddress, port);
        tcpThread.setTcpSocketManager(tcpSocketManager);
        tcpThread.setDaemon(true);
        tcpThread.start();
    }

    public void runGO() throws IOException, InterruptedException {
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        Log.d(null,"Starting Server Socket " + port);
        socket = serverSocket.accept(); // wait for connection
        //once connection is made, start input and output thread
        TCPOutputThread tcpOutputThread = new TCPOutputThread(socket, socket.getInetAddress(),isGroupOwner, port, tcpSocketManager, threadCommQueue);
        TCPInputThread tcpInputThread = new TCPInputThread(socket, isGroupOwner, port, tcpSocketManager, threadCommQueue);
        tcpInputThread.setDaemon(true);
        tcpInputThread.start();
        tcpOutputThread.setDaemon(true);
        tcpOutputThread.start();
    }

    public void runClient() throws IOException {
        socket = new Socket();
        socket.setReuseAddress(true);
        Log.d(null,"Starting Client Socket " + groupOwnerAddress + " " + port);
        socket.connect((new InetSocketAddress(groupOwnerAddress, port)), 1000); // try to connect
        //if connection is made, start input and output thread
        TCPOutputThread tcpOutputThread = new TCPOutputThread(socket, groupOwnerAddress,isGroupOwner, port, tcpSocketManager, threadCommQueue);
        TCPInputThread tcpInputThread = new TCPInputThread(socket, isGroupOwner, port, tcpSocketManager, threadCommQueue);
        tcpInputThread.setDaemon(true);
        tcpInputThread.start();
        tcpOutputThread.setDaemon(true);
        tcpOutputThread.start();
    }

}

class TCPOutputThread extends Thread{
    private boolean runLoop;
    private Socket socket;
    private InetAddress sendAddress;
    private boolean isGroupOwner;
    private int port;
    private  TCPSocketManager tcpSocketManager;
    private SocketThreadCommunicationQueue threadCommQueue;
    private OutputStream os;
    private ObjectOutputStream oos;


    public TCPOutputThread(Socket socket, InetAddress sendAddress, boolean isGroupOwner, int port, TCPSocketManager tcpSocketManager, SocketThreadCommunicationQueue threadCommQueue){
        runLoop = true;
        this.socket = socket;
        this.isGroupOwner = isGroupOwner;
        this.sendAddress =sendAddress;
        this.port = port;
        this.tcpSocketManager = tcpSocketManager;
        this.threadCommQueue = threadCommQueue;
    }
    // this thread is designed only to send output, useful if you need fast two way communication and don't have time to send and then wait for a response
    // or if you need to output at a faster rate then you want to get input
    // I need this communication scheme for a project I am working on, but most use cases can be satisfied with just one thread, check out TCPSocketThread.java

    @Override
    public void run(){
        try{
            runOutputThread();
        } catch (Exception e){
            Log.d(null,e.getMessage());
        }
    }

    public void runOutputThread() throws IOException {
        os = socket.getOutputStream();
        oos = new ObjectOutputStream(os);
        oos.writeObject(new String("Whats up! Test 2!!"));
        while (runLoop) {
            // code for communications goes here
            handleThreadComm();
        }
    }

    public void handleThreadComm() throws IOException {
        // handle communications from input thread or ui thread
        // check out the SocketThreadCommunicationQueue to see how this works
        ThreadMessage curMessage = threadCommQueue.getNextToOutputThread();
        if(curMessage == null)
            return;
        switch (curMessage.getMessage()){
            case CLOSE:
                oos.close();
                os.close();
                socket.close();
                runLoop = false;
                break;
        }
    }
}

class TCPInputThread extends Thread{
    // this thread is designed only to listen for input, useful if you need fast two way communication and don't have time to send and then wait for a response
    // or if you need to output at a faster rate then you want to get input
    // I need this communication scheme for a project I am working on, but most use cases can be satisfied with just one thread, check out TCPSocketThread.java

    private boolean runLoop;
    private Socket socket;
    private boolean isGroupOwner;
    private int port;
    private  TCPSocketManager tcpSocketManager;
    private SocketThreadCommunicationQueue threadCommQueue;
    ObjectInputStream ois;

    public TCPInputThread(Socket socket, boolean isGroupOwner, int port, TCPSocketManager tcpSocketManager, SocketThreadCommunicationQueue threadCommQueue){
        this.socket = socket;
        this.isGroupOwner = isGroupOwner;
        this.port = port;
        this.tcpSocketManager = tcpSocketManager;
        this.threadCommQueue = threadCommQueue;
        runLoop = true;
    }

    public void run(){
        try{
            runInputThread();
        } catch (Exception e){
            Log.d(null,e.getMessage());
        }
    }

    public void runInputThread() throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        while(runLoop){
            // code for communication goes here
            Object object = ois.readObject();
            Log.d(null,"Message Received: " + object);
            Log.d(null,"Client IP address: " + socket.getInetAddress());
            // if object == [object of some sort] { doSomething()};
            handleThreadComm();
        }
    }

    public void handleThreadComm(){
        // handle communications from output thread or ui thread
        ThreadMessage curMessage = threadCommQueue.getNextToInputThread();
        if(curMessage == null)
            return;

    }

}

class AddTextToScreenMessageContainer{
    public MainActivity curActivity;
    public String message;

    public AddTextToScreenMessageContainer(MainActivity curActivity, String message){
        this.curActivity = curActivity;
        this.message = message;
    }
}

public class TCPSocketManager {
    private int port;
    private boolean isGroupOwner;
    private InetAddress groupOwnerAddress;
    private MainActivity curActivity;
    private android.os.Handler handler;

    public TCPSocketManager(int port, boolean isGroupOwner, InetAddress groupOwnerAddress, MainActivity curActivity){
        this.curActivity = curActivity;
        this.groupOwnerAddress = groupOwnerAddress;
        this.isGroupOwner = isGroupOwner;
        this.port = port;
    }

    public void startSocketThreads(){
        Log.d(null, "running TCPSocketThread");
        handler = new android.os.Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage){
                switch (inputMessage.what){
                    case 0:
                        AddTextToScreenMessageContainer curObj = (AddTextToScreenMessageContainer) inputMessage.obj;
                        curObj.curActivity.addTextToScreen(curObj.message);
                        break;
                }
            }
        };
        TCPConnectionThread tcpConnectionThread = new TCPConnectionThread(port,isGroupOwner,groupOwnerAddress, this);
        tcpConnectionThread.setDaemon(true);
        tcpConnectionThread.start();
    }

    public void addTextToScreen(String message){
        Message completeMessage = handler.obtainMessage(0, new AddTextToScreenMessageContainer(curActivity, message));
        completeMessage.sendToTarget();
    }
}

package com.alaminkarno.bluetoothchat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ChatUtils {

    private Context context;
    private final Handler handler;

    private ConnectThread connectThread;
    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;
    private BluetoothAdapter bluetoothAdapter;

    private final UUID APP_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private final String APP_NAME = "BluetoothChatApp";

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private int state;

    public ChatUtils(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;

        state = STATE_NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public int getState() {
        return state;
    }

    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGED, state, -1).sendToTarget();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private synchronized void start() {

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_LISTEN);
    }

    public synchronized void stop() {

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_NONE);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    public void connect(BluetoothDevice device) {

        if (state == STATE_CONNECTING) {
            connectThread.cancel();
            connectThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();

        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_CONNECTING);
    }

    public void write(byte[] buffer){
        ConnectedThread conThread;
        synchronized (this){
            if(state != STATE_CONNECTED){
                return;
            }

            conThread = connectedThread;
        }

        conThread.write(buffer);
    }



    private class AcceptThread extends Thread {

        private BluetoothServerSocket serverSocket;

        @RequiresApi(api = Build.VERSION_CODES.S)
        public AcceptThread() {
            BluetoothServerSocket tempSeverSocket = null;
            try {
                checkPermission();
                tempSeverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
            } catch (IOException e) {
                Log.d("Accept: Constructor ", e.toString());
            }

            serverSocket = tempSeverSocket;
        }

        @RequiresApi(api = Build.VERSION_CODES.S)
        public void run() {
            BluetoothSocket socket = null;

            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                Log.d("Accept: Run ", e.toString());
                try {
                    serverSocket.close();
                } catch (IOException e1) {
                    Log.d("Accept: Close ", e1.toString());
                }
            }

            if (socket != null) {
                switch (state) {
                    case STATE_LISTEN:
                    case STATE_CONNECTING:
                        connected(socket.getRemoteDevice(),socket);
                        //connect(socket.getRemoteDevice());
                        break;
                    case STATE_NONE:
                    case STATE_CONNECTED:
                        try {
                            socket.close();
                        } catch (IOException e) {
                            Log.d("Accept: SocketClose ", e.toString());
                        }
                        break;
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.d("Accept: CloseSever ", e.toString());
            }
        }
    }

    private class ConnectThread extends Thread {

        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        @RequiresApi(api = Build.VERSION_CODES.S)
        public ConnectThread(BluetoothDevice device) {
            this.device = device;

            BluetoothSocket tmpSocket = null;

            try {
                checkPermission();
                tmpSocket = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.d("ChatUtils: Constructor ", e.toString());
            }

            socket = tmpSocket;
        }

        @RequiresApi(api = Build.VERSION_CODES.S)
        public void run() {
            try {
                checkPermission();
                socket.connect();
            } catch (IOException e) {
                Log.d("ChatUtils: Run ", e.toString());
                try {
                    socket.close();
                } catch (IOException e1) {
                    Log.d("ChatUtils: SocketClose ", e.toString());
                }

                connectionFailed();
                return;
            }

            synchronized (ChatUtils.this) {
                connectThread = null;
            }

            connected(device,socket);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.d("ChatUtils: Cancel ", e.toString());
            }
        }

    }

    private class ConnectedThread extends  Thread{

        private final  BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket){

            this.socket = socket;

            InputStream tempInputStream = null;
            OutputStream tempOutputStream = null;

            try{
                tempInputStream = socket.getInputStream();
                tempOutputStream = socket.getOutputStream();
            }catch (IOException e){
                Log.d("Connected: Constructor ", e.toString());
            }

            inputStream = tempInputStream;
            outputStream = tempOutputStream;
        }

        @RequiresApi(api = Build.VERSION_CODES.S)
        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            try{
                bytes = inputStream.read(buffer);

                handler.obtainMessage(MainActivity.MESSAGE_READ,bytes,-1,buffer).sendToTarget();

            }catch (IOException e){
                Log.d("Connected: runFailed ", e.toString());
                connectionLost();
            }
        }

        public  void write(byte[] buffer){
            try{
                outputStream.write(buffer);
                handler.obtainMessage(MainActivity.MESSAGE_WRITE,-1,-1,buffer).sendToTarget();
            }
            catch (IOException e){
                Log.d("Connected: writeFailed ", e.toString());
            }
        }

        public  void cancel(){
            try{
                socket.close();
            }
            catch (IOException e){
                Log.d("Connected: cancelFailed", e.toString());
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void connectionLost() {

        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Connection Lost");
        message.setData(bundle);
        handler.sendMessage(message);

        ChatUtils.this.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private synchronized void connectionFailed() {

        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Can not connect to the device");
        message.setData(bundle);
        handler.sendMessage(message);

        ChatUtils.this.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private synchronized void connected(BluetoothDevice device, BluetoothSocket socket) {

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        Message message = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        checkPermission();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setState(STATE_CONNECTED);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void checkPermission() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 101);
        }
    }
}

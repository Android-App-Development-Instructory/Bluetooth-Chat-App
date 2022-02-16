package com.alaminkarno.bluetoothchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;

    private final int SELECTED_DEVICE = 101;

    public static final int MESSAGE_STATE_CHANGED = 0;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;

    public static final String DEVICE_NAME = "device name";
    public static final String TOAST = "toast";

    private String connectedDevice;
    private ChatUtils chatUtils;

    private ListView listViewChat;
    private EditText smsEditText;
    private Button sendMessageBTN;
    private ArrayAdapter<String> arrayAdapterChat;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGED:
                    switch (msg.arg1) {
                        case ChatUtils.STATE_NONE:
                            setState("Not Connected");
                            break;
                        case ChatUtils.STATE_LISTEN:
                            setState("Not Connected");
                            break;
                        case ChatUtils.STATE_CONNECTING:
                            setState("Connecting...");
                            break;
                        case ChatUtils.STATE_CONNECTED:
                            setState("Connected: " + connectedDevice);
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    byte[] buffer = (byte[]) msg.obj;
                    String inputBuffer = new String(buffer,0,msg.arg1);
                    arrayAdapterChat.add(connectedDevice+": "+inputBuffer);
                    break;
                case MESSAGE_WRITE:
                    byte[] buffer1 = (byte[]) msg.obj;
                    String outputBuffer = new String(buffer1,0,msg.arg1);
                    arrayAdapterChat.add("Me: "+outputBuffer);
                    break;
                case MESSAGE_DEVICE_NAME:
                    connectedDevice = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), connectedDevice, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    private void setState(CharSequence subTitle) {
        getSupportActionBar().setSubtitle(subTitle);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialize();

        initializeBluetooth();

        chatUtils = new ChatUtils(this, handler);
    }

    private void initialize() {

        listViewChat = findViewById(R.id.listViewMessage);
        smsEditText = findViewById(R.id.smsEditText);
        sendMessageBTN = findViewById(R.id.sendMessageBTN);

        arrayAdapterChat = new ArrayAdapter<String>(this, R.layout.message_layout);
        listViewChat.setAdapter(arrayAdapterChat);

        sendMessageBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = smsEditText.getText().toString();
                if (message.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter any message", Toast.LENGTH_SHORT).show();
                } else {
                    chatUtils.write(message.getBytes());
                    smsEditText.setText("");
                }
            }
        });
    }


    private void initializeBluetooth() {

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "No bluetooth found...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search_bluetooth:
                getPermission();
                return true;
            case R.id.menu_enable_bluetooth:
                enableBluetooth();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void getPermission() {

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT};

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {

            ActivityCompat.requestPermissions(this, permissions, 101);
        } else {
            Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
            startActivityForResult(intent, SELECTED_DEVICE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == SELECTED_DEVICE && resultCode == RESULT_OK) {
            String address = data.getStringExtra("deviceAddress");
            Toast.makeText(getApplicationContext(), "" + address, Toast.LENGTH_SHORT).show();
            chatUtils.connect(bluetoothAdapter.getRemoteDevice(address));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(intent, SELECTED_DEVICE);
            } /*else {
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle("Warning!!!")
                        .setMessage("Location permission is required...")
                        .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                            @RequiresApi(api = Build.VERSION_CODES.S)
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                getPermission();
                            }
                        })
                        .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .show();
            }*/
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.S)
    private void enableBluetooth() {

        if (!bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                getPermission();
            }
            bluetoothAdapter.enable();
            Toast.makeText(getApplicationContext(), "Bluetooth turn on.", Toast.LENGTH_SHORT).show();
        }

        if(bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
            startActivity(discoverIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(chatUtils != null){
            chatUtils.stop();
        }
    }
}
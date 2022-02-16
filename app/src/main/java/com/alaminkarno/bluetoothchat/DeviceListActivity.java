package com.alaminkarno.bluetoothchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    private ListView pairedDeviceList, availableDeviceList;
    private ProgressBar progressBar;

    private ArrayAdapter<String> adapterPairedDevice, adapterAvailableDevice;

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        initialize();
    }

    private void initialize() {

        pairedDeviceList = findViewById(R.id.pairedDeviceList);
        availableDeviceList = findViewById(R.id.availableDeviceList);
        progressBar = findViewById(R.id.progressBar);

        adapterPairedDevice = new ArrayAdapter<String>(this, R.layout.device_list);
        adapterAvailableDevice = new ArrayAdapter<String>(this, R.layout.device_list);

        pairedDeviceList.setAdapter(adapterPairedDevice);
        availableDeviceList.setAdapter(adapterAvailableDevice);


        availableDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                Intent intent = new Intent();
                intent.putExtra("deviceAddress", address);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

       /* pairedDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                Intent intent = new Intent();
                intent.putExtra("deviceAddress", address);
                setResult(RESULT_OK, intent);
                finish();
            }
        });*/

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        checkPermission();

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices != null && pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                adapterPairedDevice.add(device.getName() + "\n" + device.getAddress());
            }
        }

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothBroadcastReceiver, intentFilter);
        IntentFilter discoveryIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothBroadcastReceiver, discoveryIntentFilter);

        pairedDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                checkPermission();
                bluetoothAdapter.cancelDiscovery();

                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                Log.d("Address", address);

                Intent intent = new Intent();
                intent.putExtra("deviceAddress", address);

                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }

    private void checkPermission() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DeviceListActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 101);
        }
    }

    private BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

               checkPermission();

                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    adapterAvailableDevice.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                progressBar.setVisibility(View.GONE);
                if (adapterAvailableDevice.getCount() == 0) {
                    Toast.makeText(getApplicationContext(), "No new device found.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Click on device to start chatting...", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bluetooth_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search_bluetooth:
                scanBluetoothDevice();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void scanBluetoothDevice() {
        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(getApplicationContext(), "Scan started...", Toast.LENGTH_SHORT).show();

        adapterAvailableDevice.clear();

        checkPermission();

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        bluetoothAdapter.startDiscovery();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bluetoothBroadcastReceiver != null) {
            unregisterReceiver(bluetoothBroadcastReceiver);
        }
    }
}
package com.alaminkarno.bluetoothchat;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    private ListView pairedDeviceList,availableDeviceList;

    private ArrayAdapter<String> adapterPairedDevice,adapterAvailableDevice;

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

        adapterPairedDevice = new ArrayAdapter<String>(this,R.layout.device_list);
        adapterAvailableDevice = new ArrayAdapter<String>(this,R.layout.device_list);

        pairedDeviceList.setAdapter(adapterPairedDevice);
        availableDeviceList.setAdapter(adapterAvailableDevice);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if(pairedDevices != null && pairedDevices.size() > 0){
            for(BluetoothDevice device: pairedDevices){
                adapterPairedDevice.add(device.getName() + "\n"+ device.getAddress());
            }
        }
    }
}
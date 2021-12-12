package com.alaminkarno.bluetoothchat;

import androidx.annotation.NonNull;
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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeBluetooth();
    }


    private void initializeBluetooth() {

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            Toast.makeText(getApplicationContext(), "No bluetooth found...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
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

    private void getPermission() {

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){

            ActivityCompat.requestPermissions(this,permissions,101);
        }
        else{
            startActivity(new Intent(MainActivity.this,DeviceListActivity.class));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == 101){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                startActivity(new Intent(MainActivity.this,DeviceListActivity.class));
            }
            else{
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle("Warning!!!")
                        .setMessage("Location permission is required...")
                        .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
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
            }
        }
        else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void enableBluetooth() {
        if(!bluetoothAdapter.isEnabled()){
            bluetoothAdapter.enable();
            Toast.makeText(getApplicationContext(), "Bluetooth turn on.", Toast.LENGTH_SHORT).show();
        }

        if(bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
            startActivity(discoverIntent);
        }
    }
}
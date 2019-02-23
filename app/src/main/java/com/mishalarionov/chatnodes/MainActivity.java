package com.mishalarionov.chatnodes;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView textBoi;
    private BluetoothAdapter bluetoothAdapter;
    final int REQUEST_ENABLE_BT = 1;
    final int REQUEST_LOCATION = 2;
    private BluetoothManager bluetoothManager;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private UUID serviceUUID;
    private BluetoothGattServer bluetoothGattServer;
    private HashMap<String, BluetoothDevice> results;

    private Button scanButton;
    private Button broadcastButton;

    private BluetoothGatt connectedGatt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textBoi = findViewById(R.id.textBoi);
        scanButton = findViewById(R.id.scan_button);
        broadcastButton = findViewById(R.id.broadcast_button);

        scanButton.setOnClickListener(this);
        broadcastButton.setOnClickListener(this);

        //Initialize the bluetooth adapter
        bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        bluetoothAdapter = bluetoothManager.getAdapter();

        //Enable bluetooth if it isn't already
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

        }

        //Check to see if we have ACCESS_FINE_LOCATION permission
        //We need this because BLE requires it (ACCESS_COARSE_LOCATION would probably also work)
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }

        //Initialize the UUID so we can have it consistent every time
        serviceUUID = UUID.nameUUIDFromBytes("yeetBoiChat".getBytes());

        //Initialize the BLE scanner
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        //Initialize the advertiser
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == -1) {
                Toast.makeText(getApplicationContext(), R.string.bluetooth_enable_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.bluetooth_enable_fail, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void connectDevice(BluetoothDevice device) {
        BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (status == BluetoothGatt.GATT_FAILURE) {
                    System.out.println("Gatt Failed");
                    if (connectedGatt != null) {
                        connectedGatt.disconnect();
                        connectedGatt.close();
                    }
                } else if (status != BluetoothGatt.GATT_SUCCESS) {
                    System.out.println("Gatt Failed");
                }

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    System.out.println("Gatt Success");
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    System.out.println("Gatt Disconnected");
                    if (connectedGatt != null) {
                        connectedGatt.disconnect();
                        connectedGatt.close();
                    }
                }
            }
        };

        connectedGatt = device.connectGatt(this, false, gattCallback);
    }

    private void scanBluetooth() {
        //try {
            results = new HashMap<String, BluetoothDevice>();
            bluetoothLeScanner.startScan(new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    BluetoothDevice bluetoothDevice = result.getDevice();
                    //Get the scan result, eventually getting service UUID
                    if (bluetoothDevice != null) {
                        //System.out.println("New scan result: " + bluetoothDevice.getAddress());

                        if (result.getScanRecord() != null) {
                            List<ParcelUuid> resultServiceUUIDs = result.getScanRecord().getServiceUuids();
                            //Check to see if the service UUID matches ours todo rewrite with ScanFilter
                            if (resultServiceUUIDs != null  && resultServiceUUIDs.get(0).equals(new ParcelUuid(serviceUUID))) {
                                addScanResult(result);

                                bluetoothLeScanner.stopScan(new ScanCallback() {});

                                connectDevice(bluetoothDevice);
//                              textBoi.setText(results.toString());
                            }
                        }
                    }
                }

                private void addScanResult(ScanResult result) {
                    BluetoothDevice device = result.getDevice();
                    String deviceAddress = device.getAddress();
                    results.put(deviceAddress, device);
                }
            });
            Toast.makeText(getApplicationContext(), R.string.scan_start, Toast.LENGTH_SHORT).show();
        //} catch (NullPointerException e) {
            //Toast.makeText(getApplicationContext(), R.string.scan_fail, Toast.LENGTH_SHORT).show();
        //}
    }

    private void broadcastBluetooth() {
        //Advertising parameters
        //Minimum API required is 26 (DO NOT CHANGE PROJECT TO BE LOWER THAN THIS)

        BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                super.onConnectionStateChange(device, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    System.out.println("Device connected");
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    System.out.println("Device disconnected");
                }
            }
        };

        //Initialize a bunch of garbage
        bluetoothGattServer = bluetoothManager.openGattServer(this, gattServerCallback);

        BluetoothGattService bluetoothGattService = new BluetoothGattService(serviceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        bluetoothGattServer.addService(bluetoothGattService);

        //Make the settings
        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM) //Todo: Change to max to gain more range
                .build();

        ParcelUuid parcelUuid = new ParcelUuid(serviceUUID);

        int maxLength = bluetoothAdapter.getLeMaximumAdvertisingDataLength();

        System.out.println("Max data length for device: " + Integer.toString(maxLength));

        //Create the data
        //Due to the limit on data for some devices, all we transmit is UUID
        //We'll send more data once we start a GATT server
        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceUuid(parcelUuid)
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .build();

        AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Toast.makeText(getApplicationContext(), R.string.advertise_start, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartFailure(int errorCode) {
                Toast.makeText(getApplicationContext(), R.string.advertise_fail, Toast.LENGTH_SHORT).show();
                System.out.println("Start advertise failed. Error code: " + Integer.toString(errorCode));
            }

        };

        bluetoothLeAdvertiser.startAdvertising(
                advertiseSettings,
                data,
                advertiseCallback
        );
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.scan_button:
                scanButton.setEnabled(false);
                System.out.println("Scan button pressed");
                scanBluetooth();
                break;
            case R.id.broadcast_button:
                broadcastButton.setEnabled(false);
                System.out.println("Broadcast button pressed");
                broadcastBluetooth();
                break;
        }
    }
}

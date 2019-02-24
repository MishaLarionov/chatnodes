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
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //Declare a bunch of crap
    private TextView textBoi;
    private BluetoothAdapter bluetoothAdapter;
    final int REQUEST_ENABLE_BT = 1;
    final int REQUEST_LOCATION = 2;
    private BluetoothManager bluetoothManager;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private UUID serviceUUID;
    private UUID charUUID;
    private BluetoothGattServer bluetoothGattServer;
    private HashMap<String, BluetoothDevice> results;

    private Button scanButton;
    private Button broadcastButton;
    private Button sendButton;
    private EditText sendText;

    private Handler handler;

    private ArrayList<BluetoothGatt> connectedGatts;

    private List<BluetoothDevice> connectedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fragment manager
        final FragmentManager fragmentManager = getSupportFragmentManager();

        // define your fragments here
        //final Fragment fragment1 = new FirstFragment();
        //final Fragment fragment2 = new SecondFragment();
        //final Fragment fragment3 = new ThirdFragment();

        // Text and buttons
        textBoi = findViewById(R.id.textBoi);
        scanButton = findViewById(R.id.scan_button);
        broadcastButton = findViewById(R.id.broadcast_button);
        sendButton = findViewById(R.id.sendyButton);
        sendText = findViewById(R.id.messageSendyBoi);

        scanButton.setOnClickListener(this);
        broadcastButton.setOnClickListener(this);
        sendButton.setOnClickListener(this);

        connectedGatts = new ArrayList<>();

        handler = new Handler();

        //Bottom navigation bar
        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment;
                switch (item.getItemId()) {
                    case R.id.private_message:
                        //fragment = fragment1;
                        return true;
                    case R.id.home:
                        //fragment = fragment2;
                        return true;
                    case R.id.group_message:
                        //fragment = fragment3;
                        return true;
                }
                //fragmentManager.beginTransaction().replace(R.id.flContainer, fragment).commit();
                return true;
            }
        });
        // Set default selection
        bottomNavigationView.setSelectedItemId(R.id.home);

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
        charUUID = UUID.nameUUIDFromBytes("writeyBoi".getBytes());

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

    private BluetoothGatt connectDevice(BluetoothDevice device) {

        BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (status == BluetoothGatt.GATT_FAILURE) {
                    System.out.println("Gatt Failed");
                    if (gatt != null) {
                        connectedGatts.remove(gatt);
                        gatt.disconnect();
                        gatt.close();
                    }
                } else if (status != BluetoothGatt.GATT_SUCCESS) {
                    System.out.println("Gatt Failed");
                }

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    System.out.println("Gatt Success");

                    //Possible null pointer error but I'll let it happen for debug reasons
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    System.out.println("Gatt Disconnected");
                    if (gatt != null) {
                        connectedGatts.remove(gatt);
                        gatt.disconnect();
                        gatt.close();
                    }
                }
                int[] acceptedStates = {BluetoothProfile.STATE_CONNECTED};
                connectedDevices = bluetoothManager.getDevicesMatchingConnectionStates(BluetoothProfile.GATT, acceptedStates);
                System.out.println("Connected Devices:");
                System.out.println(connectedDevices);


            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    System.out.println("Couldn't discover services");
                    return;
                }
                BluetoothGattService service = gatt.getService(serviceUUID);
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUUID);

                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                gatt.setCharacteristicNotification(characteristic, true);

            }


            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                byte[] messageBytes = characteristic.getValue();
                String messageString = new String(messageBytes, StandardCharsets.UTF_8);

                System.out.println("Got message: " + messageString);
            }
        };

        return device.connectGatt(this, false, gattCallback);

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

                                boolean connectionExists = false;

                                //Prevent duplicate devices
                                for (BluetoothGatt gatt : connectedGatts) {
                                    if (gatt.getDevice().equals(bluetoothDevice)) {
                                        connectionExists = true;
                                        gatt.connect();
                                    }
                                }

                                if (!connectionExists) {
                                    connectedGatts.add(connectDevice(bluetoothDevice));
                                }

                                System.out.println("Connected gatts: " + Integer.toString(connectedGatts.size()));

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
                int[] acceptedStates = {BluetoothProfile.STATE_CONNECTED};
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    System.out.println("Device connected");
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    System.out.println("Device disconnected");
                }
                connectedDevices = bluetoothManager.getDevicesMatchingConnectionStates(BluetoothProfile.GATT, acceptedStates);
                System.out.println("Connected Devices:");
                System.out.println(connectedDevices);
            }

            //Characteristic is something about writing data
            @Override
            public void onCharacteristicWriteRequest(
                    BluetoothDevice device,
                    int requestId,
                    final BluetoothGattCharacteristic characteristic,
                    boolean preparedWrite,
                    boolean responseNeeded,
                    final int offset,
                    final byte[] value
            ) {
                super.onCharacteristicWriteRequest(
                        device, requestId, characteristic, preparedWrite, responseNeeded, offset, value
                );
                if (characteristic.getUuid().equals(charUUID)) {

                    final String message = new String(value, StandardCharsets.UTF_8);

                    System.out.println("WE GOT A MESSAGE AAA");
                    System.out.println(message);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //Handle messages here
                            textBoi.setText(message);
                        }
                    });
                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
//                    byte[] yeet = "goteem".getBytes();
//                    characteristic.setValue(yeet);
//                    for (BluetoothDevice bDevice : connectedDevices) {
//                        bluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
//                    }
                }
            }

        };

        //Initialize a bunch of garbage
        bluetoothGattServer = bluetoothManager.openGattServer(this, gattServerCallback);

        BluetoothGattService service = new BluetoothGattService(serviceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic writeCharacteristic = new BluetoothGattCharacteristic(
                charUUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
        );

        service.addCharacteristic(writeCharacteristic);

        bluetoothGattServer.addService(service);

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

    private void sendMessage(String message) {
        for (BluetoothGatt connectedGatt : connectedGatts) {
            System.out.println("Services gotten: ");
            System.out.println(connectedGatt.getServices());
            System.out.println(serviceUUID.toString());
            BluetoothGattService service = connectedGatt.getService(serviceUUID);
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUUID);
                System.out.println("Sending message: " + message);
                byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
                characteristic.setValue(messageBytes);

                boolean success = connectedGatt.writeCharacteristic(characteristic);

                System.out.println("Success?" + Boolean.toString(success));
            } else {
                System.out.println("Service is null!!!!");
            }
        }
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
            case R.id.sendyButton:
                System.out.println("Send button pressed");
                sendMessage(sendText.getText().toString());
                sendText.setText("");
                break;
        }
    }
}

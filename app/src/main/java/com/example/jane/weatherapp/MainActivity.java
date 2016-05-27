package com.example.jane.weatherapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.*;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;


import java.util.UUID;
import java.util.logging.LogRecord;

public class MainActivity extends AppCompatActivity {

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice btDevice;
    private BluetoothLeScanner mScanner;
    private BluetoothGatt mGatt;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 100000;

    private boolean flag = true;

    private FloatingActionButton fab;
    private TextView humidity,temperature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        fab = (FloatingActionButton) findViewById(R.id.scan);

        temperature = (TextView) findViewById(R.id.temp);
        humidity = (TextView) findViewById(R.id.hum);
        setSupportActionBar(toolbar);

        mHandler = new Handler();
        // Check if BLE is supported
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        //Get the bluetooth manager
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //Get the bluetooth manager
        mBluetoothAdapter = bluetoothManager.getAdapter();

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Check if bluetooth is enabled and prompt user to enable if not
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else {
            mScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<ScanFilter>();
            //add device name as a filter
            ScanFilter filter = new ScanFilter.Builder().setDeviceName("IPVSWeather").build();
            filters.add(filter);

            //Start scanning when button in clicked
            /*fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scanLeDevice(true);
                }
            });*/
            scanLeDevice(true);
        }
    }

    protected void onPause() {
        super.onPause();
        if(mBluetoothAdapter != null && mBluetoothAdapter.isEnabled())
            scanLeDevice(false);
    }

    protected void onDestroy() {
        super.onDestroy();
        if(mGatt == null)
            return;
        mGatt.close();
        mGatt = null;
    }

    private void scanLeDevice(final boolean enabled) {
        if(enabled) {
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    mScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);
            mScanner.startScan(filters, settings, mScanCallback);
        }
            else {
            mScanner.stopScan(mScanCallback);
        }
    }


    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //super.onScanResult(callbackType, result);
            btDevice = result.getDevice();
            if(result!= null) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Connect");
                alertDialog.setMessage("Connect to " + btDevice.getName() + "?");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //dialog.dismiss();
                                //connectBtDevice(btDevice);
                            }
                        });
                /*if(flag) {
                    alertDialog.show();
                    flag = false;
                }*/
            }

            Log.i("result", result.toString());

            connectBtDevice(btDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    public void connectBtDevice(BluetoothDevice device) {
        if(mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            if(mGatt != null) {
                Toast.makeText(getApplicationContext(), "Connected to GATT server", Toast.LENGTH_SHORT).show();
                scanLeDevice(false); //stop scanning after device found
            }
        }
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    gatt.discoverServices();
                    Log.i("gattCallback","Device Connected");
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    //Toast.makeText(getApplicationContext(), "Disconnected!", Toast.LENGTH_SHORT).show();
                    Log.e("gattCallback","Device disconnected");
                    break;
                default:
                    Log.e("gattCallback","STATUS_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            List<BluetoothGattService> services = gatt.getServices();
            //Log.i("no. of services", "" + services.size());
            Log.i("Characteristics", services.get(2).getCharacteristics().toString()); //3rd service is the Weather service.
            gatt.readCharacteristic(services.get(2).getCharacteristics().get(0));   //Temperature
            gatt.readCharacteristic(services.get(2).getCharacteristics().get(1)); //Humidity

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            List<BluetoothGattDescriptor> desc = new ArrayList<BluetoothGattDescriptor>();

            //Temperature
            if (characteristic.getUuid().compareTo(UUID.fromString("00002a1c-0000-1000-8000-00805f9b34fb")) == 0) {
                float f = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1);
                updateTemperature(f);
                //Enabling notification
                desc = characteristic.getDescriptors();
                BluetoothGattDescriptor descriptor = desc.get(0);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }

            //Humidity
            if(characteristic.getUuid().compareTo(UUID.fromString("00002a6f-0000-1000-8000-00805f9b34fb")) == 0) {
                int val = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                updateHumidity(val);
                //Enabling notification
                desc = characteristic.getDescriptors();
                BluetoothGattDescriptor descriptor = desc.get(0);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            //Humidity
            if(characteristic.getUuid().compareTo(UUID.fromString("00002a6f-0000-1000-8000-00805f9b34fb")) == 0) {
                int val = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,-2);
                updateHumidity(val);
            }

            //Temperature
            if (characteristic.getUuid().compareTo(UUID.fromString("00002a1c-0000-1000-8000-00805f9b34fb")) == 0) {
                float f = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1);
                updateTemperature(f);

            }

        }
    };


    public void updateHumidity(final int value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                float f = value / 100;
                Log.i("Humidity",String.valueOf(f));
                humidity.setText(String.valueOf(f));
            }
        });
    }

    public void updateTemperature(final float value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i("Temperature",String.valueOf(value));
                temperature.setText(String.valueOf(value));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

package com.example.rotac.appsmarthealth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.service.FitnessSensorService;
import com.google.android.gms.fitness.service.FitnessSensorServiceRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class MySensorService extends FitnessSensorService {
    private static final String TAG = "Smart_Health_Sensor";
    private DataSource mDataSource = null;
    private FitnessSensorServiceRequest mRequest = null;
    private StringBuilder sb = new StringBuilder();

//    TODO Should be removed
//    private Timer mTimer = null;

    // Broadcast
    private BroadcastReceiver receiver;
    private IntentFilter filter;

    // Bluetooth
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothChatService mChatService = null;

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            Log.d(TAG, "bluetooth connected.");
                            break;

                        case BluetoothChatService.STATE_CONNECTING:
                            Log.d(TAG, "bluetooth connecting...");
                            break;

                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            Log.d(TAG, "bluetooth not connected.");
                            break;

                    }
                    break;

                case Constants.MESSAGE_WRITE:
                    Log.d(TAG, "bluetooth writing.");
                    break;

                case Constants.MESSAGE_READ:
                    byte[] b = (byte[]) msg.obj;
                    String s = new String(b, 0, msg.arg1);

                    for(int i=0; i<s.length(); i++){
                        char c = s.charAt(i);

                        if(c == '\n'){
                            continue;
                        }

                        sb.append(c);

                        if(c == '>'){
                            String str = sb.toString();
                            Log.d(TAG, "bluetooth reading. : " + str );

                            if(str.length() < 6){
                                continue;
                            }

                            switch(str.charAt(1)){
                                case 'C':
                                    break;

                                case 'M':
                                    Integer current_weight = Integer.parseInt( str.substring(2,6).trim() );
                                    emitDataPoints(current_weight / 10.0f);
                                    break;

                                default:
                                    Log.e(TAG, "bluetooth reading. : cmd invalid!");
                            }
                            sb = new StringBuilder();
                        }
                    }
                    break;

                case Constants.MESSAGE_DEVICE_NAME:
                    Log.d(TAG, "bluetooth device name.");
                    break;
                case Constants.MESSAGE_TOAST:
                    Log.d(TAG, "bluetooth toast.");
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Create");
        // 1. Initialize your software sensor(s).
        // 2. Create DataSource representations of your software sensor(s).
        mDataSource = new DataSource.Builder()
                .setAppPackageName(this.getPackageName())
                .setDataType(DataType.TYPE_WEIGHT)
                .setDevice(new Device("manufacturer", "model", "uid", Device.TYPE_SCALE))
                .setName("my_sensor_name")
                .setStreamName("my_sensor_stream_name")
                .setType(DataSource.TYPE_RAW)
                .build();
        // 3. Initialize some data structure to keep track of a registration for each sensor.

        // ----------- broadcast receiver -------------
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "connectBluetooth on MySensorService");
                emitDataPoints(20.0f);

                if (mChatService != null) {
                    // Get a set of currently paired devices
                    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

                    // If there are paired devices, add each one to the ArrayAdapter
                    if (pairedDevices.size() > 0) {
                        for (BluetoothDevice device : pairedDevices) {
                            Log.d(TAG, device.getName());
                            if (device.getName().equals("HC-06") ) {
                                mChatService.connect(device, true);
                                Log.d(TAG, "connect bluetooth device.");
                            }
                        }
                    } else {
                        Log.d(TAG, "no bluetooth devices.");
                    }

                } else {
                    Log.d(TAG, "mChatService not ready.");
                }
            }
        };
        filter = new IntentFilter("CONNECT_BT_ACTION");
        registerReceiver(receiver, filter);

        // ----------- bluetooth -------------
        // Create
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // onStart
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth is not supported!");
            onDestroy();
        } else if (!mBluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled!");
        } else if (mChatService == null) {
            mChatService = new BluetoothChatService(mHandler);
        }
        // Resume
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
        if (mChatService != null) {
            mChatService.stop();
        }
        Log.d(TAG, "MySensorService destroyed");
    }

    @Override
    public List<DataSource> onFindDataSources(List<DataType> dataTypes) {
        Log.d(TAG, "onFindDataSources");
        // 1. Find which of your software sensors provide the data types requested.
        List<DataSource> filteredDataSourceList = new ArrayList<>();
        boolean hasDataType = false;
        for (DataType mDataType : dataTypes){
            if (mDataType.equals(mDataSource.getDataType())){
                hasDataType = true;
            }
        }
        if (hasDataType){
            filteredDataSourceList.add(mDataSource);
        }
        // 2. Return those as a list of DataSource objects.
        return filteredDataSourceList;
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Log.e(TAG, "cannot send message because of no connection!");
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            Log.d(TAG, "send message to bluetooth");
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }

    private void emitDataPoints(float value) {
        if (mRequest != null){
            Log.d(TAG, "emitDataPoints");
            DataPoint mDataPoint = DataPoint.create(mDataSource);
            mDataPoint.setTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            mDataPoint.getValue(Field.FIELD_WEIGHT).setFloat(value);
            List<DataPoint> mDataPointList = new ArrayList<>();
            mDataPointList.add(mDataPoint);
            try {
                mRequest.getDispatcher().publish(mDataPointList);
            } catch (android.os.RemoteException e){
                Log.e(TAG, "RemoteException");
            }
        }else{
            Log.e(TAG, "mRequest is null!");
        }
    }

    @Override
    public boolean onRegister(FitnessSensorServiceRequest request) {
        Log.d(TAG, "onRegister");
        // 1. Determine which sensor to register with request.getDataSource().
        // 2. If a registration for this sensor already exists, replace it with this one.
        // 3. Keep (or update) a reference to the request object.
        if (
                request.getDataSource().getDevice().equals(mDataSource.getDevice()) &&
                        request.getDataSource().getStreamName().equals(mDataSource.getStreamName())
                ) {
            Log.d(TAG, "Registering Success");
            mRequest = request;

//            TODO Should be removed
//            mTimer = new Timer();
//            mTimer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    Log.d(TAG, "TimerExecute");
//                    emitDataPoints(10.0f);
//                }
//            }, 10000, 5000);

            return true;
        }
        // 4. Configure your sensor according to the request parameters.
        // 5. When the sensor has new data, deliver it to the platform by calling
        //    request.getDispatcher().publish(List<DataPoint> dataPoints)
        Log.e(TAG, "Registering Failed");
        return false;
    }

    @Override
    public boolean onUnregister(DataSource dataSource) {
        Log.d(TAG, "onUnregister");
        // 1. Configure this sensor to stop delivering data to the platform
        // 2. Discard the reference to the registration request object
        if (dataSource.equals(mDataSource)) {
            mRequest = null;
//            TODO Should be removed
//            mTimer.cancel();
            return true;
        }

        return false;
    }

}

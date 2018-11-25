package com.example.rotac.appsmarthealth;

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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


public class MySensorService extends FitnessSensorService {
    private static final String TAG = "Smart_Health_Sensor";
    private DataSource mDataSource = null;
    private FitnessSensorServiceRequest mRequest = null;

    private Timer mTimer = null;

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

    private void emitDataPoints() {
        if (mRequest != null){
            Log.d(TAG, "emitDataPoints");
            DataPoint mDataPoint = DataPoint.create(mDataSource);
            mDataPoint.setTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            mDataPoint.getValue(Field.FIELD_WEIGHT).setFloat(10.0f);
            List<DataPoint> mDataPointList = new ArrayList<>();
            mDataPointList.add(mDataPoint);
            try {
                mRequest.getDispatcher().publish(mDataPointList);
            } catch (android.os.RemoteException e){
                Log.d(TAG, "RemoteException");
            }
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
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.d(TAG, "TimerExecute");
                    emitDataPoints();
                }
            }, 10000, 5000);

            return true;
        }
        // 4. Configure your sensor according to the request parameters.
        // 5. When the sensor has new data, deliver it to the platform by calling
        //    request.getDispatcher().publish(List<DataPoint> dataPoints)
        Log.d(TAG, "Registering Failed");
        return false;
    }

    @Override
    public boolean onUnregister(DataSource dataSource) {
        Log.d(TAG, "onUnregister");
        // 1. Configure this sensor to stop delivering data to the platform
        // 2. Discard the reference to the registration request object
        if (dataSource.equals(mDataSource)) {
            mRequest = null;
            mTimer.cancel();
            return true;
        }

        return false;
    }

}

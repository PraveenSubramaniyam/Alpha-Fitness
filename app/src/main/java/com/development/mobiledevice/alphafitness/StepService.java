
package com.development.mobiledevice.alphafitness;


import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class StepService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
	private static final String TAG = "Pedometer";
    private SharedPreferences mSettings;
    private PedometerSettings mPedometerSettings;
    private SharedPreferences.Editor mStateEditor;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private StepDetector mStepDetector;
    private StepDisplayer mStepDisplayer;
    LatLongArrayClass LatLongPoints;
    private double prevLatitude;
    private double prevLongitude;
    private long workStartTime;
    private int lastStepsCounts = 0;
    private ArrayList<Integer> stepCountArray;
    private Timer timer;
    long loop =0 ;


    
    private PowerManager.WakeLock wakeLock;
    private int mSteps = 0 ;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (location == null) {
            // Blank for a moment...
            Log.i(TAG,"Location Null");
           // LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else {
            handleNewLocation(location);
        };

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        //Toast.makeText(this, "onLocation Changed" , Toast.LENGTH_LONG).show();
        Log.i(TAG, "On Location Changed Called");

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
/*
        if(LatLongPoints == null) {
            LatLongPoints = new LatLongArrayClass();
            LatLongPoints.addLatLong(latitude, longitude);

        }
        else
        {
            LatLongClass point = LatLongPoints.getLastLatLongClass();
            if (loop % 2 == 0) {
                LatLongPoints.addLatLong((point.getLatitude()+0.002),point.getLongitude());
            } else {
                // odd
                LatLongPoints.addLatLong(point.getLatitude(),(point.getLongitude()+0.002));
            }
        }
        loop++;
        if (mCallback != null) {
            mCallback.locationChanged(LatLongPoints);
        }
*/
        //counter += 0.002; //todo: remove later

        if(latitude != prevLatitude || longitude != prevLongitude || LatLongPoints == null)
        {
            prevLatitude = latitude;
            prevLongitude = longitude;
            if(LatLongPoints == null)
                LatLongPoints = new LatLongArrayClass();
            LatLongPoints.addLatLong(latitude, longitude);

            if (mCallback != null) {
                mCallback.locationChanged(LatLongPoints);
            }
            Log.i(TAG,"Change in Location");
            //Toast.makeText(this, "Change in Location" , Toast.LENGTH_LONG).show();
            handleNewLocation(location);
        }
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class StepBinder extends Binder {
        StepService getService() {
            return StepService.this;
        }
    }
    
    @Override
    public void onCreate() {
        Log.i(TAG, "[STEP SERVICE] onCreate");
        super.onCreate();

        stepCountArray = new ArrayList<Integer>();

        // Load settings
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mPedometerSettings = new PedometerSettings(mSettings);

        //db = new DataBaseHelper(getApplicationContext());
        ContentValues values = new ContentValues();
        values.put(StepCounterTable.C_STARTTIME, getDateTime());
        values.put(StepCounterTable.C_COUNT, 0);

        //long id = db.insertStepCounterTable();
        Uri uri = getContentResolver().insert(PedometerContentProvider.STEPTABLE_CONTENT_URI, values);
        Log.i(TAG, "[STEPSERVICE.JAVA] Registered sensor.. Created entry in step Counter table id: "+uri.toString());
        mPedometerSettings.saveStepCountTableId(uri.toString());
        /*
        Estimate by Height
        These are rough estimates, but useful to check your results by the other methods. It is the method used in the automatic settings of many pedometers and activity trackers:
        Females: Your height x .413 equals your stride length
        Males: Your height x .415 equals your stride length
         */
        //mState = getSharedPreferences("state", 0);
        acquireWakeLock();

        // Start detecting
        mStepDetector = new StepDetector();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        registerDetector();

        // Register our receiver for the ACTION_SCREEN_OFF action. This will make our receiver
        // code be called whenever the phone enters standby mode.
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, filter);

        mStepDisplayer = new StepDisplayer(mPedometerSettings);
        //mStepDisplayer.setSteps(mSteps = mState.getInt("steps", 0));
        mStepDisplayer.addListener(mStepListener);
        mStepDetector.addStepListener(mStepDisplayer);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

        mGoogleApiClient.connect();
        workStartTime = System.currentTimeMillis()/1000;
        startTimer();
    }
    
    @Override
    public void onStart(Intent intent, int startId) {
        Log.i(TAG, "[SERVICE] onStart");
        super.onStart(intent, startId);
    }

    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }
    @Override
    public void onDestroy() {
        Log.i(TAG, "[SERVICE] onDestroy");
        // Unregister our receiver.
        unregisterReceiver(mReceiver);
        unregisterDetector();
        wakeLock.release();

        super.onDestroy();
        
        // Stop detecting
        mSensorManager.unregisterListener(mStepDetector);

        String id = mPedometerSettings.getStepCountTableId();
        long totalSecs = (System.currentTimeMillis()/1000) - workStartTime;
        mPedometerSettings.clearStepCount();

        ContentValues values = new ContentValues();
        values.put(StepCounterTable.C_COUNT, mSteps);
        values.put(StepCounterTable.C_ENDTIME, getDateTime());
        values.put(StepCounterTable.C_TOTALTIME,totalSecs);

        Uri myUri = Uri.parse(id);
        Log.i(TAG,"StepCountTableId: "+myUri.getPathSegments().get(1));
        getContentResolver().update(PedometerContentProvider.STEPTABLE_CONTENT_URI,values,StepCounterTable.C_ID + " = " + myUri.getPathSegments().get(1),null);
        //db.endStepCounterValue(values, id);
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        Toast.makeText(this, getText(R.string.stopped), Toast.LENGTH_SHORT).show();
        timer.cancel();
        mPedometerSettings.clearServiceRunning();
    }

    private void registerDetector() {
        mSensor = mSensorManager.getDefaultSensor(
            Sensor.TYPE_STEP_DETECTOR);
        mSensorManager.registerListener(mStepDetector,
            mSensor,
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH);

    }

    private void unregisterDetector() {
        mSensorManager.unregisterListener(mStepDetector);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "[SERVICE] onBind");
        return mBinder;
    }

    /**
     * Receives messages from activity.
     */
    private final IBinder mBinder = new StepBinder();

    public interface ICallback {
        public void stepsChanged(double value);
        public void graphData(Object obj);
        public void locationChanged(Object obj);
    }
    
    private ICallback mCallback;

    public void registerCallback(ICallback cb) {
        mCallback = cb;
    }

    public void unregisterCallback() {
        mCallback = null;
    }


    public void resetValues() {
        mStepDisplayer.setSteps(0);

    }
    

    private StepDisplayer.Listener mStepListener = new StepDisplayer.Listener() {
        public void stepsChanged(int value) {
            mSteps = value;
            Log.i(TAG,"Steps: "+mSteps);
            mPedometerSettings.saveStepCount(mSteps);
            passValue();
        }
        public void passValue() {
            if (mCallback != null) {
                Log.i(TAG,"mcallback not null");
                mCallback.stepsChanged(mSteps*1.0);
            }
            else
                Log.i(TAG,"mcallback null");
        }
    };

    void sendDataEvery5mins()
    {
        int last5minsSteps = mSteps - lastStepsCounts;
        stepCountArray.add(last5minsSteps);
        lastStepsCounts += last5minsSteps;
        Log.i(TAG,"Total Step Counts: "+mSteps);
        Log.i(TAG,"Steps Last mins "+last5minsSteps);

        if(mCallback != null )
        {
            mCallback.graphData(stepCountArray);
            Log.i(TAG,"mcallback not null");
        }
        else
            Log.i(TAG,"mcallback null");
    }



    void startTimer()
    {
        int MINUTES = 1; // The delay in minutes
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() { // Function runs every MINUTES minutes.
                // Run the code you want here
                sendDataEvery5mins();
            }
        }, 0, 1000 * 60 * MINUTES);
    }



   // BroadcastReceiver for handling ACTION_SCREEN_OFF.
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Check action just to be on the safe side.
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                // Unregisters the listener and registers it again.
                StepService.this.unregisterDetector();
                StepService.this.registerDetector();
                if (mPedometerSettings.wakeAggressively()) {
                    wakeLock.release();
                    acquireWakeLock();
                }
            }
        }
    };

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        int wakeFlags;
        if (mPedometerSettings.wakeAggressively()) {
            wakeFlags = PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP;
        }
        else if (mPedometerSettings.keepScreenOn()) {
            wakeFlags = PowerManager.SCREEN_DIM_WAKE_LOCK;
        }
        else {
            Log.i(TAG,"Partial wake Lock acquiring");
            wakeFlags = PowerManager.PARTIAL_WAKE_LOCK;
        }
        wakeLock = pm.newWakeLock(wakeFlags, TAG);
        wakeLock.acquire();
    }
}


/*
 *  Pedometer - Android App
 *  Copyright (C) 2009 Levente Bagi
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.development.mobiledevice.alphafitness;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import android.support.v4.content.ContextCompat;
import  	android.support.v4.app.ActivityCompat;
import android.Manifest;

import java.util.ArrayList;

public class Pedometer extends FragmentActivity implements OnMapReadyCallback {
	private static final String TAG = "Pedometer";
    private SharedPreferences mSettings;
    private PedometerSettings mPedometerSettings;
    private TextView mStepValueView;
    private TextView mDurationView;
    private double mStepValue;
    private boolean mQuitting = false; // Set when user selected Quit from menu, can be used by onPause, onStop, onDestroy
    private GoogleMap mMap;
    private CountDownTimer newtimer;
    /**
     * True, when service is running.
     */
    private static boolean mIsRunning = false;
    private long startTime = 0L;
    private Handler customHandler = new Handler();
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "[ACTIVITY] onCreate");
        super.onCreate(savedInstanceState);
        
        mStepValue = 0;
        int userExists = 0;

        DataBaseHelper myDBHelper = new DataBaseHelper(getApplicationContext());

        SQLiteDatabase myDB = myDBHelper.getReadableDatabase();
        String userSelect = "select "+UserTable.COLUMN_USERID +" from "+UserTable.TABLE_NAME;
        Cursor c = myDB.rawQuery(userSelect, null);
        if (c.moveToFirst()) {
            userExists = 1;
        }
        c.close();
        myDB.close();

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mPedometerSettings = new PedometerSettings(mSettings);
        if (userExists == 0)
        {
            //setContentView(R.layout.createuser);
            Intent intent= new Intent(Pedometer.this,createUser.class);
            startActivity(intent);
            finish();
            return;
        }
        else
        {
            setContentView(R.layout.activity_main);
            mIsRunning = mPedometerSettings.isServiceRunning();
            if(mIsRunning)
            {
                Button startWorkout = (Button) findViewById(R.id.startWorkout);
                startWorkout.setText("Stop Workout");
                bindStepService();
                startDurationCounter();
            }
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mStepValueView = (TextView) findViewById(R.id.distance);
        mDurationView = (TextView) findViewById(R.id.duration);
        if(mDurationView == null)
            Log.i(TAG,"durationview object null");

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }


    private void drawPath(LatLongArrayClass pointsList){

        if(pointsList == null) {
            Log.e(TAG, "points is null");
            return;
        }

        ArrayList<LatLongClass> points = pointsList.getLatLongArray();
        if(points == null || points.size() <= 0){
            Log.e(TAG, "Expecting points - no points recieved");
            return;
        }

        LatLongClass startPoint = points.get(0);
        LatLongClass endPoint = points.get(points.size()-1);
        mMap.clear();  //clears all Markers and Polylines

        if(points.size() > 1) {
            PolylineOptions options = new PolylineOptions().width(10).color(Color.RED).geodesic(true);
            for (LatLongClass p : points) {
                LatLng latLng = new LatLng(p.getLatitude(), p.getLongitude());
                options.add(latLng);
            }
            mMap.addPolyline(options); //add Polyline
            int DARK_GREEN = Color.argb(1, 0, 102, 0);
            mMap.addCircle(new CircleOptions()
                    .center(new LatLng(endPoint.getLatitude(), endPoint.getLongitude())) //end location
                    .radius(7)
                    .strokeColor(DARK_GREEN)
                    .fillColor(DARK_GREEN));
        }

        mMap.addCircle(new CircleOptions()
                .center(new LatLng(startPoint.getLatitude(), startPoint.getLongitude())) //start location
                .radius(7)
                .strokeColor(Color.BLUE)
                .fillColor(Color.BLUE));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(endPoint.getLatitude(), endPoint.getLongitude()),
                (float) 17));
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "[ACTIVITY] onStart");
        super.onStart();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "[ACTIVITY] onResume");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.
                // PERMISSION_REQUEST_ACCESS_FINE_LOCATION can be any unique int
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            }
        }
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.i(TAG, "[ACTIVITY] Permission granted");

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
    
    public void activateProfileScreen (View view)
    {
        Intent intent= new Intent(Pedometer.this,ProfileScreen.class);
        startActivity(intent);
        finish();
    }

    public void startWorkout (View view)
    {
        Button startWorkout = (Button) findViewById(R.id.startWorkout);
        if(startWorkout.getText().equals("Start Workout")) {
            startWorkout.setText("Stop Workout");

            mSettings = PreferenceManager.getDefaultSharedPreferences(this);
            // Read from preferences if the service was running on the last onPause
            mIsRunning = mPedometerSettings.isServiceRunning();
            Log.i(TAG, "[ACTIVITY] is service running "+mIsRunning);
            // Start the service if this is considered to be an application start (last onPause was long ago)
            if (!mIsRunning) {
                Log.i(TAG, "[ACTIVITY] Starting Service ");
                startStepService();
                bindStepService();
            }

            mPedometerSettings.clearServiceRunning();
        }
        else
        {
            startWorkout.setText("Start Workout");
            mIsRunning = false;
            unbindStepService();
            stopStepService();
        }
    }
    
    @Override
    protected void onPause() {
        Log.i(TAG, "[ACTIVITY] onPause");
        if (mIsRunning) {
            unbindStepService();
            customHandler.removeCallbacks(updateTimerThread);
        }
        mPedometerSettings.saveServiceRunning(mIsRunning);
        super.onPause();
        savePaceSetting();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "[ACTIVITY] onStop");
        super.onStop();
    }

    protected void onDestroy() {
        Log.i(TAG, "[ACTIVITY] onDestroy");
        super.onDestroy();
    }
    
    protected void onRestart() {
        Log.i(TAG, "[ACTIVITY] onRestart");
        super.onDestroy();
    }

    private void setDesiredPaceOrSpeed(float desiredPaceOrSpeed) {
        if (mService != null) {

        }
    }
    
    private void savePaceSetting() {
       // mPedometerSettings.savePaceOrSpeedSetting(mMaintain, mDesiredPaceOrSpeed);
    }

    private StepService mService;
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((StepService.StepBinder)service).getService();
            mService.registerCallback(mCallback);
            mService.reloadSettings();
        }
        public void onServiceDisconnected(ComponentName className) {

            mService = null;
        }
    };





    private Runnable updateTimerThread = new Runnable() {

        public void run() {

            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;
            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000);

            mDurationView.setText("" + mins + ":"
                            + String.format("%02d", secs) + ":"
                            + String.format("%03d", milliseconds));
            customHandler.postDelayed(this, 0);

        }
    };

    public void startDurationCounter ()
    {
        if(mIsRunning)
            startTime = mPedometerSettings.getWorkoutStartTime();
        else {
            startTime = SystemClock.uptimeMillis();
            mPedometerSettings.saveWorkoutStartTime(startTime);
        }
        customHandler.postDelayed(updateTimerThread, 0);
    }
    
    private void startStepService() {
        if (!mIsRunning) {
            Log.i(TAG, "[SERVICE] Start");
            startDurationCounter();
            mIsRunning = true;
            startService(new Intent(Pedometer.this,
                    StepService.class));

        }
        else
            Log.i(TAG,"[SERVICE] Service already running");
    }
    
    private void bindStepService() {
        Log.i(TAG, "[SERVICE] Bind");
        bindService(new Intent(Pedometer.this,
                StepService.class), mConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
    }

    private void unbindStepService() {
        Log.i(TAG, "[SERVICE] Unbind");
        unbindService(mConnection);
    }
    
    private void stopStepService() {
        Log.i(TAG, "[SERVICE] Stop");
        if (mService != null) {
            Log.i(TAG, "[SERVICE] stopService");
            stopService(new Intent(Pedometer.this,
                  StepService.class));
        }
        mIsRunning = false;
       // timeSwapBuff += timeInMilliseconds;
        customHandler.removeCallbacks(updateTimerThread);
    }
    
    private void resetValues(boolean updateDisplay) {
        if (mService != null && mIsRunning) {
            mService.resetValues();                    
        }
        else {
            mStepValueView.setText("0");
            SharedPreferences state = getSharedPreferences("state", 0);
            SharedPreferences.Editor stateEditor = state.edit();
            if (updateDisplay) {
                stateEditor.putInt("steps", 0);
                stateEditor.putInt("pace", 0);
                stateEditor.putFloat("distance", 0);
                stateEditor.putFloat("speed", 0);
                stateEditor.putFloat("calories", 0);
                stateEditor.commit();
            }
        }
    }

    // TODO: unite all into 1 type of message
    private StepService.ICallback mCallback = new StepService.ICallback() {
        public void stepsChanged(double value) {
            Message msg = new Message();
            msg.what = STEPS_MSG;
            msg.obj = new Double(value);
            mHandler.sendMessage(msg);
        }

        public void locationChanged(Object obj)
        {
            Message msg = new Message();
            msg.what = LOCATION_MSG;
            msg.obj = obj;
            mHandler.sendMessage(msg);
        }
    };
    
    private static final int STEPS_MSG = 1;
    private static final int LOCATION_MSG = 2;
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STEPS_MSG:
                    mStepValue = (double)msg.obj;
                    Log.i(TAG,"Distance: "+mStepValue);
                    mStepValueView.setText("" + mStepValue);
                    break;
                case LOCATION_MSG:
                    LatLongArrayClass pointsArray = (LatLongArrayClass) msg.obj;
                    drawPath(pointsArray);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
        
    };
    

}
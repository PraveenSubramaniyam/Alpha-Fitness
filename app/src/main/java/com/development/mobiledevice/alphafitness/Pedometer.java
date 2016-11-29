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
import android.content.res.Configuration;
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
import android.support.v4.app.ActivityCompat;
import android.Manifest;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;


import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class Pedometer extends FragmentActivity implements OnMapReadyCallback, OnChartValueSelectedListener {
	private static final String TAG = "Pedometer";
    private SharedPreferences mSettings;
    private PedometerSettings mPedometerSettings;
    private TextView mStepValueView;
    private TextView mDurationView;
    private double mStepValue;
    private GoogleMap mMap;

    private LineChart mChart;
    private double stepLength;
    private TextView minMinsPerKmView;
    private TextView maxMinsPerKmView;
    private TextView avgMinsPerKmView;

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
        Cursor c = myDB.rawQuery(userSelect,null);
        //Cursor c = getApplicationContext().getContentResolver().query(PedometerContentProvider.USERTABLE_CONTENT_URI, null, null, null, null);
        if (c.moveToFirst()) {
            userExists = 1;
        }
        c.close();

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
            startTime = mPedometerSettings.getWorkoutStartTime();

            if((mPedometerSettings.getUserSex().equals("Male")))
                stepLength = mPedometerSettings.getUserheight()*0.415;
            else
                stepLength = mPedometerSettings.getUserheight()*0.413;

            Log.i(TAG,"StepLength: "+stepLength);
            mIsRunning = mPedometerSettings.isServiceRunning();
            Log.i(TAG,"On create IsService Running: "+mIsRunning);
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

                mStepValueView = (TextView) findViewById(R.id.distance);
                mDurationView = (TextView) findViewById(R.id.duration);
                if (mDurationView == null)
                    Log.i(TAG, "durationview object null");

                if (mIsRunning) {
                    Button startWorkout = (Button) findViewById(R.id.startWorkout);
                    startWorkout.setText("Stop Workout");
                    bindStepService();
                    startDurationCounter();
                    UpdateDistanceUI();
                }
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(this);


            }
            else
            {
                Log.i(TAG,"Landscape Mode");
                mChart = (LineChart) findViewById(R.id.chart1);
                minMinsPerKmView = (TextView) findViewById(R.id.minminsperkm);
                maxMinsPerKmView = (TextView) findViewById(R.id.maxminsperkm);
                avgMinsPerKmView = (TextView) findViewById(R.id.avgminsperkm);

                if(mChart != null)
                    Log.i(TAG,"Chart not null");
                mChart.setViewPortOffsets(0, 0, 0, 0);
                mChart.setOnChartValueSelectedListener(this);
                // enable description text
                mChart.getDescription().setEnabled(true);
                // enable touch gestures
                mChart.setTouchEnabled(true);
                // enable scaling and dragging
                mChart.setDragEnabled(true);
                mChart.setScaleEnabled(true);
                mChart.setDrawGridBackground(false);
                // if disabled, scaling can be done on x- and y-axis separately
                mChart.setPinchZoom(true);
                // set an alternative background color
                mChart.setBackgroundColor(Color.LTGRAY);
                LineData data = new LineData();
                data.setValueTextColor(Color.WHITE);
                // add empty data
                mChart.setData(data);
                mChart.animateXY(2000, 2000);
                // get the legend (only possible after setting data)
                Legend l = mChart.getLegend();
                // modify the legend ...
                l.setForm(LegendForm.LINE);
                // l.setTypeface(mTfLight);
                l.setTextColor(Color.WHITE);
                XAxis xl = mChart.getXAxis();
                //  xl.setTypeface(mTfLight);
                xl.setTextColor(Color.WHITE);
                xl.setDrawGridLines(false);
                xl.setAvoidFirstLastClipping(true);
                xl.setEnabled(true);
                YAxis leftAxis = mChart.getAxisLeft();
                // leftAxis.setTypeface(mTfLight);
                leftAxis.setTextColor(Color.WHITE);
                leftAxis.setAxisMaximum(100f);
                leftAxis.setAxisMinimum(0f);
                leftAxis.setDrawGridLines(true);

                YAxis rightAxis = mChart.getAxisRight();
                rightAxis.setEnabled(false);
                if (mIsRunning) {
                    bindStepService();
                }
            }
        }
    }

    private void UpdateDistanceUI()
    {
        int stepsSoFar = mPedometerSettings.getStepCount();
        double distance = (stepLength*stepsSoFar)/100000;

        DecimalFormat df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.CEILING);
        mStepValueView.setText("" + df.format(distance));

    }

    private LineDataSet createSet(String name) {
        LineDataSet set = new LineDataSet(null, name);
        set.setAxisDependency(AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

    private int[] mColors = new int[] {
            ColorTemplate.VORDIPLOM_COLORS[0],
            ColorTemplate.VORDIPLOM_COLORS[1],
            ColorTemplate.VORDIPLOM_COLORS[2]
    };

    private void addEntry(ArrayList<Integer> graphValue)
    {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            int minVal = Integer.MAX_VALUE;
            int maxVal = 0;
            int sum = 0;
            double distance;
            int totalsecs, secs,mins;

            mChart = (LineChart) findViewById(R.id.chart1);
            TextView mStepValueView = (TextView) findViewById(R.id.distance);
            if(mStepValueView != null)
                Log.i(TAG,"stepview not null");
            else
                Log.i(TAG," stepview null");


            if(mChart != null) {
                LineData data = mChart.getData();
                LineDataSet set1, set2;
                set1 = createSet("Steps Per Min");
                set2 = createSet("Calories per Min");

                data.removeDataSet(1);
                data.removeDataSet(0);
                data.addDataSet(set1);
                data.addDataSet(set2);

                int xValue = 1;
                for (Integer value : graphValue) {
                    //code here
                    set1.addEntry(new Entry(xValue, value));
                    int calorie = (int) Math.round(((mPedometerSettings.getUserWeight() * 28)/100000.0)*value);
                    set2.addEntry(new Entry(xValue++,calorie));

                    if(value != 0)
                    {
                        if(value  < minVal)
                            minVal = value;
                    }

                    if(value > maxVal)
                        maxVal = value;
                    sum += value;
                }

                if(minVal != Integer.MAX_VALUE) {
                    //minVal = minVal / 5;
                    distance = (minVal * stepLength) / 100000;
                    totalsecs = (int) (1 / distance) * 60;
                    mins = totalsecs / 60;
                    secs = totalsecs - mins * 60;
                    minMinsPerKmView.setText(String.valueOf(mins) + ":" + String.valueOf(secs));
                }
                else
                    minMinsPerKmView.setText("Infinity");

                if(maxVal != 0) {
                    //maxVal = maxVal / 5;
                    distance = (maxVal * stepLength) / 100000;
                    totalsecs = (int) (1 / distance) * 60;
                    mins = totalsecs / 60;
                    secs = totalsecs - mins * 60;
                    maxMinsPerKmView.setText(String.valueOf(mins) + ":" + String.valueOf(secs));
                }
                else
                    maxMinsPerKmView.setText("Infinity");

                if(sum != 0) {
                    maxVal = sum / ((xValue - 1) * 1);
                    distance = (maxVal * stepLength) / 100000;
                    totalsecs = (int) (1 / distance) * 60;
                    mins = totalsecs / 60;
                    secs = totalsecs - mins * 60;
                    avgMinsPerKmView.setText(String.valueOf(mins) + ":" + String.valueOf(secs));
                }
                else
                    avgMinsPerKmView.setText("Infinity");


                int color1 = mColors[1 % mColors.length];
                int color2 = mColors[2 % mColors.length];

                set1.setColor(color1);
                set1.setCircleColor(color1);

                set2.setColor(color2);
                set2.setCircleColor(color2);

                set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                set2.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                set1.setCubicIntensity(0.2f);
                set2.setCubicIntensity(0.2f);

                // make the first DataSet dashed
                set1.enableDashedLine(10, 10, 0);
                set1.setColors(ColorTemplate.VORDIPLOM_COLORS);
                set1.setCircleColors(ColorTemplate.VORDIPLOM_COLORS);

                set1.setDrawFilled(true);

                set2.setDrawFilled(true);

                set1.setFillColor(Color.GREEN);
                set2.setFillColor(Color.rgb(216, 8, 140));

                data.notifyDataChanged();
                //let the chart know it's data has changed
                mChart.notifyDataSetChanged();
                // limit the number of visible entries
                mChart.setVisibleXRangeMaximum(5);
                // mChart.setVisibleYRange(30, AxisDependency.LEFT);

                // move to the latest entry
                mChart.moveViewToX(data.getEntryCount());
            }
            else
                Log.i(TAG,"mchart null");
        }
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Log.i("Entry selected", e.toString());
    }

    @Override
    public void onNothingSelected() {
        Log.i("Nothing selected", "Nothing selected.");
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
        Log.i(TAG,"draw Path called");
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.i(TAG,"InLadscape Mode no map");
            return;
        }
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
            Log.i(TAG,"point size greater than 1");
            PolylineOptions options = new PolylineOptions().width(10).color(Color.RED).geodesic(true);
            for (LatLongClass p : points) {
                LatLng latLng = new LatLng(p.getLatitude(), p.getLongitude());
                Log.i(TAG,"Point: "+p.getLatitude()+" "+p.getLongitude());
                options.add(latLng);
            }
            mMap.addPolyline(options); //add Polyline
            mMap.addCircle(new CircleOptions()
                    .center(new LatLng(endPoint.getLatitude(), endPoint.getLongitude())) //end location
                    .radius(7)
                    .strokeColor(Color.GREEN)
                    .fillColor(Color.GREEN));
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
        Log.i(TAG, "[ACTIVITY] Start/Stop Workout is service running "+mIsRunning);
        if(!mIsRunning)
        {
            startWorkout.setText("Stop Workout");
            Log.i(TAG, "[ACTIVITY] Starting Service ");
            startStepService();
            bindStepService();
            mIsRunning = true;
            mPedometerSettings.saveServiceRunning(true);
        }
        else
        {
            startWorkout.setText("Start Workout");
            mIsRunning = false;
            unbindStepService();
            stopStepService();
            mPedometerSettings.clearServiceRunning();
        }
    }
    
    @Override
    protected void onPause() {
        Log.i(TAG, "[ACTIVITY] onPause");
        if (mIsRunning) {
            unbindStepService();
            customHandler.removeCallbacks(updateTimerThread);
        }
        super.onPause();
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

    private StepService mService;
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((StepService.StepBinder)service).getService();
            mService.registerCallback(mCallback);
            Log.i(TAG,"Service connected");
        }
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            Log.i(TAG,"Service unconnected");
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
        Log.i(TAG, "[SERVICE] Start");
        startDurationCounter();
        startService(new Intent(Pedometer.this,
                    StepService.class));
    }
    
    private void bindStepService() {
        Log.i(TAG, "[SERVICE] Bind");
        bindService(new Intent(Pedometer.this,
                StepService.class), mConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
    }

    private void unbindStepService() {
        Log.i(TAG, "[SERVICE] Unbind");
        unbindService(mConnection);
        mService.unregisterCallback();
    }
    
    private void stopStepService() {
        Log.i(TAG, "[SERVICE] Stop");
        if (mService != null) {
            Log.i(TAG, "[SERVICE] stopService");
            stopService(new Intent(Pedometer.this,
                  StepService.class));
        }
        customHandler.removeCallbacks(updateTimerThread);
    }
    

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

        public void graphData(Object obj)
        {
            Message msg = new Message();
            msg.what = GRAPH_DATA_MSG;
            msg.obj = obj;
            mHandler.sendMessage(msg);
        }
    };
    
    private static final int STEPS_MSG = 1;
    private static final int LOCATION_MSG = 2;
    private static final int GRAPH_DATA_MSG = 3;
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STEPS_MSG:
                    double distance;
                    mStepValue = (double)msg.obj;
                    distance = (mStepValue*stepLength)/100000;
                    //Toast.makeText(getApplicationContext(), "Distance: "+distance, Toast.LENGTH_LONG).show();
                    Log.i(TAG,"Distance: "+distance);
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        DecimalFormat df = new DecimalFormat("#.###");
                        df.setRoundingMode(RoundingMode.CEILING);
                        mStepValueView.setText("" + df.format(distance));
                    }
                    break;
                case LOCATION_MSG:
                    LatLongArrayClass pointsArray = (LatLongArrayClass) msg.obj;
                    drawPath(pointsArray);
                    break;
                case GRAPH_DATA_MSG:
                    ArrayList<Integer> graphData;
                    graphData = (ArrayList<Integer>)msg.obj;
                    Log.i(TAG,"Graph Data Msg called");
                    addEntry(graphData);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
        
    };
    

}
package com.development.mobiledevice.alphafitness;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;
import java.util.ArrayList;

public class StepDetector implements SensorEventListener
{
    private final static String TAG = "Pedometer";
    private float   mScale[] = new float[2];
    private ArrayList<StepListener> mStepListeners = new ArrayList<StepListener>();

    public void addStepListener(StepListener sl) {
        mStepListeners.add(sl);
    }
    public void onSensorChanged(SensorEvent event) {
        Log.i(TAG, "On Sensor Changed called");

        Sensor sensor = event.sensor;
        synchronized (this) {
            int j = (sensor.getType() == Sensor.TYPE_STEP_DETECTOR) ? 1 : 0;
            if (j == 1) {
                float vSum = 0;
                Log.i(TAG, "step");
                for (StepListener stepListener : mStepListeners) {
                    stepListener.onStep();
                }
            }
        }
    }
    
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

}
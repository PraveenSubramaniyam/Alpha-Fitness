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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Detects steps and notifies all listeners (that implement StepListener).
 * @author Levente Bagi
 * @todo REFACTOR: SensorListener is deprecated
 */
public class StepDetector implements SensorEventListener
{
    private final static String TAG = "Pedometer";
    private float   mLimit = 10;
    private float   mLastValues[] = new float[3*2];
    private float   mScale[] = new float[2];
    private float   mYOffset;

    private float   mLastDirections[] = new float[3*2];
    private float   mLastExtremes[][] = { new float[3*2], new float[3*2] };
    private float   mLastDiff[] = new float[3*2];
    private int     mLastMatch = -1;
    
    private ArrayList<StepListener> mStepListeners = new ArrayList<StepListener>();
    
    public StepDetector() {
        int h = 480; // TODO: remove this constant
        mYOffset = h * 0.5f;
        mScale[0] = - (h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        mScale[1] = - (h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
    }
    
    public void setSensitivity(float sensitivity) {
        mLimit = sensitivity; // 1.97  2.96  4.44  6.66  10.00  15.00  22.50  33.75  50.62
    }
    
    public void addStepListener(StepListener sl) {
        mStepListeners.add(sl);
    }
    
    //public void onSensorChanged(int sensor, float[] values) {
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
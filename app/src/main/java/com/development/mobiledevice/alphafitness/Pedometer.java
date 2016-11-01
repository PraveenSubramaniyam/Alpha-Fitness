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


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class Pedometer extends Activity {
	private static final String TAG = "Pedometer";
    private SharedPreferences mSettings;
    private PedometerSettings mPedometerSettings;
    private TextView mStepValueView;
    TextView mDesiredPaceView;
    private int mStepValue;
    private boolean mQuitting = false; // Set when user selected Quit from menu, can be used by onPause, onStop, onDestroy

    /**
     * True, when service is running.
     */
    private boolean mIsRunning = false;
    
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
        if (userExists == 0)
        {
            setContentView(R.layout.activity_main);
        }
        else
        {
            mIsRunning = mPedometerSettings.isServiceRunning();
            if(mIsRunning)
            {
                setContentView(R.layout.activity_main);
                Button startWorkout = (Button) findViewById(R.id.startWorkout);
                startWorkout.setText("Stop Workout");
            }
        }
    }
    
    @Override
    protected void onStart() {
        Log.i(TAG, "[ACTIVITY] onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "[ACTIVITY] onResume");
        super.onResume();
    }
    
    private void displayDesiredPaceOrSpeed() {

    }

    public void startWorkout (View view)
    {
        Button startWorkout = (Button) findViewById(R.id.startWorkout);
        if(startWorkout.getText().equals("Start Workout")) {
            startWorkout.setText("Stop Workout");

            mSettings = PreferenceManager.getDefaultSharedPreferences(this);
            mPedometerSettings = new PedometerSettings(mSettings);

            // Read from preferences if the service was running on the last onPause
            mIsRunning = mPedometerSettings.isServiceRunning();
            Log.i(TAG, "[ACTIVITY] is service running "+mIsRunning);
            // Start the service if this is considered to be an application start (last onPause was long ago)
            if (!mIsRunning) {
                Log.i(TAG, "[ACTIVITY] Starting Service ");
                startStepService();
                bindStepService();
            } else if (mIsRunning) {
                bindStepService();
            }

            mStepValueView = (TextView) findViewById(R.id.distance);
            mPedometerSettings.clearServiceRunning();
            displayDesiredPaceOrSpeed();
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
    

    private void startStepService() {
        if (!mIsRunning) {
            Log.i(TAG, "[SERVICE] Start");
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

    private static final int MENU_SETTINGS = 8;
    private static final int MENU_QUIT     = 9;

    private static final int MENU_PAUSE = 1;
    private static final int MENU_RESUME = 2;
    private static final int MENU_RESET = 3;
    
    /* Creates the menu items */
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();

        return true;
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_PAUSE:
                unbindStepService();
                stopStepService();
                return true;
            case MENU_RESUME:
                startStepService();
                bindStepService();
                return true;
            case MENU_RESET:
                resetValues(true);
                return true;
            case MENU_QUIT:
                resetValues(false);
                unbindStepService();
                stopStepService();
                mQuitting = true;
                finish();
                return true;
        }
        return false;
    }
 
    // TODO: unite all into 1 type of message
    private StepService.ICallback mCallback = new StepService.ICallback() {
        public void stepsChanged(int value) {
            mHandler.sendMessage(mHandler.obtainMessage(STEPS_MSG, value, 0));
        }
        public void paceChanged(int value) {
            mHandler.sendMessage(mHandler.obtainMessage(PACE_MSG, value, 0));
        }
        public void distanceChanged(float value) {
            mHandler.sendMessage(mHandler.obtainMessage(DISTANCE_MSG, (int)(value*1000), 0));
        }
        public void speedChanged(float value) {
            mHandler.sendMessage(mHandler.obtainMessage(SPEED_MSG, (int)(value*1000), 0));
        }
        public void caloriesChanged(float value) {
            mHandler.sendMessage(mHandler.obtainMessage(CALORIES_MSG, (int)(value), 0));
        }
    };
    
    private static final int STEPS_MSG = 1;
    private static final int PACE_MSG = 2;
    private static final int DISTANCE_MSG = 3;
    private static final int SPEED_MSG = 4;
    private static final int CALORIES_MSG = 5;
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STEPS_MSG:
                    mStepValue = (int)msg.arg1;
                    mStepValueView.setText("" + mStepValue);
                    break;
                case PACE_MSG:

                    break;
                case DISTANCE_MSG:
                    //mDistanceValue = ((int)msg.arg1)/1000f;

                    break;
                case SPEED_MSG:
                   // mSpeedValue = ((int)msg.arg1)/1000f;

                    break;
                case CALORIES_MSG:
                   // mCaloriesValue = msg.arg1;

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
        
    };
    

}
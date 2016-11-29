
package com.development.mobiledevice.alphafitness;

import android.content.SharedPreferences;

public class PedometerSettings {

    SharedPreferences mSettings;

    public PedometerSettings(SharedPreferences settings) {
        mSettings = settings;
    }

    public boolean wakeAggressively() {
        return mSettings.getString("operation_level", "run_in_background").equals("wake_up");
    }

    public boolean keepScreenOn() {
        return mSettings.getString("operation_level", "run_in_background").equals("keep_screen_on");
    }


    public void saveServiceRunning(boolean running) {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean("service_running", running);
        editor.commit();
    }

    public void saveWorkoutStartTime(long startTime) {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putLong("startTime", startTime);
        editor.commit();
    }

    public long getWorkoutStartTime()
    {
        return mSettings.getLong("startTime", 0);
    }

    public void saveStepCountTableId(String id)
    {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString("stepCountId", id);
        editor.commit();
    }

    public String getStepCountTableId()
    {
        return mSettings.getString("stepCountId", "");
    }

    public void saveUserheight(int height)
    {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt("height", height);
        editor.commit();
    }

    public int getUserheight()
    {
        return mSettings.getInt("height", 0);
    }

    public void saveUserWeight(int weight)
    {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt("weight", weight);
        editor.commit();
    }

    public int getUserWeight()
    {
        return mSettings.getInt("weight", 0);
    }


    public void saveUserSex(String sex)
    {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString("sex", sex);
        editor.commit();
    }

    public String getUserSex()
    {
        return mSettings.getString("sex", "");
    }

    public void clearServiceRunning() {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean("service_running", false);
        editor.commit();
    }

    public void saveStepCount(int stepCount)
    {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt("StepCount", stepCount);
        editor.commit();
    }

    public void clearStepCount()
    {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt("StepCount", 0);
        editor.commit();
    }

    public int getStepCount()
    {
        return mSettings.getInt("StepCount", 0);
    }


    public boolean isServiceRunning() {
        return mSettings.getBoolean("service_running", false);
    }
}

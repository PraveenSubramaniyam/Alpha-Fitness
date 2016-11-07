package com.development.mobiledevice.alphafitness;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ProfileScreen extends AppCompatActivity {

    private SharedPreferences mSettings;
    private PedometerSettings mPedometerSettings;
    private boolean mIsRunning = false;
    private static final String TAG = "Pedometer";
    DataBaseHelper db;
    String userName="", sex="";
    long totalWorkOuts = 0;
    double totalDistance = 0;
    long totalSecs=0;
    private double stepLength;
    private String startTime = "";
    private int noofWeeks = 1;
    int age =0;
    int weight=0,height=0;
    private Handler customHandler = new Handler();
    long timeInMilliseconds = 0L;
    private long startTimeLong = 0L;
    private TextView timeTotalView;
    private TextView timeWeeklyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mPedometerSettings = new PedometerSettings(mSettings);

        String totalTimeString = "";
        String avgTimeString = "";

        db = new DataBaseHelper(getApplicationContext());

        SQLiteDatabase myDB = db.getReadableDatabase();
        String countSelect = "select count(*) from "+StepCounterTable.TABLE_NAME;
        Cursor c = myDB.rawQuery(countSelect, null);
        if (c.moveToFirst()) {
            totalWorkOuts = c.getInt(0);
        }
        c.close();

        int totalStepCounts = 0;
        String totalStepsQuery = "select sum("+ StepCounterTable.C_COUNT+ ") from "+StepCounterTable.TABLE_NAME;
        c = myDB.rawQuery(totalStepsQuery, null);
        if (c.moveToFirst()) {
            totalStepCounts = c.getInt(0);
        }
        c.close();
          /*
        Estimate by Height
        These are rough estimates, but useful to check your results by the other methods. It is the method used in the automatic settings of many pedometers and activity trackers:
        Females: Your height x .413 equals your stride length
        Males: Your height x .415 equals your stride length
         */
        if((mPedometerSettings.getUserSex().equals("Male")))
            stepLength = mPedometerSettings.getUserheight()*0.415;
        else
            stepLength = mPedometerSettings.getUserheight()*0.413;

        totalDistance = (double)(stepLength * (double)totalStepCounts)/100000.0;


        String totalTimeQuery = "select sum("+ StepCounterTable.C_TOTALTIME+ ") from "+StepCounterTable.TABLE_NAME;
        c = myDB.rawQuery(totalTimeQuery, null);
        if (c.moveToFirst()) {
            totalSecs = c.getInt(0);
        }
        c.close();
        /*SELECT * FROM SAMPLE_TABLE ORDER BY ROWID ASC LIMIT 1 */
        String firstTimeQuery = "select "+ StepCounterTable.C_STARTTIME+ " from "+StepCounterTable.TABLE_NAME
                +" ORDER BY "+StepCounterTable.C_ID+" ASC LIMIT 1";
        Log.i(TAG," firstTime Query: "+firstTimeQuery);
        c = myDB.rawQuery(firstTimeQuery, null);
        if (c.moveToFirst()) {
            startTime = c.getString(0);

        }
        c.close();

        Log.i(TAG,"StartTime: "+startTime);
        Date inputDate = new Date() ;
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try{
            inputDate = dateFormat.parse(startTime);
        }
        catch(Exception e)
        {
            Log.i(TAG,e.toString());
        }
        Date todayDate = new Date();

        noofWeeks = getWeeksBetween(inputDate,todayDate) + 1;
        Log.i(TAG,"No of Weeks: "+noofWeeks);

        totalTimeString = convertTimeToString(totalSecs);
        avgTimeString = convertTimeToString(totalSecs/noofWeeks);


        String userNameQuery = "select "+UserTable.COLUMN_USERNAME+" , "+UserTable.COLUMN_AGE+" ," +
                UserTable.COLUMN_HEIGHT+" , "+UserTable.COLUMN_SEX+" , " +UserTable.COLUMN_WEIGHT+" from "+
                UserTable.TABLE_NAME;
        c = myDB.rawQuery(userNameQuery, null);
        if (c.moveToFirst()) {
            userName = c.getString(0);
            age = c.getInt(1);
            height = c.getInt(2);
            sex = c.getString(3);
            weight = c.getInt(4);
        }
        c.close();

        myDB.close();


        setContentView(R.layout.profile_screen_workout_desc);
        EditText user = (EditText)findViewById(R.id.userNameProfileScreen);
        user.setText(userName);
        EditText ageText = (EditText) findViewById(R.id.ageProfileScreen);
        ageText.setText(String.valueOf(age));
        EditText weighttext = (EditText)findViewById(R.id.weightProfileScreen);
        weighttext.setText(String.valueOf(weight));
        EditText heighttext = (EditText)findViewById(R.id.heightProfileScreen);
        heighttext.setText(String.valueOf(height));

        Spinner dropdown = (Spinner)findViewById(R.id.genderSpinner);
        String[] items = new String[]{"Male", "Female"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items);
        dropdown.setAdapter(adapter);
        int spinnerPosition = adapter.getPosition(sex);

        dropdown.setSelection(spinnerPosition);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ((TextView)findViewById(R.id.distanceWeekly)).setText(String.valueOf(totalDistance/noofWeeks)+ " km");
        ((TextView)findViewById(R.id.workoutsWeekly)).setText(String.valueOf(totalWorkOuts/noofWeeks) + " times");
        timeWeeklyView = (TextView)findViewById(R.id.timeWeekly);
        timeWeeklyView.setText(avgTimeString);
        ((TextView)findViewById(R.id.caloriesWeekly)).setText("");

        ((TextView)findViewById(R.id.distanceTotal)).setText(String.valueOf(totalDistance)+ " km");
        ((TextView)findViewById(R.id.workoutsTotal)).setText(String.valueOf(totalWorkOuts) + " times");
        timeTotalView = (TextView)findViewById(R.id.timeTotal);
        timeTotalView.setText(totalTimeString);
        ((TextView)findViewById(R.id.caloriesTotal)).setText("");

        mIsRunning = mPedometerSettings.isServiceRunning();
        if(mIsRunning) {
            bindStepService();
            startDurationCounter();
        }
    }

    public String convertTimeToString(long totalSecs)
    {
        String totalTimeString = "";
        int day = (int)TimeUnit.SECONDS.toDays(totalSecs);
        long hours = TimeUnit.SECONDS.toHours(totalSecs) - (day *24);
        long minute = TimeUnit.SECONDS.toMinutes(totalSecs) - (TimeUnit.SECONDS.toHours(totalSecs)* 60);
        long second = TimeUnit.SECONDS.toSeconds(totalSecs) - (TimeUnit.SECONDS.toMinutes(totalSecs) *60);

        if(day != 0)
            totalTimeString += (String.valueOf(day)+ " day ");

        if(hours != 0)
            totalTimeString += (String.valueOf(hours)+ " hr ");

        if(minute != 0)
            totalTimeString += (String.valueOf(minute)+ " min ");

        if(second != 0)
            totalTimeString += (String.valueOf(second)+ " sec");
        return totalTimeString;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return false;
    }

    public static int getWeeksBetween (Date a, Date b) {
        if (b.before(a)) {
            return -getWeeksBetween(b, a);
        }
        a = resetTime(a);
        b = resetTime(b);

        Calendar cal = new GregorianCalendar();
        cal.setTime(a);
        int weeks = 0;
        while (cal.getTime().before(b)) {
            // add another week
            cal.add(Calendar.WEEK_OF_YEAR, 1);
            weeks++;
        }
        return weeks;
    }

    public static Date resetTime (Date d) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    @Override
    public void onPause()
    {
        Log.i(TAG, "[Profile Screen ACTIVITY] onPause");
        if (mPedometerSettings.isServiceRunning()) {
            unbindStepService();
            customHandler.removeCallbacks(updateTimerThread);
        }
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        boolean updateNeeded = false;

        EditText user = (EditText)findViewById(R.id.userNameProfileScreen);
        EditText ageText = (EditText) findViewById(R.id.ageProfileScreen);
        EditText weighttext = (EditText)findViewById(R.id.weightProfileScreen);
        EditText heighttext = (EditText)findViewById(R.id.heightProfileScreen);
        Spinner mySpinner=(Spinner) findViewById(R.id.genderSpinner);

        String nameval =user.getText().toString();
        int ageVal = Integer.parseInt(ageText.getText().toString());
        int weightVal = Integer.parseInt(weighttext.getText().toString());
        int heightVal = Integer.parseInt(heighttext.getText().toString());
        String text = mySpinner.getSelectedItem().toString();

        if (!nameval.equals(userName) || ageVal != age || weightVal != weight
                || heightVal != height || !text.equals(sex))
        {
            Log.i(TAG,"Update value");
            ContentValues values = new ContentValues();
            values.put(UserTable.COLUMN_USERNAME,nameval);
            values.put(UserTable.COLUMN_SEX, text);
            values.put(UserTable.COLUMN_AGE,ageVal);
            values.put(UserTable.COLUMN_HEIGHT, heightVal);
            values.put(UserTable.COLUMN_WEIGHT,weightVal);
            db.updateUserValue(values);
        }

        Intent intent = new Intent(ProfileScreen.this, Pedometer.class);
        startActivity(intent);
        finish();
    }

    public void startDurationCounter ()
    {
        startTimeLong = mPedometerSettings.getWorkoutStartTime();
        customHandler.postDelayed(updateTimerThread, 0);
    }

    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTimeLong;
            int secs = (int) (timeInMilliseconds / 1000);
            String totalTimeString= convertTimeToString(totalSecs+secs);
            String weeklyTimeString =convertTimeToString((totalSecs+secs)/noofWeeks);
            timeWeeklyView.setText(weeklyTimeString);
            timeTotalView.setText(totalTimeString);
            customHandler.postDelayed(this, 0);
        }
    };

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

    private void unbindStepService() {
        Log.i(TAG, "[SERVICE] Unbind");
        unbindService(mConnection);
    }


    private StepService mService;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG,"Service connected in Profile Screen");
            mService = ((StepService.StepBinder)service).getService();
            mService.registerCallback(mCallback);
            mService.reloadSettings();
        }
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    private void bindStepService() {
        Log.i(TAG, "[SERVICE] Bind");
        bindService(new Intent(ProfileScreen.this,
                StepService.class), mConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
    }



    private static final int STEPS_MSG = 1;
    private static final int LOCATION_MSG = 2;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STEPS_MSG:
                    totalDistance += (Double)msg.obj;
                    Log.i(TAG,"TotalDistances: "+totalDistance);
                    break;
                case LOCATION_MSG:

                  break;
                default:
                    super.handleMessage(msg);
            }
        }

    };


}

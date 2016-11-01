package com.development.mobiledevice.alphafitness;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class ProfileScreen extends AppCompatActivity {

    DataBaseHelper db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long totalWorkOuts = 0;
        long totalStepCounts = 0;
        DataBaseHelper myDBHelper = new DataBaseHelper(getApplicationContext());

        SQLiteDatabase myDB = myDBHelper.getReadableDatabase();
        String countSelect = "select count(*) from "+StepCounterTable.TABLE_NAME;
        Cursor c = myDB.rawQuery(countSelect, null);
        if (c.moveToFirst()) {
            totalWorkOuts = c.getInt(0);
        }
        c.close();

        String totalDistances = "select sum("+StepCounterTable.C_COUNT+") from "+StepCounterTable.TABLE_NAME;
        c = myDB.rawQuery(totalDistances, null);
        if (c.moveToFirst()) {
            totalStepCounts = c.getInt(0);
        }
        c.close();

        myDB.close();
        setContentView(R.layout.profile_screen_workout_desc);
    }
}

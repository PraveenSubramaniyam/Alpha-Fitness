package com.development.mobiledevice.alphafitness;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class createUser extends AppCompatActivity {

    private PedometerSettings mPedometerSettings;
    private SharedPreferences mSettings;
    private DataBaseHelper db;
    private static final String TAG = "Pedometer";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getApplicationContext().deleteDatabase("AlphaFitness");
        setContentView(R.layout.activity_create_user);
        Spinner dropdown = (Spinner)findViewById(R.id.createUserSex);
        String[] items = new String[]{"Male", "Female"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items);
        dropdown.setAdapter(adapter);
    }

    public void addUser(View view)
    {
        long id;
        EditText firstName = (EditText) findViewById(R.id.firstName);
        EditText lastName = (EditText) findViewById(R.id.lastName);
        EditText age = (EditText) findViewById(R.id.age);
        EditText height = (EditText) findViewById(R.id.height);
        EditText weight = (EditText) findViewById(R.id.weight);
        Spinner sex = (Spinner) findViewById(R.id.createUserSex);

        String name = firstName.getText().toString() + " "+lastName.getText().toString();
        int ageVal = Integer.parseInt(age.getText().toString());
        int heightVal = Integer.parseInt(height.getText().toString());
        int weightVal = Integer.parseInt(weight.getText().toString());
        //sex.get
        String sexValue = sex.getItemAtPosition(sex.getSelectedItemPosition()).toString();


        db = new DataBaseHelper(getApplicationContext());
        Log.i(TAG,name+":"+ageVal+":"+sexValue+":"+heightVal+":"+weightVal);
        id = db.insertUserTable(name,ageVal,sexValue,heightVal,weightVal);

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mPedometerSettings = new PedometerSettings(mSettings);
        mPedometerSettings.saveUserheight(heightVal);
        mPedometerSettings.saveUserSex(sexValue);
        Intent intent= new Intent(createUser.this,Pedometer.class);
        startActivity(intent);
        finish();
    }
}

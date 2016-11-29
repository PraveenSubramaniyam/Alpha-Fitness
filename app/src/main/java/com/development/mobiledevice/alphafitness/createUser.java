package com.development.mobiledevice.alphafitness;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

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
        String ageValStr = age.getText().toString();
        String heightValStr = height.getText().toString();
        String weightValStr = weight.getText().toString();

        name = name.trim().replaceAll("\n ", "");
        ageValStr = ageValStr.trim().replaceAll("\n ", "");
        heightValStr = heightValStr.trim().replaceAll("\n ", "");
        weightValStr = weightValStr.trim().replaceAll("\n ", "");

        if(name.equals("") || ageValStr.equals("") || heightValStr.equals("") || weightValStr.equals(""))
        {
            Toast.makeText(getApplicationContext(), "Please Enter all the values", Toast.LENGTH_LONG).show();
            return;
        }

        int ageVal = Integer.parseInt(ageValStr);
        int heightVal = Integer.parseInt(heightValStr);
        int weightVal = Integer.parseInt(weightValStr);
        //sex.get
        String sexValue = sex.getItemAtPosition(sex.getSelectedItemPosition()).toString();


        db = new DataBaseHelper(getApplicationContext());
        Log.i(TAG,name+":"+ageVal+":"+sexValue+":"+heightVal+":"+weightVal);

        ContentValues values = new ContentValues();
        values.put(UserTable.COLUMN_USERNAME, name);
        values.put(UserTable.COLUMN_HEIGHT, heightVal);
        values.put(UserTable.COLUMN_AGE,ageVal);
        values.put(UserTable.COLUMN_WEIGHT,weightVal);
        values.put(UserTable.COLUMN_SEX, sexValue);

        //id = db.insertUserTable(name,ageVal,sexValue,heightVal,weightVal);
        Uri uri = getApplicationContext().getContentResolver().insert(PedometerContentProvider.USERTABLE_CONTENT_URI, values);
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mPedometerSettings = new PedometerSettings(mSettings);
        mPedometerSettings.saveUserheight(heightVal);
        mPedometerSettings.saveUserSex(sexValue);
        mPedometerSettings.saveUserWeight(weightVal);
        Intent intent= new Intent(createUser.this,Pedometer.class);
        startActivity(intent);
        finish();
    }
}

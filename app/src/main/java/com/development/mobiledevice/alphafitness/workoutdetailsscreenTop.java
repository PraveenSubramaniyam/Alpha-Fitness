package com.development.mobiledevice.alphafitness;


import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class workoutdetailsscreenTop extends Fragment {
    private static final String TAG = "Pedometer";
    int mCurCheckPosition = 0;
       
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.i(TAG,"view created11ss1");
       return inflater.inflate(R.layout.workoutdetailsscreentop, container, false);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("curChoice", mCurCheckPosition);
    }
    
   

    /**
     * Helper function to show the details of a selected item, either by
     * displaying a fragment in-place in the current UI, or starting a
     * whole new activity in which it is displayed.
     */
   
}
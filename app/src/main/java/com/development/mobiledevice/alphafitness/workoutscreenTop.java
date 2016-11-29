package com.development.mobiledevice.alphafitness;


import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class workoutscreenTop extends Fragment {
    
    int mCurCheckPosition = 0;
       
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
       System.out.println("view created");
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            return inflater.inflate(R.layout.workoutscreentop, container, false);
        }
        else
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
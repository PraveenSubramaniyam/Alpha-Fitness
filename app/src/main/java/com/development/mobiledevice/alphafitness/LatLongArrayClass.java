package com.development.mobiledevice.alphafitness;

import java.io.Serializable;
import java.util.ArrayList;

public class LatLongArrayClass implements Serializable {

    private ArrayList<LatLongClass> LatLongArray;

    public LatLongArrayClass()
    {
        LatLongArray = new ArrayList<LatLongClass>();
    }

    public void addLatLong(double latitude, double longitude)
    {
        LatLongArray.add(new LatLongClass(latitude, longitude));
    }

    public ArrayList<LatLongClass> getLatLongArray() {
        return LatLongArray;
    }

    public void setLatLongArray(ArrayList<LatLongClass> latLongArray) {
        LatLongArray = latLongArray;
    }
    public LatLongClass getLastLatLongClass()
    {
        return LatLongArray.get(LatLongArray.size()-1);
    }

}

package com.development.mobiledevice.alphafitness;

import java.io.Serializable;


public class LatLongClass implements Serializable {


    private double latitude;
    private double longitude;

    public LatLongClass (double latitude, double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

}

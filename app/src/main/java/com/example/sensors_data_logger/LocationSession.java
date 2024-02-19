package com.example.sensors_data_logger;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.IOException;
import java.security.KeyException;
import java.util.Date;

public class LocationSession implements LocationListener {

    private static final String TAG = LocationSession.class.getSimpleName();
    private FileStreamer mFileStreamer = null;

    //private String mFolderName;
    private LocationManager mLocationManager = null;
    protected long mNanosOffset = 0;
    protected String mSensorName;

    private Context mContext;
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "LocationServiceChannel";

    private String label;
    private SwitchMaterial mLabelLay;
    private SwitchMaterial mLabelWalk;
    private SwitchMaterial mLabelRun;
    private SwitchMaterial mLabelSit;
    private SwitchMaterial mLabelStairs;
    private SwitchMaterial mLabelCustom;
    private EditText mCustomLabel;


    //pri povik na konstruktorot zasega nanosOffset da se stava 0
    public LocationSession(Context context, String folderName, String sensorName, long nanosOffset) {

        mContext = context;
        mSensorName = sensorName;
        //mFolderName = folderName;

        String path = File.separator + mSensorName;
        // Offset to match timestamps both in master and slaves devices
        mNanosOffset = nanosOffset;
        Log.d(TAG, "VO LOCATIONSESSION context: " + mContext + ", folderName: "+ folderName);
        mFileStreamer = new FileStreamer(mContext,folderName);

        mLocationManager = (LocationManager) mContext.getSystemService(mContext.LOCATION_SERVICE);
        Log.d(TAG, "constructor() in LocationSession executed");
    }

    @Override
    public void onLocationChanged(Location location) {

        if (location == null) return;

        // Return the time of this fix, in elapsed real-time since system boot.
//        long locationNanoTime = location.getElapsedRealtimeNanos() + mNanosOffset;
//
//        // System local time in millis
//        long currentMillis = (new Date()).getTime();

        // Get the estimated accuracy of this location, in meters.
        float accuracy = location.getAccuracy();

        // Get the latitude, in degrees.
        float latitude = (float)location.getLatitude();

        // Get the longitude, in degrees.
        float longitude = (float)location.getLongitude();

        // Get the altitude if available, in meters above the WGS 84 reference ellipsoid.
        float altitude = (float)location.getAltitude();

        long timestamp = System.currentTimeMillis() + (location.getElapsedRealtimeNanos() - SystemClock.elapsedRealtimeNanos()) / 1000000;

        addLabel();

        final float[] locationValues = new float[4];
        locationValues[0]=accuracy;
        locationValues[1]=latitude;
        locationValues[2]=longitude;
        locationValues[3]=altitude;
//        // Get the latitude, in degrees.
//        String latitudeDegrees = Location.convert(latitude, Location.FORMAT_DEGREES);
//        // Get the longitude, in degrees.
//        String longitudeDegrees = Location.convert(longitude, Location.FORMAT_DEGREES);

        //dali treba label za location??
        try {
            mFileStreamer.addRecord(timestamp, label, "loc", 4, locationValues);
        } catch (IOException e) {
            Log.d(TAG, "FILE NOT FOUND EXCEPTION OD onLocationChanged");
            throw new RuntimeException(e);
        } catch (KeyException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void start() {
        Log.i(TAG, "start:: Starting function for LocationSession ");
        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Treba da se dozvoli permisija za lokacijata");
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
        try {
            mFileStreamer.addFile("loc", "loc.csv");
            mFileStreamer.addRow("loc");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (KeyException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        Log.i(TAG,"stop:: Stopping listener for sensor ");
        try {
            mFileStreamer.endFiles();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(this);
        }
    }


     void addLabel() {
        // if(mLabelLay!=null && mLabelSit!=null && mLabelWalk!=null && mLabelStairs!=null && mLabelRun!=null) {
        if (mLabelLay.isChecked()) {
            this.label = mLabelLay.getText().toString();
            //System.out.println("Laying label is ON: " + label);
        } else if (mLabelSit.isChecked()) {
            this.label = mLabelSit.getText().toString();
            //System.out.println("Sitting label is ON: " + label);
        } else if (mLabelWalk.isChecked()) {
            this.label = mLabelWalk.getText().toString();
            //System.out.println("Walking label is ON: " + label);
        } else if (mLabelStairs.isChecked()) {
            this.label = mLabelStairs.getText().toString();
            //System.out.println("Stairs label is ON: " + label);
        } else if (mLabelRun.isChecked()) {
            this.label = mLabelRun.getText().toString();
            //System.out.println("Running label is ON: " + label);
        }else {
            this.label = "None";
        }
    }

    public void setSwitchMaterialElements(SwitchMaterial lLaying, SwitchMaterial lWalking, SwitchMaterial lRunning, SwitchMaterial lSitting, SwitchMaterial lStairs, SwitchMaterial lCustom, EditText customText) {
        this.mLabelLay = lLaying;
        this.mLabelWalk = lWalking;
        this.mLabelSit = lSitting;
        this.mLabelRun = lRunning;
        this.mLabelStairs = lStairs;
        this.mLabelCustom = lCustom;
        this.mCustomLabel = customText;
    }
}

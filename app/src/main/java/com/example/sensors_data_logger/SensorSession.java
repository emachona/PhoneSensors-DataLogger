package com.example.sensors_data_logger;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.widget.EditText;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.security.KeyException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class SensorSession implements SensorEventListener {

    private final static String LOG_TAG = SensorSession.class.getName();

    private MainActivity mContext;
    private SensorManager mSensorManager;
    private HashMap<String, Sensor> mSensors = new HashMap<>();
    private FileStreamer mFileStreamer = null;

    private AtomicBoolean mIsRecording = new AtomicBoolean(false);
    private AtomicBoolean mIsWritingFile = new AtomicBoolean(false);

    private float[] mAcceMeasure = new float[3];
    private float[] mGyroMeasure = new float[3];
    private float[] mMagnetMeasure = new float[3];

    private float[] mAcceBias = new float[3];
    private float[] mGyroBias = new float[3];
    private float[] mMagnetBias = new float[3];
    private String label;
    private SwitchMaterial mLabelLay;
    private SwitchMaterial mLabelWalk;
    private SwitchMaterial mLabelRun;
    private SwitchMaterial mLabelSit;
    private SwitchMaterial mLabelStairs;
    private SwitchMaterial mLabelCustom;
    private EditText mCustomLabel;

    /*
    Puts file names with correspondent sensors in hashmap to register
     */
    public SensorSession(MainActivity context) {
        mContext = context;

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

        mSensors.put("acce", mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        mSensors.put("acce_uncalib", mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED));
        mSensors.put("gyro", mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
        mSensors.put("gyro_uncalib", mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED));
        mSensors.put("linacce", mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
        mSensors.put("gravity", mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY));
        mSensors.put("magnet", mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        mSensors.put("magnet_uncalib", mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED));

        //Orientation sensors:
        mSensors.put("rv", mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR));
        mSensors.put("game_rv", mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR));
        mSensors.put("magnetic_rv", mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR));
         /*
         The game rotation vector does not use the magnetic field. As such relative rotations are more accurate,
         and not impacted by magnetic field changes;
         The geomagnetic rotation vector uses magnetometer instead of using a gyroscope.
         This sensor uses lower power than the other rotation vectors, because it doesn't use the gyroscope.
         However, it is more noisy and will work best outdoors.
         */


        // Environment Sensors: (should check if available on device!)
        mSensors.put("pressure", mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)); //Ambient air pressure [hPa or mbar]
        mSensors.put("temp", mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)); //Ambient air temperature [°C]
        mSensors.put("light", mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)); //Illuminance [lx]
        mSensors.put("humidity", mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)); //Ambient relative humidity [%]

        registerSensors();
    }

    /*
    Registers each sensor with given sampling rate as SENSOR_DELAY_NORMAL
    (app battery usage settings should be set to unrestricted usage for this to be followed as fixed sampling rate)
     */
    public void registerSensors() {
        for (Sensor eachSensor : mSensors.values()) {
            mSensorManager.registerListener(this, eachSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void unregisterSensors() {
        for (Sensor eachSensor : mSensors.values()) {
            mSensorManager.unregisterListener(this, eachSensor);
        }
    }

    /*
    Creates a separate csv file for every sensor - populates first row with column names
     */
    public void startSession(String streamFolder){
        if (streamFolder != null) {
            mFileStreamer = new FileStreamer(mContext, streamFolder);
            try {
                mFileStreamer.addFile("acce", "acce.csv");
                mFileStreamer.addFile("acce_uncalib", "acce_uncalib.csv");
                mFileStreamer.addFile("gyro", "gyro.csv");
                mFileStreamer.addFile("gyro_uncalib", "gyro_uncalib.csv");
                mFileStreamer.addFile("linacce", "linacce.csv");
                mFileStreamer.addFile("gravity", "gravity.csv");
                mFileStreamer.addFile("magnet", "magnet.csv");
                mFileStreamer.addFile("magnet_uncalib", "magnet_uncalib.csv");
                mFileStreamer.addFile("rv", "rv.csv");
                mFileStreamer.addFile("game_rv", "game_rv.csv");
                mFileStreamer.addFile("magnetic_rv", "magnetic_rv.csv");

                mFileStreamer.addFile("acce_bias", "acce_bias.csv");
                mFileStreamer.addFile("gyro_bias", "gyro_bias.csv");
                mFileStreamer.addFile("magnet_bias", "magnet_bias.csv");

                mFileStreamer.addFile("pressure", "pressure.csv");
                mFileStreamer.addFile("temp", "temperature.csv");
                mFileStreamer.addFile("light", "light.csv");
                mFileStreamer.addFile("humidity", "humidity.csv");

                mIsWritingFile.set(true);
            } catch (IOException e) {
                mContext.showToast("Error occurs when creating output sensor files.");
                e.printStackTrace();
            }
            try{
                mFileStreamer.addRow("acce");
                mFileStreamer.addRow("acce_uncalib");
                mFileStreamer.addRow("acce_bias");
                mFileStreamer.addRow("linacce");
                mFileStreamer.addRow("gyro");
                mFileStreamer.addRow("gyro_uncalib");
                mFileStreamer.addRow("gyro_bias");
                mFileStreamer.addRow("magnet");
                mFileStreamer.addRow("magnet_uncalib");
                mFileStreamer.addRow("magnet_bias");
                mFileStreamer.addRow("gravity");
                mFileStreamer.addRow("rv");
                mFileStreamer.addRow("game_rv");
                mFileStreamer.addRow("magnetic_rv");
                mFileStreamer.addRow("pressure");
                mFileStreamer.addRow("light");
                mFileStreamer.addRow("temp");
                mFileStreamer.addRow("humidity");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (KeyException e) {
                throw new RuntimeException(e);
            }
        }
        mIsRecording.set(true);
    }

    public void stopSession() {

        mIsRecording.set(false);
        if (mIsWritingFile.get()) {
            try {
                mFileStreamer.endFiles();
            } catch (IOException e) {
                mContext.showToast("Error occurs when finishing sensor csv files.");
                e.printStackTrace();
            }

            // copy accelerometer calibration file to the streaming folder
            try {
                File acceCalibFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/acce_calib.csv");
                File outAcceCalibFile = new File(mFileStreamer.getOutputFolder() + "/acce_calib.csv");
                if (acceCalibFile.exists()) {
                    FileInputStream istr = new FileInputStream(acceCalibFile);
                    FileOutputStream ostr = new FileOutputStream(outAcceCalibFile);
                    FileChannel ichn = istr.getChannel();
                    FileChannel ochn = ostr.getChannel();
                    ichn.transferTo(0, ichn.size(), ochn);
                    istr.close();
                    ostr.close();

                    Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    scanIntent.setData(Uri.fromFile(outAcceCalibFile));
                    mContext.sendBroadcast(scanIntent);
                }
            } catch (IOException e) {
                mContext.showToast("Error occurs when copying accelerometer calibration text files.");
                e.printStackTrace();
            }

            mIsWritingFile.set(false);
            mFileStreamer = null;
        }
    }

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {
        boolean isFileSaved = (mIsRecording.get() && mIsWritingFile.get());

        //long timestamp = sensorEvent.timestamp;
        long timestamp = System.currentTimeMillis() + (sensorEvent.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000;
        //System.nanoTime() //mnogu pozahtevna od .currentTimeMillis()

        addLabel();
        //System.out.println("Label is: " + label);
        Sensor eachSensor = sensorEvent.sensor;
        try {
            switch (eachSensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    mAcceMeasure[0] = sensorEvent.values[0];
                    mAcceMeasure[1] = sensorEvent.values[1];
                    mAcceMeasure[2] = sensorEvent.values[2];
                    if (isFileSaved) {
                        mFileStreamer.addRecord(timestamp, label, "acce", 3, sensorEvent.values);
                    }
                    break;

                case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
                    mAcceBias[0] = sensorEvent.values[3];
                    mAcceBias[1] = sensorEvent.values[4];
                    mAcceBias[2] = sensorEvent.values[5];
                    if (isFileSaved) {
                        mFileStreamer.addRecord(timestamp, label, "acce_uncalib", 3, sensorEvent.values);
                        mFileStreamer.addRecord(timestamp, label, "acce_bias", 3, mAcceBias);
                    }
                    break;

                case Sensor.TYPE_GYROSCOPE:
                    mGyroMeasure[0] = sensorEvent.values[0];
                    mGyroMeasure[1] = sensorEvent.values[1];
                    mGyroMeasure[2] = sensorEvent.values[2];
                    if (isFileSaved) {
                        mFileStreamer.addRecord(timestamp, label, "gyro", 3, sensorEvent.values);
                    }
                    break;

                case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                    mGyroBias[0] = sensorEvent.values[3];
                    mGyroBias[1] = sensorEvent.values[4];
                    mGyroBias[2] = sensorEvent.values[5];
                    if (isFileSaved) {
                        mFileStreamer.addRecord(timestamp,  label,"gyro_uncalib", 3, sensorEvent.values);
                        mFileStreamer.addRecord(timestamp, label,"gyro_bias", 3, mGyroBias);
                    }
                    break;

                case Sensor.TYPE_LINEAR_ACCELERATION:
                    if (isFileSaved) {
                        mFileStreamer.addRecord(timestamp, label, "linacce", 3, sensorEvent.values);
                    }
                    break;

                case Sensor.TYPE_GRAVITY:
                    if (isFileSaved) {
                        mFileStreamer.addRecord(timestamp, label, "gravity", 3, sensorEvent.values);
                    }
                    break;

                case Sensor.TYPE_MAGNETIC_FIELD:
                    mMagnetMeasure[0] = sensorEvent.values[0];
                    mMagnetMeasure[1] = sensorEvent.values[1];
                    mMagnetMeasure[2] = sensorEvent.values[2];

                    if (isFileSaved) {
                        mFileStreamer.addRecord(timestamp, label, "magnet", 3, sensorEvent.values);
                    }
                    break;

                case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                    mMagnetBias[0] = sensorEvent.values[3];
                    mMagnetBias[1] = sensorEvent.values[4];
                    mMagnetBias[2] = sensorEvent.values[5];
                    if (isFileSaved) {
                        mFileStreamer.addRecord(timestamp, label, "magnet_uncalib", 3, sensorEvent.values);
                        mFileStreamer.addRecord(timestamp, label, "magnet_bias", 3, mMagnetBias);
                    }
                    break;

                case Sensor.TYPE_ROTATION_VECTOR:
                    final float[] quaternionValue = new float[4];
                    SensorManager.getQuaternionFromVector(quaternionValue, sensorEvent.values);
                    if (isFileSaved) {
                        mFileStreamer.addRecord(timestamp, label, "rv", 4, quaternionValue);
                    }
                    break;

                case Sensor.TYPE_GAME_ROTATION_VECTOR:
                    if (isFileSaved) {
                        mFileStreamer.addRecord(timestamp, label,"game_rv", 4, sensorEvent.values);
                    }
                    break;

                case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                    if (isFileSaved) {
                        mFileStreamer.addRecord(timestamp, label, "magnetic_rv", 4, sensorEvent.values);
                    }
                    break;

                case Sensor.TYPE_PRESSURE:
                    if (isFileSaved) {
                        mFileStreamer.addRecord(timestamp, label, "pressure", 1, sensorEvent.values);
                    }
                case Sensor.TYPE_LIGHT:
                    if (isFileSaved) {
                        mFileStreamer.addRecord(timestamp, label, "light", 1, sensorEvent.values);
                    }
                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    if (isFileSaved) {
                        mFileStreamer.addRecord(timestamp, label, "temp", 1, sensorEvent.values);
                    }

                case Sensor.TYPE_RELATIVE_HUMIDITY:
                    if (isFileSaved) {
                        mFileStreamer.addRecord(timestamp, label, "humidity", 1, sensorEvent.values);
                    }
                    break;
            }
        } catch (IOException | KeyException e) {
            Log.d(LOG_TAG, "onSensorChanged: There is an error.");
            e.printStackTrace();
        }
    }

    /*
    Gets toggled label text as string to be written in file
    (should be fixed to check if only one label is toggled!)
     */
    private void addLabel() {
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
            }else if (mLabelCustom.isChecked()) {
                if(mCustomLabel.getText() != null){
                    this.label = mCustomLabel.getText().toString();
                    //System.out.println("Custom label is ON: " + label);
                }
            }else {
                this.label = "none";
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public boolean isRecording() {
        return mIsRecording.get();
    }

    public float[] getAcceMeasure() {
        return mAcceMeasure;
    }

    public float[] getGyroMeasure() {
        return mGyroMeasure;
    }

    public float[] getMagnetMeasure() {
        return mMagnetMeasure;
    }

    public float[] getAcceBias() {
        return mAcceBias;
    }

    public float[] getGyroBias() {
        return mGyroBias;
    }

    public float[] getMagnetBias() {
        return mMagnetBias;
    }
}
package com.example.sensors_data_logger;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.security.KeyException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity{

    private final static int REQUEST_CODE_ANDROID = 1001;
    private static String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private SensorSession mSensorSession;
    private LocationSession mLocationSession;
    private BatterySession mBatterySession;
    private Handler mHandler = new Handler();
    private AtomicBoolean mIsRecording = new AtomicBoolean(false);
    private PowerManager.WakeLock mWakeLock;
    private SwitchMaterial mLabelLaying;
    private SwitchMaterial mLabelSitting;
    private SwitchMaterial mLabelWalking;
    private SwitchMaterial mLabelStairs;
    private SwitchMaterial mLabelRunning;
    private SwitchMaterial mLabelCustom;
    private EditText mCustomLabel;
    private Button mStartStopButton;
    private TextView mLabelInterfaceTime;
    private Timer mInterfaceTimer = new Timer();
    private int mSecondCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        mSensorSession = new SensorSession(this);
        mBatterySession = new BatterySession(this);
        //sending label instances to SensorSession class
        mSensorSession.setSwitchMaterialElements(mLabelLaying, mLabelWalking, mLabelRunning, mLabelSitting, mLabelStairs, mLabelCustom, mCustomLabel);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sensors_data_logger:wakelocktag");
        mWakeLock.acquire();

        mLabelInterfaceTime.setText(R.string.ready_title);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_ANDROID);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mIsRecording.get()) {
            stopRecording();
        }
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        mSensorSession.unregisterSensors();
        super.onDestroy();
    }

    public void startStopRecording(View view){
        if (!mIsRecording.get()) {

            startRecording();

            // start interface timer on display
            mSecondCounter = 0;
            mInterfaceTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mSecondCounter += 1;
                    mLabelInterfaceTime.setText(interfaceIntTime(mSecondCounter));
                }
            }, 0, 1000);

        } else {
            stopRecording();

            // stop interface timer on display
            mInterfaceTimer.cancel();
            mLabelInterfaceTime.setText(R.string.ready_title);
        }
    }

    /*
    Creates output folder,
    calls startSession in Sensor, Location and Battery class with the created session output folder,
    isRecording set to true
     */
    private void startRecording(){
        String outputFolder = null;
        try {
            OutputDirectoryManager folder = new OutputDirectoryManager("", "em");
            outputFolder = folder.getOutputDirectory();
        } catch (IOException e) {
            showAlertAndStop("Cannot create output folder.");
            e.printStackTrace();
        }

        mSensorSession.startSession(outputFolder);

        //nanoOffset is set to 0, not needed for one device scenario
        mLocationSession=new LocationSession(this,outputFolder,"Location",0);
        mLocationSession.setSwitchMaterialElements(mLabelLaying, mLabelWalking, mLabelRunning, mLabelSitting, mLabelStairs, mLabelCustom, mCustomLabel);
        mLocationSession.start();

        mBatterySession.startSession(outputFolder);
        mIsRecording.set(true);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStartStopButton.setEnabled(true);
                mStartStopButton.setText(R.string.stop_title);
            }
        });
        showToast("Recording starts!");
    }

    protected void stopRecording() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mSensorSession.stopSession();
                mBatterySession.stopSession();
                mLocationSession.stop();
                mIsRecording.set(false);

                showToast("Recording stops!");
                resetUI();
            }
        });
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void showAlertAndStop(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(text)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                stopRecording();
                            }
                        }).show();
            }
        });
    }

    public void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStartStopButton.setEnabled(true);
                mStartStopButton.setText(R.string.start_title);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CODE_ANDROID) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                showToast("Permission not granted"+grantResult);
                finish();
                return;
            }
        }
    }

    //registers label toggle views
    private void initializeViews() {
        mLabelLaying = (SwitchMaterial) findViewById(R.id.lay);
        mLabelSitting = (SwitchMaterial) findViewById(R.id.sit);
        mLabelWalking = (SwitchMaterial) findViewById(R.id.walk);
        mLabelStairs = (SwitchMaterial) findViewById(R.id.stairs);
        mLabelRunning = (SwitchMaterial) findViewById(R.id.run);
        mLabelCustom = (SwitchMaterial) findViewById(R.id.custom);
        mCustomLabel = (EditText) findViewById(R.id.customLabel);
        mStartStopButton = (Button) findViewById(R.id.button_start_stop);
        mLabelInterfaceTime = (TextView) findViewById(R.id.label_interface_time);
    }

    private String interfaceIntTime(final int second) {
        if (second < 0) {
            showAlertAndStop("Second cannot be negative.");
        }

        // extract hour, minute, second information from second
        int input = second;
        int hours = input / 3600;
        input = input % 3600;
        int mins = input / 60;
        int secs = input % 60;

        // return interface int time
        return String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs);
    }

}
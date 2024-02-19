package com.example.sensors_data_logger;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyException;
import java.util.HashMap;
import java.util.Locale;

public class FileStreamer {

    private final static String LOG_TAG = FileStreamer.class.getName();
    private Context mContext;
    private HashMap<String, BufferedWriter> mFileWriters = new HashMap<>();
    private String mOutputFolder;

    //constructor
    public FileStreamer(Context mContext, final String mOutputFolder) {
        this.mContext = mContext;
        this.mOutputFolder = mOutputFolder;
    }

    //create a csv file in the given folder
    public void addFile(final String writerId, final String fileName) throws IOException{
        if (mFileWriters.containsKey(writerId)) {
            Log.w(LOG_TAG, "addFile: " + writerId + " already exist.");
            return;
        }
        BufferedWriter newWriter = createFile(mOutputFolder + "/" + fileName);
        mFileWriters.put(writerId, newWriter);
        Log.w(LOG_TAG, "added File: " + writerId);
    }

    private BufferedWriter createFile(final String path) throws IOException {

        File file = new File(path);
        BufferedWriter writer = new BufferedWriter((new FileWriter(file)));

        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(Uri.fromFile(file));
        mContext.sendBroadcast(scanIntent);
        return writer;
    }

    public String getOutputFolder() {
        return mOutputFolder;
    }

    public BufferedWriter getFileWriter(final String writerId) {
        return mFileWriters.get(writerId);
    }

    //populate the first row with column names in file according to the sensor values
    public void addRow(String writerId) throws KeyException, IOException {
        synchronized (this) {
            BufferedWriter writer = getFileWriter(writerId);
            StringBuilder stringBuilder = new StringBuilder();
            if (writer == null) {
                throw new KeyException("addRow: " + writerId + " not found.");
            }
            if(writerId.equals("acce") || writerId.equals("acce_uncalib") || writerId.equals("acce_bias")|| writerId.equals("linacce") ||
                    writerId.equals("gyro") || writerId.equals("gyro_uncalib") || writerId.equals("gyro_bias")||
                    writerId.equals("magnet")||writerId.equals("magnet_uncalib") || writerId.equals("magnet_bias")||
                    writerId.equals("gravity"))
            {
                stringBuilder.append("timestamp,X,Y,Z,label");
                Log.d("CsvWriter", "added first row!!");
                stringBuilder.append(" \n");
                writer.write(stringBuilder.toString());
            }else if(writerId.equals("rv") || writerId.equals("game_rv")|| writerId.equals("magnetic_rv")){
                stringBuilder.append("timestamp,X,Y,Z,scalar,label");
                Log.d("CsvWriter", "added first row!!");
                stringBuilder.append(" \n");
                writer.write(stringBuilder.toString());
            }else if(writerId.equals("pressure")){
                stringBuilder.append("timestamp,hPa(millibar),label");
                Log.d("CsvWriter", "added first row!!");
                stringBuilder.append(" \n");
                writer.write(stringBuilder.toString());
            }
            else if(writerId.equals("light")){
                stringBuilder.append("timestamp,lx,label");
                Log.d("CsvWriter", "added first row!!");
                stringBuilder.append(" \n");
                writer.write(stringBuilder.toString());
            }
            else if(writerId.equals("humidity")){
                stringBuilder.append("timestamp,%,label");
                Log.d("CsvWriter", "added first row!!");
                stringBuilder.append(" \n");
                writer.write(stringBuilder.toString());
            }
            else if(writerId.equals("temp")){
                stringBuilder.append("timestamp,°C,label");
                Log.d("CsvWriter", "added first row!!");
                stringBuilder.append(" \n");
                writer.write(stringBuilder.toString());
            }
            else if(writerId.equals("loc")){
                stringBuilder.append("timestamp,accuracy,latitude,longitude,altitude,label");
                Log.d("CsvWriter", "added first row!!");
                stringBuilder.append(" \n");
                writer.write(stringBuilder.toString());
            }
        }
    }

    /*
    Formatting the values to be written in csv
     */
    public void addRecord(final long timestamp, String label, final String writerId, final int numValues, final float[] values) throws IOException, KeyException {
        synchronized (this) {
            BufferedWriter writer = getFileWriter(writerId);
            if (writer == null) {
                throw new KeyException("addRecord: " + writerId + " not found.");
            }

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(timestamp);

            for (int i = 0; i < numValues; ++i) {
                // Separating the values with commas
                stringBuilder.append(String.format(Locale.US, ",%.6f", values[i]));
            }

            stringBuilder.append(String.format(Locale.US, ",%s", label));

            stringBuilder.append(" \n");
            writer.write(stringBuilder.toString());
        }
    }

    public void endFiles() throws IOException {
        synchronized (this) {
            for (BufferedWriter eachWriter : mFileWriters.values()) {
                eachWriter.flush();
                eachWriter.close();
            }
        }
    }
}

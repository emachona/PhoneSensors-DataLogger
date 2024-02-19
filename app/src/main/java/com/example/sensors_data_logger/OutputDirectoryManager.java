package com.example.sensors_data_logger;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class OutputDirectoryManager {
    private final static String LOG_TAG = OutputDirectoryManager.class.getName();
    private String mOutputDirectory;


    public OutputDirectoryManager(final String prefix, final String suffix) throws FileNotFoundException {
        update(prefix, suffix);
    }

    //give a unique name for new folder using timestamp
    private void update(final String prefix, final String suffix) throws FileNotFoundException {
        Calendar currentTime = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss");
        File externalDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String folderName = formatter.format(currentTime.getTime());

        if (prefix != null) {
            folderName = prefix + folderName;
        }
        if (suffix != null) {
            folderName = folderName + suffix;
        }


        File outputDirectory = new File(externalDirectory.getAbsolutePath() + "/" + folderName);
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdir()) {
                Log.e(LOG_TAG, "update: Cannot create output directory.");
                throw new FileNotFoundException();
            }
        }
        mOutputDirectory = outputDirectory.getAbsolutePath();
        Log.i(LOG_TAG, "update: Output directory: " + outputDirectory.getAbsolutePath());
    }

    public String getOutputDirectory() {
        return mOutputDirectory;
    }
}

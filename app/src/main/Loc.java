package com.example.sensors_data_logger;

        import android.Manifest;
        import android.annotation.SuppressLint;
        import android.content.Context;
        import android.os.CancellationSignal;
        import android.os.Looper;

        import androidx.annotation.RequiresPermission;

        import com.google.android.gms.location.FusedLocationProviderClient;
        import com.google.android.gms.location.LocationServices;
        import com.google.android.gms.location.LocationRequest;
        import com.google.android.gms.location.LocationResult;
        import com.google.android.gms.tasks.CancellationTokenSource;
        import com.google.android.gms.tasks.Task;

        import java.util.List;

public class Loc {

    @SuppressLint("MissingPermission")
    @RequiresPermission(
            anyOf = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}
    )
    public static void getCurrentLocation(Context context, boolean usePreciseLocation) {
        FusedLocationProviderClient locationClient =
                LocationServices.getFusedLocationProviderClient(context);

        // Check for permissions before calling this method
        // ...

        // Get last known location
        locationClient.getLastLocation()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        // Handle last known location
                        handleLocation(task.getResult());
                    } else {
                        // Handle case where last known location is not available
                        handleNoLastKnownLocation();
                    }
                });

        // Get current location with more accuracy
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(usePreciseLocation ?
                LocationRequest.PRIORITY_HIGH_ACCURACY :
                LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        Task<LocationResult> locationTask = locationClient.getCurrentLocation(
                locationRequest, cancellationTokenSource.getToken());

        locationTask.addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                // Handle current location
                handleLocation(task.getResult());
            }
        });
    }

    private static void handleLocation(LocationResult locationResult) {
        double latitude = locationResult.getLastLocation().getLatitude();
        double longitude = locationResult.getLastLocation().getLongitude();
        long timestamp = System.currentTimeMillis();

        // Handle the location information (e.g., update UI)
        String locationInfo = "Current location is \n" +
                "lat : " + latitude + "\n" +
                "long : " + longitude + "\n" +
                "fetched at " + timestamp;
        updateUI(locationInfo);
    }

    private static void handleNoLastKnownLocation() {
        // Handle case where last known location is not available (e.g., update UI)
        String locationInfo = "No last known location. Try fetching the current location first";
        updateUI(locationInfo);
    }

    private static void updateUI(String locationInfo) {
        // Update the UI with the location information
        // ...
    }
}

package com.example.myapplication;

import android.content.Intent;             // ← add this
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private MaterialButton btnSearch, btnNotifications, btnSettings;
    private Button btnAddLocation, btnAddNearestLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisierung
        btnSearch = findViewById(R.id.btn_search);
        btnNotifications = findViewById(R.id.btn_notifications);
        btnSettings = findViewById(R.id.btn_settings);
        btnAddLocation = findViewById(R.id.btn_add_location);
        btnAddNearestLocation = findViewById(R.id.btn_add_nearest_Location);

        // Klick-Listener
        Intent Search = new Intent(this, SearchActivity.class);

        btnSearch.setOnClickListener(v -> startActivity(Search));
        btnAddLocation.setOnClickListener(v -> startActivity(Search));

        btnNotifications.setOnClickListener(v -> {
            // TODO: Benachrichtigungen anzeigen
        });
        btnSettings.setOnClickListener(v -> {
            // TODO: Profil anzeigen
        });

        btnAddNearestLocation.setOnClickListener(v -> {

            // In your MainActivity or wherever you call getAirQuality:
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        // Permission is not granted, request it
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_LOCATION_PERMISSION);
                    } else {
                    // Permission already granted, proceed with location logic
                    getNearestLocation();
                    }
                    // This runs in a background thread
                    int result = AirQuality.getAirQuality(47.076668, 15.421371);
                    System.out.println(result);
                    // If you need to update the UI, use runOnUiThread:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Update your UI here with 'result'
                        }
                    });
                }
            }).start();

        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with location logic
                getNearestLocation();
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "GPS permission is required to add the nearest location.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getNearestLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Toast.makeText(MainActivity.this, "Lat: " + latitude + ", Lon: " + longitude, Toast.LENGTH_LONG).show();
                // You can use latitude and longitude here as needed
            }
            @Override
            public void onStatusChanged(String provider, int status, android.os.Bundle extras) {}
            @Override
            public void onProviderEnabled(String provider) {}
            @Override
            public void onProviderDisabled(String provider) {
                Toast.makeText(MainActivity.this, "Please enable GPS", Toast.LENGTH_SHORT).show();
            }
        }, Looper.getMainLooper());
    }
}

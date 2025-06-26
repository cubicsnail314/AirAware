package com.example.myapplication;

import android.content.Intent;             // ← add this
import android.os.Bundle;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import com.google.android.material.button.MaterialButton;

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
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        btnAddNearestLocation.setOnClickListener(v -> {
            // Get current location and air quality
            getPermission();
        });
    }

    private void getPermission() {
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
            }
        }).start();
    }

    private void getAirQualityForNearestLocation(final double lat, final double lon) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // This runs in a background thread
                AirQualityResult result = AirQuality.getAirQualityWithDetails(lat, lon);
                System.out.println("Location-based AQI: " + result.aqi + ", City: " + result.city + ", Country: " + result.country);
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(MainActivity.this, ActiveLocationActivity.class);
                        intent.putExtra("city", result.city);
                        intent.putExtra("country", result.country);
                        intent.putExtra("stationName", result.stationName);
                        intent.putExtra("aqi", result.aqi);
                        intent.putExtra("forecastDates", result.forecastDates);
                        intent.putExtra("forecastAqi", result.forecastAqi);
                        startActivity(intent);
                    }
                });
            }
        }).start();
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
        
        // Check permission before accessing location services
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Location permission not granted", Toast.LENGTH_SHORT).show());
            return;
        }
        
        // Try network location first (faster), then GPS
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            System.out.println("Trying Network provider for faster location...");
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Getting network location...", Toast.LENGTH_SHORT).show());
            requestLocation(locationManager, LocationManager.NETWORK_PROVIDER);
        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            System.out.println("Trying GPS provider...");
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Getting GPS location...", Toast.LENGTH_SHORT).show());
            requestLocation(locationManager, LocationManager.GPS_PROVIDER);
        } else {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "No location providers available", Toast.LENGTH_SHORT).show());
        }
    }
    
    private void requestLocation(LocationManager locationManager, String provider) {
        // Check permission before requesting location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            locationManager.requestSingleUpdate(provider, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    System.out.println("Location received from " + provider + ": Lat=" + latitude + ", Lon=" + longitude);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Lat: " + latitude + ", Lon: " + longitude, Toast.LENGTH_LONG).show();
                        }
                    });
                    getAirQualityForNearestLocation(latitude, longitude);
                }

                @Override
                public void onStatusChanged(String provider, int status, android.os.Bundle extras) {
                    System.out.println("Location Status changed for " + provider + ": " + status);
                }

                @Override
                public void onProviderEnabled(String provider) {
                    System.out.println("Location Provider enabled: " + provider);
                }

                @Override
                public void onProviderDisabled(String provider) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Location provider disabled", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }, Looper.getMainLooper());
        } catch (SecurityException e) {
            System.out.println("SecurityException when requesting location: " + e.getMessage());
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}

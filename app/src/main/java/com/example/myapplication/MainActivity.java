package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import android.view.View;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import androidx.work.Data;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import com.example.myapplication.AqiCheckWorker;
import android.content.SharedPreferences;
import java.util.concurrent.TimeUnit;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Log;
import android.content.Context;

public class MainActivity extends AppCompatActivity implements SavedLocationsAdapter.OnLocationClickListener {

    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private MaterialButton btnSearch, btnNotifications, btnSettings, btnPin;
    private Button btnAddLocation, btnAddNearestLocation;
    private RecyclerView recyclerViewLocations;
    private View bigButtonsContainer;
    private SavedLocationsAdapter adapter;
    private ExecutorService executorService;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(MyApplication.updateLanguageContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyApplication.updateLanguage(this);
        setContentView(R.layout.activity_main);

        executorService = Executors.newSingleThreadExecutor();

        // Initialize permission launcher
        notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, getString(R.string.notifications_allowed), Toast.LENGTH_SHORT).show();
                    // Reschedule worker with new permission
                    rescheduleWorkerIfNeeded();
                } else {
                    Toast.makeText(this, getString(R.string.notifications_require_permission), Toast.LENGTH_LONG).show();
                }
            }
        );

        // Schedule AQI background worker if notifications are enabled
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", false);
        if (notificationsEnabled) {
            scheduleAqiWorkerWithPermissionCheck();
        }

        // Initialize views
        initializeViews();
        setupClickListeners();
        loadSavedLocations();
    }

    private void initializeViews() {
        btnSearch = findViewById(R.id.btn_search);
        btnNotifications = findViewById(R.id.btn_notifications);
        btnSettings = findViewById(R.id.btn_settings);
        btnPin = findViewById(R.id.btn_pin);
        btnAddLocation = findViewById(R.id.btn_add_location);
        btnAddNearestLocation = findViewById(R.id.btn_add_nearest_Location);
        recyclerViewLocations = findViewById(R.id.recycler_view_locations);
        bigButtonsContainer = findViewById(R.id.big_buttons_container);

        // Setup RecyclerView
        recyclerViewLocations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SavedLocationsAdapter(null, this);
        recyclerViewLocations.setAdapter(adapter);
    }

    private void setupClickListeners() {
        Intent Search = new Intent(this, SearchActivity.class);

        btnSearch.setOnClickListener(v -> startActivity(Search));
        btnAddLocation.setOnClickListener(v -> startActivity(Search));

        btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
            startActivity(intent);
        });
        
        // Add long press on notifications button to test notifications
        btnNotifications.setOnLongClickListener(v -> {
            testNotification();
            return true;
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
        btnPin.setOnClickListener(v -> {
            getPermissionAndLocation();
        });

        btnAddNearestLocation.setOnClickListener(v -> {
            // Get current location and air quality
            getPermissionAndLocation();
        });
    }

    private void loadSavedLocations() {
        executorService.execute(() -> {
            try {
                List<LocationEntity> locations = DatabaseClient.getInstance(this)
                        .getAppDatabase()
                        .locationDao()
                        .getAllLocations();
                
                runOnUiThread(() -> {
                    if (locations != null && !locations.isEmpty()) {
                        // Show saved locations
                        adapter.updateLocations(locations);
                        recyclerViewLocations.setVisibility(View.VISIBLE);
                        bigButtonsContainer.setVisibility(View.GONE);
                    } else {
                        // Show big buttons
                        recyclerViewLocations.setVisibility(View.GONE);
                        bigButtonsContainer.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.error_loading_locations), Toast.LENGTH_SHORT).show();
                    // Show big buttons as fallback
                    recyclerViewLocations.setVisibility(View.GONE);
                    bigButtonsContainer.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload locations when returning to the activity
        loadSavedLocations();
        
        // Reschedule worker if settings changed
        rescheduleWorkerIfNeeded();
    }
    
    private void rescheduleWorkerIfNeeded() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", false);
        
        if (notificationsEnabled) {
            // Cancel existing work first
            WorkManager.getInstance(this).cancelUniqueWork("aqi_check");
            
            // Check if location permission is granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted, safe to access location
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            Data inputData = new Data.Builder()
                                .putDouble("latitude", location.getLatitude())
                                .putDouble("longitude", location.getLongitude())
                                .build();

                            // Use shorter interval for testing (5 minutes instead of 30)
                            PeriodicWorkRequest aqiCheckRequest =
                                new PeriodicWorkRequest.Builder(AqiCheckWorker.class, 5, TimeUnit.MINUTES)
                                    .setInputData(inputData)
                                    .build();

                            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                                "aqi_check",
                                ExistingPeriodicWorkPolicy.REPLACE,
                                aqiCheckRequest
                            );
                            
                            Log.d("MainActivity", "Worker scheduled with location: " + location.getLatitude() + ", " + location.getLongitude());
                        } else {
                            Log.e("MainActivity", "No location available for worker");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("MainActivity", "Failed to get location for worker", e);
                    });
            } else {
                // Permission is not granted, log this but don't crash
                Log.w("MainActivity", "Location permission not granted, cannot schedule AQI worker");
            }
        } else {
            // Cancel worker if notifications are disabled
            WorkManager.getInstance(this).cancelUniqueWork("aqi_check");
            Log.d("MainActivity", "Worker cancelled - notifications disabled");
        }
    }

    @Override
    public void onLocationClick(LocationEntity location) {
        // Get air quality data for the selected location
        executorService.execute(() -> {
            AirQualityResult result = AirQualityAPI.getAirQualityWithDetails(location.latitude, location.longitude);
            runOnUiThread(() -> {
                Intent intent = new Intent(MainActivity.this, ActiveLocationActivity.class);
                intent.putExtra("city", result.city);
                intent.putExtra("country", result.country);
                intent.putExtra("stationName", result.stationName);
                intent.putExtra("aqi", result.aqi);
                intent.putExtra("forecastDates", result.forecastDates);
                intent.putExtra("forecastAqi", result.forecastAqi);
                // Always include correct coordinates
                intent.putExtra("latitude", location.latitude);
                intent.putExtra("longitude", location.longitude);
                startActivity(intent);
            });
        });
    }

    @Override
    public void onLocationDelete(LocationEntity location) {
        executorService.execute(() -> {
            DatabaseClient.getInstance(this)
                    .getAppDatabase()
                    .locationDao()
                    .deleteLocation(location);

            runOnUiThread(() -> {
                Toast.makeText(this, getString(R.string.location_deleted), Toast.LENGTH_SHORT).show();
                loadSavedLocations(); // Reload the list
            });
        });
    }

    private void getPermissionAndLocation() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted, request it
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
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
                AirQualityResult result = AirQualityAPI.getAirQualityWithDetails(lat, lon);
                System.out.println("Location-based AQI: " + result.aqi + ", City: " + result.city + ", Country: " + result.country);
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setAqiColor(btnAddLocation, result.aqi);
                        Intent intent = new Intent(MainActivity.this, ActiveLocationActivity.class);
                        intent.putExtra("longitude", lon);
                        intent.putExtra("latitude", lat);
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
                Toast.makeText(this, getString(R.string.gps_permission_required_to_add_nearest_location), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getNearestLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        // Check permission before accessing location services
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, getString(R.string.location_permission_not_granted), Toast.LENGTH_SHORT).show());
            return;
        }
        
        // Try network location first (faster), then GPS
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            System.out.println("Trying Network provider for faster location...");
            runOnUiThread(() -> Toast.makeText(MainActivity.this, getString(R.string.getting_network_location), Toast.LENGTH_SHORT).show());
            requestLocation(locationManager, LocationManager.NETWORK_PROVIDER);
        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            System.out.println("Trying GPS provider...");
            runOnUiThread(() -> Toast.makeText(MainActivity.this, getString(R.string.getting_gps_location), Toast.LENGTH_SHORT).show());
            requestLocation(locationManager, LocationManager.GPS_PROVIDER);
        } else {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, getString(R.string.no_location_providers_available), Toast.LENGTH_SHORT).show());
        }
    }
    
    private void requestLocation(LocationManager locationManager, String provider) {
        // Check permission before requesting location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.location_permission_not_granted), Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(MainActivity.this, getString(R.string.lat) + ": " + latitude + ", " + getString(R.string.lon) + ": " + longitude, Toast.LENGTH_LONG).show();
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
                            Toast.makeText(MainActivity.this, getString(R.string.location_provider_disabled), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }, Looper.getMainLooper());
        } catch (SecurityException e) {
            System.out.println("SecurityException when requesting location: " + e.getMessage());
            Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private void setAqiColor(Button button, int aqi) {
        GradientDrawable gradient = new GradientDrawable();
        gradient.setShape(GradientDrawable.RECTANGLE);
        gradient.setCornerRadius(16);

        if (aqi <= 50) {
            gradient.setColor(Color.parseColor("#00E400")); // Green
        } else if (aqi <= 100) {
            gradient.setColor(Color.parseColor("#FFFF00")); // Yellow
        } else if (aqi <= 150) {
            gradient.setColor(Color.parseColor("#FF7E00")); // Orange
        } else if (aqi <= 200) {
            gradient.setColor(Color.parseColor("#FF0000")); // Red
        } else if (aqi <= 300) {
            gradient.setColor(Color.parseColor("#8F3F97")); // Purple
        } else {
            gradient.setColor(Color.parseColor("#7E0023")); // Maroon
        }

        button.setBackground(gradient);
    }
    
    private void testNotification() {
        Log.d("MainActivity", "Testing notification...");
        
        // Check if we have notification permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.notifications_require_permission_to_test), Toast.LENGTH_LONG).show();
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }
        
        // Send simple test notification
        NotificationActivity.addNotification(this, getString(R.string.test_notification), getString(R.string.test_notification));
        
        Toast.makeText(this, getString(R.string.test_notification_sent), Toast.LENGTH_SHORT).show();
        Log.d("MainActivity", "Test notification sent");
    }

    private void scheduleAqiWorkerWithPermissionCheck() {
        // Check if location permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, safe to access location
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        Data inputData = new Data.Builder()
                            .putDouble("latitude", location.getLatitude())
                            .putDouble("longitude", location.getLongitude())
                            .build();

                        PeriodicWorkRequest aqiCheckRequest =
                            new PeriodicWorkRequest.Builder(AqiCheckWorker.class, 30, TimeUnit.MINUTES)
                                .setInputData(inputData)
                                .build();

                        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                            "aqi_check",
                            ExistingPeriodicWorkPolicy.REPLACE,
                            aqiCheckRequest
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure, e.g., log or show a message
                    Log.e("MainActivity", "Failed to get location for AQI worker", e);
                });
        } else {
            // Permission is not granted, log this but don't crash
            Log.w("MainActivity", "Location permission not granted, cannot schedule AQI worker");
        }
    }
}

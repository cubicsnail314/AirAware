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
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SavedLocationsAdapter.OnLocationClickListener {

    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private MaterialButton btnSearch, btnNotifications, btnSettings, btnPin;
    private Button btnAddLocation, btnAddNearestLocation;
    private RecyclerView recyclerViewLocations;
    private TextView tvEmptyState;
    private View bigButtonsContainer;
    private SavedLocationsAdapter adapter;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        executorService = Executors.newSingleThreadExecutor();

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
        tvEmptyState = findViewById(R.id.tv_empty_state);
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
            // TODO: Benachrichtigungen anzeigen
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
                    Toast.makeText(this, "Error loading locations", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Standort gelöscht", Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

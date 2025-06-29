package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.content.SharedPreferences;

public class ActiveLocationActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(MyApplication.updateLanguageContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyApplication.updateLanguage(this);
        setContentView(R.layout.activity_nearest_location_result);

        // Set up toolbar with back button
        MaterialToolbar toolbar = findViewById(R.id.toolbar_active_location);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvCity = findViewById(R.id.city_name);
        TextView tvCountry = findViewById(R.id.tv_country);
        TextView tvStation = findViewById(R.id.tv_station);
        TextView tvAqi = findViewById(R.id.tv_aqi);
        TextView tvAqiDescription = findViewById(R.id.tv_aqi_description);
        TextView tvAqiLabel = findViewById(R.id.tv_aqi_label);

        String city = getIntent().getStringExtra("city");
        String country = getIntent().getStringExtra("country");
        String stationName = getIntent().getStringExtra("stationName");
        double latitude = roundCoord(getIntent().getDoubleExtra("latitude", 0.0));
        double longitude = roundCoord(getIntent().getDoubleExtra("longitude", 0.0));
        int aqi = getIntent().getIntExtra("aqi", -1);
        String[] forecastDates = getIntent().getStringArrayExtra("forecastDates");
        int[] forecastAqi = getIntent().getIntArrayExtra("forecastAqi");

        tvCity.setText(city);
        tvCountry.setText(country);

        String cleanStationName = stationName;

        tvStation.setText(getString(R.string.station) + " " + cleanStationName);
        tvAqi.setText(String.valueOf(aqi));
        tvAqiDescription.setText(getAqiDescription(aqi));
        tvAqi.setShadowLayer(4, 0, 0, Color.BLACK);
        tvAqiLabel.setShadowLayer(4, 0, 0, Color.BLACK);
        tvAqiDescription.setShadowLayer(4, 0, 0, Color.BLACK);
        
        // Set AQI color
        LinearLayout layoutAqiBackground = findViewById(R.id.layout_aqi_background);
        setAqiColor(layoutAqiBackground, aqi);

        // Set up forecast
        if (forecastDates != null && forecastAqi != null) {
            setupForecast(forecastDates, forecastAqi);
        }

        // Normalize location entity for DB operations
        LocationEntity normalizedLoc = LocationEntity.normalize(new LocationEntity(stationName, city, longitude, latitude));
        // Hide plus button if location is already saved
        MaterialButton btnPlus = findViewById(R.id.btn_plus);
        new Thread(() -> {
            boolean exists = DatabaseClient.getInstance(this)
                .getAppDatabase()
                .locationDao()
                .checkLocationExists(normalizedLoc.name) > 0;
            runOnUiThread(() -> {
                if (exists) {
                    btnPlus.setVisibility(android.view.View.GONE);
                } else {
                    btnPlus.setVisibility(android.view.View.VISIBLE);
                }
            });
        }).start();

        // Set up button listener
        findViewById(R.id.btn_plus).setOnClickListener(v -> {
            // Add immediate feedback
            android.widget.Toast.makeText(ActiveLocationActivity.this, "Checking location...", android.widget.Toast.LENGTH_SHORT).show();
            
            // Save current station to database
            LocationEntity location = LocationEntity.normalize(new LocationEntity(stationName, city, longitude, latitude));
            new Thread(() -> {
                try {
                    // Use the transaction method to check and insert atomically
                    boolean wasInserted = DatabaseClient.getInstance(ActiveLocationActivity.this)
                            .getAppDatabase()
                            .locationDao()
                            .insertIfNotExists(location);
                    
                    runOnUiThread(() -> {
                        if (wasInserted) {
                            android.widget.Toast.makeText(ActiveLocationActivity.this, "Location saved: " + stationName, android.widget.Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else {
                            android.widget.Toast.makeText(ActiveLocationActivity.this, "Location already saved: " + stationName, android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        android.widget.Toast.makeText(ActiveLocationActivity.this, "Error saving location", android.widget.Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });
    }

    private void setAqiColor(View view, int aqi) {
        int color;
        if (aqi <= 50) {
            color = Color.parseColor("#4CAF50"); // Green
        } else if (aqi <= 100) {
            color = Color.parseColor("#FFEB3B"); // Yellow
        } else if (aqi <= 150) {
            color = Color.parseColor("#FF9800"); // Orange
        } else if (aqi <= 200) {
            color = Color.parseColor("#F44336"); // Red
        } else if (aqi <= 300) {
            color = Color.parseColor("#9C27B0"); // Purple
        } else {
            color = Color.parseColor("#8D6E63"); // Maroon
        }
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(16);
        background.setColor(color);
        view.setBackground(background);
    }

    private void setupForecast(String[] dates, int[] aqiValues) {
        TextView tvDate1 = findViewById(R.id.tv_forecast_date1);
        TextView tvDate2 = findViewById(R.id.tv_forecast_date2);
        TextView tvDate3 = findViewById(R.id.tv_forecast_date3);
        TextView tvAqi1 = findViewById(R.id.tv_forecast_aqi1);
        TextView tvAqi2 = findViewById(R.id.tv_forecast_aqi2);
        TextView tvAqi3 = findViewById(R.id.tv_forecast_aqi3);

        // If fewer than 3 valid dates, show 'No forecast' in all fields
        if (dates.length < 3 || dates[0] == null || dates[1] == null || dates[2] == null) {
            String noForecast = getString(R.string.no_forecast);
            tvDate1.setText(noForecast);
            tvDate2.setText(noForecast);
            tvDate3.setText(noForecast);
            tvAqi1.setText("");
            tvAqi2.setText("");
            tvAqi3.setText("");
            return;
        }

        if (dates.length > 0 && aqiValues.length > 0 && dates[0] != null) {
            tvDate1.setText(formatDate(dates[0]));
            tvAqi1.setText(String.valueOf(aqiValues[0]));
            setAqiColor(tvAqi1, aqiValues[0]);
            tvAqi1.setShadowLayer(4, 0, 0, Color.BLACK);
        }

        if (dates.length > 1 && aqiValues.length > 1 && dates[1] != null) {
            tvDate2.setText(formatDate(dates[1]));
            tvAqi2.setText(String.valueOf(aqiValues[1]));
            setAqiColor(tvAqi2, aqiValues[1]);
            tvAqi2.setShadowLayer(4, 0, 0, Color.BLACK);
        }

        if (dates.length > 2 && aqiValues.length > 2 && dates[2] != null) {
            tvDate3.setText(formatDate(dates[2]));
            tvAqi3.setText(String.valueOf(aqiValues[2]));
            setAqiColor(tvAqi3, aqiValues[2]);
            tvAqi3.setShadowLayer(4, 0, 0, Color.BLACK);
        }
    }

    private String formatDate(String dateStr) {
        try {
            // Get the app's language setting
            SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
            String language = prefs.getString("language", "de");
            Locale appLocale = new Locale(language);
            
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd", appLocale);
            
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String getAqiDescription(int aqi) {
        if (aqi <= 50) {
            return getString(R.string.aqi_good);
        } else if (aqi <= 100) {
            return getString(R.string.aqi_moderate);
        } else if (aqi <= 150) {
            return getString(R.string.aqi_unhealthy_sensitive);
        } else if (aqi <= 200) {
            return getString(R.string.aqi_unhealthy);
        } else if (aqi <= 300) {
            return getString(R.string.aqi_very_unhealthy);
        } else {
            return getString(R.string.aqi_hazardous);
        }
    }

    // Utility method to round coordinates
    public static double roundCoord(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
} 
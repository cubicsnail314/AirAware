package com.example.myapplication;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ActiveLocationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearest_location_result);

        TextView tvCity = findViewById(R.id.tv_city);
        TextView tvCountry = findViewById(R.id.tv_country);
        TextView tvStation = findViewById(R.id.tv_station);
        TextView tvAqi = findViewById(R.id.tv_aqi);
        TextView tvAqiDescription = findViewById(R.id.tv_aqi_description);

        String city = getIntent().getStringExtra("city");
        String country = getIntent().getStringExtra("country");
        String stationName = getIntent().getStringExtra("stationName");
        int aqi = getIntent().getIntExtra("aqi", -1);
        String[] forecastDates = getIntent().getStringArrayExtra("forecastDates");
        int[] forecastAqi = getIntent().getIntArrayExtra("forecastAqi");

        tvCity.setText(city);
        tvCountry.setText(country);
        
        // Clean up station name - remove ", Austria" if it exists
        String cleanStationName = stationName;
        if (cleanStationName != null && cleanStationName.endsWith(", Austria")) {
            cleanStationName = cleanStationName.substring(0, cleanStationName.length() - 9);
        }
        tvStation.setText("Station: " + cleanStationName);
        tvAqi.setText(String.valueOf(aqi));
        tvAqiDescription.setText(getAqiDescription(aqi));
        
        // Set AQI color
        setAqiColor(tvAqi, aqi);

        // Set up forecast
        if (forecastDates != null && forecastAqi != null) {
            setupForecast(forecastDates, forecastAqi);
        }

        // Set up top button listeners
        findViewById(R.id.btn_search).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, SearchActivity.class));
        });
        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, SettingsActivity.class));
        });
        findViewById(R.id.btn_notifications).setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "TODO: Benachrichtigungen anzeigen", android.widget.Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btn_plus).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, SearchActivity.class));
        });
    }

    private void setAqiColor(TextView textView, int aqi) {
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
        textView.setBackground(background);
    }

    private void setupForecast(String[] dates, int[] aqiValues) {
        TextView tvDate1 = findViewById(R.id.tv_forecast_date1);
        TextView tvDate2 = findViewById(R.id.tv_forecast_date2);
        TextView tvDate3 = findViewById(R.id.tv_forecast_date3);
        TextView tvAqi1 = findViewById(R.id.tv_forecast_aqi1);
        TextView tvAqi2 = findViewById(R.id.tv_forecast_aqi2);
        TextView tvAqi3 = findViewById(R.id.tv_forecast_aqi3);

        if (dates.length > 0 && aqiValues.length > 0) {
            tvDate1.setText(formatDate(dates[0]));
            tvAqi1.setText(String.valueOf(aqiValues[0]));
            setAqiColor(tvAqi1, aqiValues[0]);
        }

        if (dates.length > 1 && aqiValues.length > 1) {
            tvDate2.setText(formatDate(dates[1]));
            tvAqi2.setText(String.valueOf(aqiValues[1]));
            setAqiColor(tvAqi2, aqiValues[1]);
        }

        if (dates.length > 2 && aqiValues.length > 2) {
            tvDate3.setText(formatDate(dates[2]));
            tvAqi3.setText(String.valueOf(aqiValues[2]));
            setAqiColor(tvAqi3, aqiValues[2]);
        }
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String getAqiDescription(int aqi) {
        if (aqi <= 50) {
            return "Good";
        } else if (aqi <= 100) {
            return "Moderate";
        } else if (aqi <= 150) {
            return "Unhealthy for Sensitive Groups";
        } else if (aqi <= 200) {
            return "Unhealthy";
        } else if (aqi <= 300) {
            return "Very Unhealthy";
        } else {
            return "Hazardous";
        }
    }
} 
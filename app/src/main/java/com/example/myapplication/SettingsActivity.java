package com.example.myapplication;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "settings";
    private static final String KEY_THRESHOLD = "aqi_threshold";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_AQI_TYPE = "aqi_type";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";

    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.settings_title));
        }

        EditText editThreshold = findViewById(R.id.edit_threshold);
        RadioGroup radioLanguage = findViewById(R.id.radio_language);
        RadioGroup radioAqiType = findViewById(R.id.radio_aqi_type);
        Switch switchNotifications = findViewById(R.id.switch_notifications);

        // Enforce minimum value 0 for Grenzwert
        editThreshold.setFilters(new InputFilter[]{
            new InputFilterMinMax(0, Integer.MAX_VALUE)
        });

        // Select all text when the box is focused
        editThreshold.setSelectAllOnFocus(true);

        // Load saved settings
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int threshold = prefs.getInt(KEY_THRESHOLD, 100);
        String language = prefs.getString(KEY_LANGUAGE, "de");
        String aqiType = prefs.getString(KEY_AQI_TYPE, "usa");
        boolean notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, false);

        editThreshold.setText(String.valueOf(threshold));
        switchNotifications.setChecked(notificationsEnabled);

        // Set language radio
        ((RadioButton) findViewById(language.equals("de") ? R.id.radio_de : R.id.radio_en)).setChecked(true);

        // Set AQI type radio
        ((RadioButton) findViewById(aqiType.equals("usa") ? R.id.radio_aqi_usa : R.id.radio_aqi_wien)).setChecked(true);

        // Initialize permission launcher
        notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, getString(R.string.notifications_enabled), Toast.LENGTH_SHORT).show();
                } else {
                    // Permission denied, uncheck the switch
                    switchNotifications.setChecked(false);
                    prefs.edit().putBoolean(KEY_NOTIFICATIONS, false).apply();
                    Toast.makeText(this, getString(R.string.notifications_required), Toast.LENGTH_LONG).show();
                }
            }
        );

        // Save threshold on change
        editThreshold.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                int value = 0;
                try { value = Integer.parseInt(s.toString()); } catch (Exception ignored) {}
                prefs.edit().putInt(KEY_THRESHOLD, value).apply();
            }
        });

        // Save language on change
        radioLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            String lang = (checkedId == R.id.radio_de) ? "de" : "en";
            prefs.edit().putString(KEY_LANGUAGE, lang).apply();
        });

        // Save AQI type on change
        radioAqiType.setOnCheckedChangeListener((group, checkedId) -> {
            String type = (checkedId == R.id.radio_aqi_usa) ? "usa" : "wien";
            prefs.edit().putString(KEY_AQI_TYPE, type).apply();
        });

        // Save notifications switch on change
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Check if we need to request notification permission (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                            != PackageManager.PERMISSION_GRANTED) {
                        // Request permission
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        return; // Don't save yet, wait for permission result
                    }
                }
            }
            // Save the setting
            prefs.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply();
            
            // Show feedback
            if (isChecked) {
                Toast.makeText(this, getString(R.string.notifications_enabled), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.notifications_disabled), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper class to enforce min/max in EditText
    public static class InputFilterMinMax implements InputFilter {
        private int min, max;
        public InputFilterMinMax(int min, int max) {
            this.min = min;
            this.max = max;
        }
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            try {
                String newVal = dest.toString().substring(0, dstart) + source + dest.toString().substring(dend);
                int input = newVal.isEmpty() ? 0 : Integer.parseInt(newVal);
                if (isInRange(min, max, input))
                    return null;
            } catch (NumberFormatException nfe) { }
            return "";
        }
        private boolean isInRange(int a, int b, int c) {
            return b > a ? c >= a && c <= b : c >= b && c <= a;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
} 
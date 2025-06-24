package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class StartActivity2 extends AppCompatActivity {
    private static final String PREFS = "app_prefs";
    private static final String KEY_FIRST_RUN = "first_run";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_2);

        // “Nicht jetzt” → MainActivity AND mark first_run = false
        Button btnSkip = findViewById(R.id.btn_skip);
        btnSkip.setOnClickListener(v -> {
            // save that we have shown the onboarding
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_FIRST_RUN, false)
                    .apply();


            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

    }
}

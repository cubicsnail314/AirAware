package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class StartActivity1 extends AppCompatActivity {
    private static final String PREFS = "app_prefs";
    private static final String KEY_FIRST_RUN = "first_run";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ► if it's NOT the first run, go straight to MainActivity
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        if (!prefs.getBoolean(KEY_FIRST_RUN, true)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_start_1);

        // “Nicht jetzt” → StartActivity2
        Button btnSkip = findViewById(R.id.btn_skip);
        btnSkip.setOnClickListener(v -> {
            startActivity(new Intent(this, StartActivity2.class));
            finish();
        });

        // (optional) hook up your “Meine Stadt orten” button here...
    }
}

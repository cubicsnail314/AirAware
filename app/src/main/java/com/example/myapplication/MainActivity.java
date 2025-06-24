package com.example.myapplication;

import android.content.Intent;             // ← add this
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.material.button.MaterialButton;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private MaterialButton btnSearch, btnNotifications, btnSettings;
    private Button btnAddLocation, btnAddNearestLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisierung
        btnSearch             = findViewById(R.id.btn_search);
        btnNotifications      = findViewById(R.id.btn_notifications);
        btnSettings           = findViewById(R.id.btn_settings);
        btnAddLocation        = findViewById(R.id.btn_add_location);
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
            // TODO: Nächsten Ort hinzufügen
        });
    }
}

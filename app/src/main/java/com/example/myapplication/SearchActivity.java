package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import com.google.android.material.appbar.MaterialToolbar;

public class SearchActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // 1) Set up our custom Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar_search);
        setSupportActionBar(toolbar);
        // show the nav arrow
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // hide default title
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // 2) Back-arrow behavior: finish() returns us to MainActivity
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                finish();
            }
        });

        // 3) Wire up the SearchView
        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                // TODO: perform your search
                return true;
            }
            @Override public boolean onQueryTextChange(String newText) {
                // TODO: live-filter as user types
                return true;
            }
        });
    }
}

package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchActivity extends AppCompatActivity {
    private SearchResultAdapter adapter;
    private ArrayList<StationSearchResult> searchResults = new ArrayList<>();

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
        ExecutorService executor = Executors.newSingleThreadExecutor();
        RecyclerView recyclerView = findViewById(R.id.recycler_search_results);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchResultAdapter();
        recyclerView.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                executor.execute(() -> {
                    List<StationSearchResult> stationSearchResults = AirQualityAPI.getStationsFromSearch(query);
                    runOnUiThread(() -> {
                        searchResults.clear();
                        if (stationSearchResults != null && !stationSearchResults.isEmpty()) {
                            searchResults.addAll(stationSearchResults);
                        }
                        adapter.notifyDataSetChanged();
                    });
                });
                return true;
            }
            @Override public boolean onQueryTextChange(String newText) {
                // TODO: live-filter as user types
                return true;
            }
        });
    }

    private class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_search_result, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            StationSearchResult searchResult = searchResults.get(position);
            
            // Clean up station name - remove ", Austria" if it exists
            String cleanStationName = searchResult.stationName;
            if (cleanStationName != null && cleanStationName.endsWith(", Austria")) {
                cleanStationName = cleanStationName.substring(0, cleanStationName.length() - 9);
            }
            
            holder.tvStationName.setText(cleanStationName);
            
            holder.itemView.setOnClickListener(v -> {
                // Navigate to ActiveLocationActivity with the station data
                ExecutorService clickExecutor = Executors.newSingleThreadExecutor();
                clickExecutor.execute(() -> {
                    // Get air quality data for this specific station using its coordinates
                    AirQualityResult airQualityResult = AirQualityAPI.getAirQualityWithDetails(searchResult.longitude, searchResult.latitude);
                    runOnUiThread(() -> {
                        if (airQualityResult != null) {
                            android.content.Intent intent = new android.content.Intent(SearchActivity.this, ActiveLocationActivity.class);
                            intent.putExtra("city", airQualityResult.city);
                            intent.putExtra("country", airQualityResult.country);
                            intent.putExtra("stationName", searchResult.stationName);
                            intent.putExtra("aqi", airQualityResult.aqi);
                            intent.putExtra("forecastDates", airQualityResult.forecastDates);
                            intent.putExtra("forecastAqi", airQualityResult.forecastAqi);
                            startActivity(intent);
                        } else {
                            Toast.makeText(SearchActivity.this, "No air quality data available for this station", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            });
        }
        
        @Override
        public int getItemCount() {
            return searchResults.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvStationName;
            
            ViewHolder(View itemView) {
                super(itemView);
                tvStationName = itemView.findViewById(R.id.tv_station_name);
            }
        }
    }
}

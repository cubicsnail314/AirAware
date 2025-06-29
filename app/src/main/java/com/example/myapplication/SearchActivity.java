package com.example.myapplication;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
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
import android.widget.ImageButton;
import android.util.Log;

public class SearchActivity extends AppCompatActivity {
    private SearchResultAdapter adapter;
    private ArrayList<StationSearchResult> searchResults = new ArrayList<>();

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(MyApplication.updateLanguageContext(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyApplication.updateLanguage(this);
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
            String cleanStationName = searchResult.stationName;
            holder.tvStationName.setText(cleanStationName);
            
            // Add click listener for plus button to save station to database
            holder.btnPlus.setOnClickListener(v -> {
                // Add immediate feedback
                Toast.makeText(SearchActivity.this, getString(R.string.checking_location), Toast.LENGTH_SHORT).show();
                // Normalize and round before saving
                LocationEntity location = LocationEntity.normalize(new LocationEntity(searchResult.stationName, "", searchResult.longitude, searchResult.latitude));
                Log.d("SearchActivity", "[DEBUG] Saving location: name=" + location.name + ", lat=" + location.latitude + ", lon=" + location.longitude);
                ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
                dbExecutor.execute(() -> {
                    try {
                        // Use the transaction method to check and insert atomically
                        boolean wasInserted = DatabaseClient.getInstance(SearchActivity.this)
                                .getAppDatabase()
                                .locationDao()
                                .insertIfNotExists(location);
                        
                        runOnUiThread(() -> {
                            if (wasInserted) {
                                Toast.makeText(SearchActivity.this, getString(R.string.location_saved) + searchResult.stationName, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(SearchActivity.this, getString(R.string.location_already_saved) + searchResult.stationName, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(SearchActivity.this, getString(R.string.error_saving_location), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
                dbExecutor.shutdown();
            });
            
            holder.itemView.setOnClickListener(v -> {
                // Navigate to ActiveLocationActivity with the station data
                ExecutorService clickExecutor = Executors.newSingleThreadExecutor();
                clickExecutor.execute(() -> {
                    // Get air quality data for this specific station using its coordinates
                    AirQualityResult airQualityResult = AirQualityAPI.getAirQualityWithDetails(searchResult.latitude, searchResult.longitude);
                    runOnUiThread(() -> {
                        if (airQualityResult != null) {
                            android.content.Intent intent = new android.content.Intent(SearchActivity.this, ActiveLocationActivity.class);
                            intent.putExtra("city", airQualityResult.city);
                            intent.putExtra("country", airQualityResult.country);
                            intent.putExtra("stationName", searchResult.stationName);
                            intent.putExtra("latitude", searchResult.latitude);
                            intent.putExtra("longitude", searchResult.longitude);
                            intent.putExtra("aqi", airQualityResult.aqi);
                            intent.putExtra("forecastDates", airQualityResult.forecastDates);
                            intent.putExtra("forecastAqi", airQualityResult.forecastAqi);
                            startActivity(intent);
                        } else {
                            Toast.makeText(SearchActivity.this, getString(R.string.no_air_quality_data_available_for_this_station), Toast.LENGTH_SHORT).show();
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
            ImageButton btnPlus;
            
            ViewHolder(View itemView) {
                super(itemView);
                tvStationName = itemView.findViewById(R.id.tv_station_name);
                btnPlus = itemView.findViewById(R.id.btn_plus);
            }
        }
    }
}

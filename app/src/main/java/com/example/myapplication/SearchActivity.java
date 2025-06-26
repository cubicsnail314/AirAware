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
            TextView tv = (TextView) LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(tv);
        }
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String result = searchResults.get(position).stationName;
            holder.textView.setText(result);
            holder.textView.setOnClickListener(v -> {
                Toast.makeText(SearchActivity.this, "Selected: " + result, Toast.LENGTH_SHORT).show();
                // TODO: handle selection (e.g., return result, open details, etc.)
            });
        }
        @Override
        public int getItemCount() {
            return searchResults.size();
        }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(TextView itemView) {
                super(itemView);
                textView = itemView;
            }
        }
    }
}

package com.example.myapplication;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SavedLocationsAdapter extends RecyclerView.Adapter<SavedLocationsAdapter.ViewHolder> {

    private List<LocationEntity> locations;
    private OnLocationClickListener listener;
    private ExecutorService executorService;

    public interface OnLocationClickListener {
        void onLocationClick(LocationEntity location);
        void onLocationDelete(LocationEntity location);
    }

    public SavedLocationsAdapter(List<LocationEntity> locations, OnLocationClickListener listener) {
        this.locations = locations;
        this.listener = listener;
        this.executorService = Executors.newCachedThreadPool();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_location, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocationEntity location = locations.get(position);
        holder.stationName.setText(location.name);
        holder.city.setText(location.city);
        holder.aqiNumber.setText("--");
        executorService.execute(() -> {
            try {
                AirQualityResult result = AirQualityAPI.getAirQualityWithDetails(location.latitude, location.longitude);
                holder.itemView.post(() -> {
                    if (result != null && result.aqi > 0) {
                        holder.aqiNumber.setText(String.valueOf(result.aqi));
                        setAqiColor(holder.aqiNumber, result.aqi);
                        holder.aqiNumber.setShadowLayer(2, 0, 0, Color.BLACK);
                    } else {
                        holder.aqiNumber.setText("--");
                    }
                });
            } catch (Exception e) {
                holder.itemView.post(() -> {
                    holder.aqiNumber.setText("--");
                });
            }
        });

        holder.btnDeleteLocation.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLocationDelete(location);
            }
        });
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLocationClick(location);
            }
        });
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    public void updateLocations(List<LocationEntity> newLocations) {
        this.locations = newLocations;
        notifyDataSetChanged();
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
        background.setCornerRadius(8);
        background.setColor(color);
        view.setBackground(background);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView stationName, aqiNumber, city;
        ImageButton btnDeleteLocation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            city = itemView.findViewById(R.id.city_name);
            stationName = itemView.findViewById(R.id.station_name);
            aqiNumber = itemView.findViewById(R.id.aqi_number);
            btnDeleteLocation = itemView.findViewById(R.id.btn_delete_location);
        }
    }
} 
package com.example.myapplication;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

        // Parse station name and country from location.name
        String stationName = location.name;
        String country = "";
        int lastComma = stationName.lastIndexOf(",");
        if (lastComma != -1) {
            country = stationName.substring(lastComma + 1).trim();
            stationName = stationName.substring(0, lastComma).trim();
        }

        if (!country.isEmpty()) {
            holder.city.setText(country.substring(0, 1).toUpperCase() + country.substring(1).toLowerCase());
        } else {
            holder.city.setText("");
        }
        holder.stationName.setText(stationName);

        // Show loading state initially
        holder.aqiNumber.setText("--");

        // Fetch AQI data for this location
        executorService.execute(() -> {
            try {
                AirQualityResult result = AirQualityAPI.getAirQualityWithDetails(location.latitude, location.longitude);
                holder.itemView.post(() -> {
                    if (result != null && result.aqi > 0) {
                        SharedPreferences prefs = holder.itemView.getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
                        String aqiType = prefs.getString("aqi_type", "us");
                        if ("wien".equals(aqiType)) {
                            AqiUtils.WienerAqiInfo info = AqiUtils.getWienerAqiInfo(holder.itemView.getContext(), result.aqi);
                            holder.aqiNumber.setText(String.valueOf(info.index));
                            setAqiColor(holder.aqiNumber, info.color, true);
                            holder.aqiNumber.setTextColor(Color.WHITE);
                        } else {
                            holder.aqiNumber.setText(String.valueOf(result.aqi));
                            setAqiColor(holder.aqiNumber, result.aqi, false);
                            holder.aqiNumber.setTextColor(Color.WHITE);
                        }
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

    private void setAqiColor(View view, int colorOrAqi, boolean useDirectColor) {
        int color = colorOrAqi;
        if (!useDirectColor) {
            if (colorOrAqi <= 50) {
                color = Color.parseColor("#4CAF50"); // Green
            } else if (colorOrAqi <= 100) {
                color = Color.parseColor("#FFEB3B"); // Yellow
            } else if (colorOrAqi <= 150) {
                color = Color.parseColor("#FF9800"); // Orange
            } else if (colorOrAqi <= 200) {
                color = Color.parseColor("#F44336"); // Red
            } else if (colorOrAqi <= 300) {
                color = Color.parseColor("#9C27B0"); // Purple
            } else if (colorOrAqi > 300) {
                color = Color.parseColor("#8D6E63"); // Maroon
            }
        }
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(8);
        background.setColor(color);
        view.setBackground(background);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView city, stationName, aqiNumber;
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
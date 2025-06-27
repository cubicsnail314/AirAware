package com.example.myapplication;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

    public interface OnLocationClickListener {
        void onLocationClick(LocationEntity location);
        void onLocationDelete(LocationEntity location);
    }

    public SavedLocationsAdapter(List<LocationEntity> locations, OnLocationClickListener listener) {
        this.locations = locations;
        this.listener = listener;
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
        
        holder.tvLocationName.setText(location.name);
        holder.tvLocationCoordinates.setText(String.format("Lat: %.4f, Lon: %.4f", 
                location.latitude, location.longitude));


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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocationName, tvLocationCoordinates;
        ImageButton btnViewLocation, btnDeleteLocation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocationName = itemView.findViewById(R.id.tv_location_name);
            tvLocationCoordinates = itemView.findViewById(R.id.tv_location_coordinates);
            btnDeleteLocation = itemView.findViewById(R.id.btn_delete_location);
        }
    }
} 
package com.example.myapplication;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "locations", 
        indices = {@androidx.room.Index(value = {"name", "latitude", "longitude"}, unique = true)})
public class LocationEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public double longitude;
    public double latitude;

    public LocationEntity(String name, double longitude, double latitude) {
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public static LocationEntity normalize(LocationEntity loc) {
        String normalizedName = loc.name.trim().toLowerCase();
        double roundedLat = Math.round(loc.latitude * 10000.0) / 10000.0;
        double roundedLon = Math.round(loc.longitude * 10000.0) / 10000.0;
        return new LocationEntity(normalizedName, roundedLon, roundedLat);
    }
} 
package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;

@Dao
public interface LocationDao {
    @Insert
    void insert(LocationEntity location);

    @Query("SELECT * FROM locations")
    List<LocationEntity> getAllLocations();

    @Delete
    void deleteLocation(LocationEntity location);

    @Query("SELECT COUNT(*) FROM locations WHERE name = :name COLLATE NOCASE")
    int checkLocationExists(String name);

    @Query("SELECT COUNT(*) FROM locations WHERE ABS(latitude - :lat) < :margin AND ABS(longitude - :lon) < :margin")
    int checkLocationExistsByLatLon(double lat, double lon, double margin);
    
    @Transaction
    default boolean insertIfNotExists(LocationEntity location) {
        double margin = 0.001; // ~100 meters
        int existingCount = checkLocationExistsByLatLon(location.latitude, location.longitude, margin);
        if (existingCount == 0) {
            insert(location);
            return true; // Successfully inserted
        }
        return false; // Already exists
    }
} 
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

    @Query("SELECT COUNT(*) FROM locations WHERE name = :name AND latitude = :latitude AND longitude = :longitude")
    int checkLocationExists(String name, double latitude, double longitude);
    
    @Transaction
    default boolean insertIfNotExists(LocationEntity location) {
        int existingCount = checkLocationExists(location.name, location.latitude, location.longitude);
        if (existingCount == 0) {
            insert(location);
            return true; // Successfully inserted
        }
        return false; // Already exists
    }
} 
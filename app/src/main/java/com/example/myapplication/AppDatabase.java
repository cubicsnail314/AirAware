package com.example.myapplication;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {LocationEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract LocationDao locationDao();
    
    // Migration from version 1 to 2: Clear old data and start fresh
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            try {
                // Drop the old table completely
                database.execSQL("DROP TABLE IF EXISTS locations");
                
                // Create new table with exact schema Room expects
                database.execSQL("CREATE TABLE locations (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT, " +  // Note: name is nullable (NOT NULL = false)
                        "latitude REAL NOT NULL, " +
                        "longitude REAL NOT NULL)");
                
                // Add the unique index separately
                database.execSQL("CREATE UNIQUE INDEX index_locations_name_latitude_longitude ON locations (name, latitude, longitude)");
                
            } catch (Exception e) {
                // If migration fails, try to create the table anyway
                database.execSQL("DROP TABLE IF EXISTS locations");
                database.execSQL("CREATE TABLE locations (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT, " +
                        "latitude REAL NOT NULL, " +
                        "longitude REAL NOT NULL)");
                database.execSQL("CREATE UNIQUE INDEX index_locations_name_latitude_longitude ON locations (name, latitude, longitude)");
            }
        }
    };
} 
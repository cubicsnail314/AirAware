package com.example.myapplication;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class MyApplication extends Application {
    
    private static float latestLux = 0;
    public static float getLatestLux() { return latestLux; }
    public static void setLatestLux(float lux) { latestLux = lux; }
    
    @Override
    public void onCreate() {
        super.onCreate();
        updateLanguage(this);
    }
    
    public static void updateLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String language = prefs.getString("language", "de");
        
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        
        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
    
    public static Context updateLanguageContext(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String language = prefs.getString("language", "de");
        
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        
        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        
        return context.createConfigurationContext(config);
    }
} 
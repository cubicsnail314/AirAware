package com.example.myapplication;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.location.Priority;

public class AqiCheckWorker extends Worker {
    private static final String TAG = "AqiCheckWorker";
    private static final String CHANNEL_ID = "aqi_notifications";
    private static final int NOTIFICATION_ID = 1;
    private static final String PREF_LAST_NOTIFICATION_THRESHOLD = "last_notification_threshold";
    private static final String PREF_LAST_NOTIFICATION_TIME = "last_notification_time";
    private static final long NOTIFICATION_COOLDOWN_HOURS = 24; // 24 hours cooldown

    public AqiCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting AQI check worker");

        // Get fresh location using FusedLocationProviderClient
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        double latitude = 0.0;
        double longitude = 0.0;
        try {
            Location location = Tasks.await(
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null),
                10, TimeUnit.SECONDS
            );
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            } else {
                Log.e(TAG, "No valid location provided");
                return Result.retry();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get location", e);
            return Result.retry();
        }

        Log.d(TAG, "Worker location: " + latitude + ", " + longitude);

        // Check if notifications are enabled
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", false);
        
        Log.d(TAG, "Notifications enabled: " + notificationsEnabled);
        
        if (!notificationsEnabled) {
            Log.d(TAG, "Notifications are disabled");
            return Result.success();
        }

        // Get AQI threshold
        int aqiThreshold = prefs.getInt("aqi_threshold", 100);
        int aqiThresholdOutside = prefs.getInt("aqi_threshold_outside", 0);
        float lux = MyApplication.getLatestLux();
        if (lux >= 5000 && aqiThresholdOutside > 0) {
            aqiThreshold = aqiThresholdOutside;
            Log.d(TAG, "Using outside threshold: " + aqiThreshold);
        } else {
            Log.d(TAG, "Using regular threshold: " + aqiThreshold);
        }

        // Get AQI type (US or EU)
        String aqiType = prefs.getString("aqi_type", "us");
        Log.d(TAG, "AQI type: " + aqiType);

        // Make API call to get AQI
        checkAQI(latitude, longitude, aqiThreshold, aqiType);

        return Result.success();
    }

    private void checkAQI(double latitude, double longitude, int threshold, String aqiType) {
        String url = String.format("https://api.waqi.info/feed/geo:%.6f;%.6f/?token=YOUR_TOKEN_HERE", latitude, longitude);
        
        // Use the actual API token
        url = url.replace("YOUR_TOKEN_HERE", "a216862100a39d6530de9a623889e273588fab3f");
        
        Log.d(TAG, "Making API call to: " + url);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "API call failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d(TAG, "API response code: " + response.code());
                
                if (!response.isSuccessful()) {
                    Log.e(TAG, "API response not successful: " + response.code());
                    return;
                }

                String responseBody = response.body().string();
                Log.d(TAG, "API response body: " + responseBody.substring(0, Math.min(200, responseBody.length())) + "...");
                
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    
                    if (jsonResponse.getString("status").equals("ok")) {
                        JSONObject data = jsonResponse.getJSONObject("data");
                        int aqi = data.getInt("aqi");
                        
                        Log.d(TAG, "Current AQI: " + aqi + ", Threshold: " + threshold);
                        
                        // Check if AQI exceeds threshold
                        if (aqi > threshold) {
                            Log.d(TAG, "AQI " + aqi + " exceeds threshold " + threshold + " - checking if notification should be sent");
                            
                            // Check if we should send notification (avoid duplicates)
                            if (shouldSendNotification(threshold)) {
                                Log.d(TAG, "Sending notification for AQI: " + aqi);
                                
                                // Get location name
                                JSONObject city = data.getJSONObject("city");
                                String locationName = city.getString("name");
                                
                                // Send notification
                                sendNotification(aqi, locationName, threshold);
                                
                                // Mark that notification was sent for this threshold
                                markNotificationSent(threshold);
                            } else {
                                Log.d(TAG, "Notification already sent for threshold " + threshold + " within cooldown period");
                            }
                        } else {
                            Log.d(TAG, "AQI " + aqi + " does not exceed threshold " + threshold + " - no notification");
                            
                            // If AQI is now below threshold, reset the notification tracking
                            // so that future exceedances will trigger notifications again
                            resetNotificationTrackingIfBelowThreshold(threshold);
                        }
                    } else {
                        Log.e(TAG, "API returned error status: " + jsonResponse.getString("status"));
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON response", e);
                }
            }
        });
    }

    private boolean shouldSendNotification(int threshold) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        int lastThreshold = prefs.getInt(PREF_LAST_NOTIFICATION_THRESHOLD, -1);
        long lastNotificationTime = prefs.getLong(PREF_LAST_NOTIFICATION_TIME, 0);
        long currentTime = System.currentTimeMillis();
        
        // If this is a different threshold or enough time has passed, send notification
        if (lastThreshold != threshold || 
            (currentTime - lastNotificationTime) > (NOTIFICATION_COOLDOWN_HOURS * 60 * 60 * 1000)) {
            return true;
        }
        
        return false;
    }

    private void markNotificationSent(int threshold) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_LAST_NOTIFICATION_THRESHOLD, threshold);
        editor.putLong(PREF_LAST_NOTIFICATION_TIME, System.currentTimeMillis());
        editor.apply();
        
        Log.d(TAG, "Marked notification sent for threshold: " + threshold);
    }

    private void resetNotificationTrackingIfBelowThreshold(int threshold) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        int lastThreshold = prefs.getInt(PREF_LAST_NOTIFICATION_THRESHOLD, -1);
        
        // If we previously sent a notification for this threshold and AQI is now below it,
        // reset the tracking so future exceedances will trigger notifications
        if (lastThreshold == threshold) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(PREF_LAST_NOTIFICATION_THRESHOLD);
            editor.remove(PREF_LAST_NOTIFICATION_TIME);
            editor.apply();
            
            Log.d(TAG, "Reset notification tracking - AQI now below threshold: " + threshold);
        }
    }

    private void sendNotification(int aqi, String locationName, int threshold) {
        Context context = getApplicationContext();
        
        // Check if we have permission to post notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted, cannot send notification");
                return;
            }
        }
        
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "AQI Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for high AQI levels");
            notificationManager.createNotificationChannel(channel);
        }

        String title = context.getString(R.string.air_quality_warning);
        String content = context.getString(R.string.air_quality_warning_message);

        // Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
        
        // Save notification to history
        NotificationActivity.addNotification(context, title, content);
        
        Log.d(TAG, "Notification sent for AQI: " + aqi);
    }
} 
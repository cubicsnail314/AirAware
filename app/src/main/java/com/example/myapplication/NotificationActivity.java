package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "notification_history";
    private static final String KEY_NOTIFICATIONS = "notifications";
    
    private LinearLayout notificationContainer;
    private Button clearButton;
    private List<NotificationItem> notificationList;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Initialize Gson for JSON serialization
        gson = new Gson();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setContentInsetStartWithNavigation(0);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle("Benachrichtigungen");
        }
        
        // Initialize views
        notificationContainer = findViewById(R.id.notification_container);
        clearButton = findViewById(R.id.clear_button);

        // Load notifications
        loadNotifications();

        // Setup clear button
        clearButton.setOnClickListener(v -> clearAllNotifications());

        // Display notifications
        displayNotifications();
    }

    private void loadNotifications() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String notificationsJson = prefs.getString(KEY_NOTIFICATIONS, "[]");
        
        Type type = new TypeToken<ArrayList<NotificationItem>>(){}.getType();
        notificationList = gson.fromJson(notificationsJson, type);
        
        if (notificationList == null) {
            notificationList = new ArrayList<>();
        }
    }

    private void displayNotifications() {
        notificationContainer.removeAllViews();

        if (notificationList.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Keine Benachrichtigungen vorhanden");
            emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            emptyText.setPadding(0, 100, 0, 0);
            notificationContainer.addView(emptyText);
            clearButton.setVisibility(View.GONE);
        } else {
            clearButton.setVisibility(View.VISIBLE);
            
            for (int i = notificationList.size() - 1; i >= 0; i--) {
                NotificationItem item = notificationList.get(i);
                View notificationView = createNotificationView(item);
                notificationContainer.addView(notificationView);
            }
        }
    }

    private View createNotificationView(NotificationItem item) {
        // Create container
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 30, 50, 30);
        container.setBackgroundResource(R.drawable.notification_background);

        // Create title
        TextView titleText = new TextView(this);
        titleText.setText(item.getTitle());
        titleText.setTextSize(18);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        titleText.setTextColor(getResources().getColor(android.R.color.black));

        // Create content
        TextView contentText = new TextView(this);
        contentText.setText(item.getContent());
        contentText.setTextSize(14);
        contentText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        contentText.setPadding(0, 10, 0, 0);

        // Create timestamp
        TextView timeText = new TextView(this);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN);
        timeText.setText(sdf.format(new Date(item.getTimestamp())));
        timeText.setTextSize(12);
        timeText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        timeText.setPadding(0, 10, 0, 0);

        // Add views to container
        container.addView(titleText);
        container.addView(contentText);
        container.addView(timeText);

        // Add margin between notifications
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 20);
        container.setLayoutParams(params);

        return container;
    }

    private void clearAllNotifications() {
        new AlertDialog.Builder(this)
            .setTitle("Benachrichtigungen löschen")
            .setMessage("Möchten Sie wirklich alle Benachrichtigungen löschen?")
            .setPositiveButton("Löschen", (dialog, which) -> {
                notificationList.clear();
                saveNotifications();
                displayNotifications();
            })
            .setNegativeButton("Abbrechen", null)
            .show();
    }

    private void saveNotifications() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String notificationsJson = gson.toJson(notificationList);
        prefs.edit().putString(KEY_NOTIFICATIONS, notificationsJson).apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Static method to add a new notification (called from AqiCheckWorker)
    public static void addNotification(NotificationActivity activity, String title, String content) {
        if (activity != null) {
            NotificationItem newItem = new NotificationItem(title, content, System.currentTimeMillis());
            activity.notificationList.add(newItem);
            activity.saveNotifications();
            activity.displayNotifications();
        }
    }

    // Static method to add a new notification (called from other activities)
    public static void addNotification(Context context, String title, String content) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String notificationsJson = prefs.getString(KEY_NOTIFICATIONS, "[]");
        
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<NotificationItem>>(){}.getType();
        List<NotificationItem> notifications = gson.fromJson(notificationsJson, type);
        
        if (notifications == null) {
            notifications = new ArrayList<>();
        }
        
        notifications.add(new NotificationItem(title, content, System.currentTimeMillis()));
        
        String newJson = gson.toJson(notifications);
        prefs.edit().putString(KEY_NOTIFICATIONS, newJson).apply();
    }

    // Inner class to represent a notification item
    public static class NotificationItem {
        private String title;
        private String content;
        private long timestamp;

        public NotificationItem(String title, String content, long timestamp) {
            this.title = title;
            this.content = content;
            this.timestamp = timestamp;
        }

        public String getTitle() { return title; }
        public String getContent() { return content; }
        public long getTimestamp() { return timestamp; }
    }
} 
package com.aiassistant.scheduler.executor.handlers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import utils.ActionCallback;
import com.aiassistant.scheduler.executor.handlers.ActionHandler;
import utils.ActionResult;
import utils.ActionStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for sending notifications
 */
public class NotificationHandler implements ActionHandler {
    private static final String TAG = "NotificationHandler";
    
    // Constants
    private static final String CHANNEL_ID = "aiassistant_notifications";
    private static final String CHANNEL_NAME = "AI Assistant Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications from AI Assistant";
    
    // Context
    private final Context context;
    
    // Notification ID counter
    private int notificationId = 1;
    
    /**
     * Create a new notification handler
     */
    public NotificationHandler(Context context) {
        this.context = context;
        
        // Create notification channel for Android O and above
        createNotificationChannel();
    }
    
    /**
     * Create notification channel for Android O and above
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            
            // Register the channel with the system
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    @Override
    public boolean executeAction(Map<String, Object> params) {
        // Extract parameters
        String title = getStringParam(params, "title", "AI Assistant");
        String message = getStringParam(params, "message", "");
        String channelId = getStringParam(params, "channelId", CHANNEL_ID);
        boolean ongoing = getBooleanParam(params, "ongoing", false);
        boolean silent = getBooleanParam(params, "silent", false);
        int priority = getIntParam(params, "priority", NotificationCompat.PRIORITY_DEFAULT);
        
        try {
            // Create notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority)
                .setOngoing(ongoing);
            
            if (silent) {
                builder.setSilent(true);
            }
            
            // Get notification manager
            NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Send notification
            int id = notificationId++;
            notificationManager.notify(id, builder.build());
            
            Log.d(TAG, "Sent notification: " + title);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending notification: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get a string parameter
     */
    private String getStringParam(Map<String, Object> parameters, String key, String defaultValue) {
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }
    
    /**
     * Get a boolean parameter
     */
    private boolean getBooleanParam(Map<String, Object> parameters, String key, boolean defaultValue) {
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }
    
    /**
     * Get an integer parameter
     */
    private int getIntParam(Map<String, Object> parameters, String key, int defaultValue) {
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    @Override
    public void executeAction(Map<String, Object> parameters, ActionCallback callback) {
        // Extract parameters
        String title = getStringParam(parameters, "title", "AI Assistant");
        String message = getStringParam(parameters, "message", "");
        String channelId = getStringParam(parameters, "channelId", CHANNEL_ID);
        boolean ongoing = getBooleanParam(parameters, "ongoing", false);
        boolean silent = getBooleanParam(parameters, "silent", false);
        int priority = getIntParam(parameters, "priority", NotificationCompat.PRIORITY_DEFAULT);
        
        try {
            // Create notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority)
                .setOngoing(ongoing);
            
            if (silent) {
                builder.setSilent(true);
            }
            
            // Get notification manager
            NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Send notification
            int id = notificationId++;
            notificationManager.notify(id, builder.build());
            
            Log.d(TAG, "Sent notification: " + title);
            
            // Prepare result
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("notificationId", id);
            result.put("title", title);
            result.put("message", message);
            
            // Return success
            callback.onComplete(result);
        } catch (Exception e) {
            Log.e(TAG, "Error sending notification: " + e.getMessage(), e);
            callback.onError("Failed to send notification: " + e.getMessage());
        }
    }
    
    @Override
    public void cancel() {
        // Nothing to cancel for notifications
        Log.d(TAG, "Cancel called, but notifications can't be cancelled through this method");
    }
    
    @Override
    public String getHandlerType() {
        return "notification";
    }
}
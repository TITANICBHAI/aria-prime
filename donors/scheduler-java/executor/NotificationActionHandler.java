package com.aiassistant.scheduler.executor;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handler for showing notifications as scheduled actions
 */
public class NotificationActionHandler implements ActionHandler {
    
    private static final String TAG = "NotificationHandler";
    private static final String DEFAULT_CHANNEL_ID = "com.aiassistant.notification.default";
    private static final String DEFAULT_CHANNEL_NAME = "Default Notifications";
    
    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final NotificationManager notificationManager;
    private final Random random;
    
    /**
     * Constructor
     * 
     * @param context Android context
     */
    public NotificationActionHandler(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.random = new Random();
        
        // Create default notification channel for Android O and above
        createDefaultNotificationChannel();
    }
    
    /**
     * Create default notification channel for Android Oreo and above
     */
    private void createDefaultNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    DEFAULT_CHANNEL_ID,
                    DEFAULT_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.enableVibration(true);
            channel.setDescription("Default notification channel for AI Assistant");
            
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    @Override
    public boolean executeAction(Map<String, Object> params) {
        try {
            // Extract parameters
            String title = (String) params.get("title");
            String message = (String) params.get("message");
            String channelId = (String) params.get("channelId");
            Integer priority = (Integer) params.get("priority");
            Boolean autoCancel = (Boolean) params.get("autoCancel");
            String actionPackage = (String) params.get("actionPackage");
            String actionClass = (String) params.get("actionClass");
            Integer notificationId = (Integer) params.get("notificationId");
            
            // Validate parameters
            if (title == null || title.isEmpty()) {
                Log.e(TAG, "Notification title is required");
                return false;
            }
            
            if (message == null) {
                message = ""; // Empty message is allowed
            }
            
            if (channelId == null || channelId.isEmpty()) {
                channelId = DEFAULT_CHANNEL_ID;
            }
            
            if (priority == null) {
                priority = NotificationCompat.PRIORITY_DEFAULT;
            }
            
            if (autoCancel == null) {
                autoCancel = true;
            }
            
            if (notificationId == null) {
                // Generate random notification ID if not provided
                notificationId = random.nextInt(100000);
            }
            
            // Create notification builder
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(priority)
                    .setAutoCancel(autoCancel);
            
            // Add action intent if specified
            if (actionPackage != null && !actionPackage.isEmpty() && 
                    actionClass != null && !actionClass.isEmpty()) {
                try {
                    Intent intent = new Intent();
                    intent.setClassName(actionPackage, actionClass);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId,
                            intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    
                    builder.setContentIntent(pendingIntent);
                } catch (Exception e) {
                    Log.e(TAG, "Error creating action intent", e);
                }
            }
            
            // Show notification
            notificationManager.notify(notificationId, builder.build());
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification", e);
            return false;
        }
    }
    
    @Override
    public void executeAction(Map<String, Object> parameters, ActionCallback callback) {
        // Extract parameters
        String title = (String) parameters.get("title");
        String message = (String) parameters.get("message");
        String channelId = (String) parameters.get("channelId");
        Integer priority = (Integer) parameters.get("priority");
        Boolean autoCancel = (Boolean) parameters.get("autoCancel");
        String actionPackage = (String) parameters.get("actionPackage");
        String actionClass = (String) parameters.get("actionClass");
        Integer notificationId = (Integer) parameters.get("notificationId");
        
        // Validate parameters
        if (title == null || title.isEmpty()) {
            callback.onError("Notification title is required");
            return;
        }
        
        if (message == null) {
            message = ""; // Empty message is allowed
        }
        
        if (channelId == null || channelId.isEmpty()) {
            channelId = DEFAULT_CHANNEL_ID;
        }
        
        if (priority == null) {
            priority = NotificationCompat.PRIORITY_DEFAULT;
        }
        
        if (autoCancel == null) {
            autoCancel = true;
        }
        
        if (notificationId == null) {
            // Generate random notification ID if not provided
            notificationId = random.nextInt(100000);
        }
        
        // Execute notification in background
        final String finalMessage = message;
        final String finalChannelId = channelId;
        final int finalPriority = priority;
        final boolean finalAutoCancel = autoCancel;
        final int finalNotificationId = notificationId;
        
        executorService.execute(() -> {
            try {
                // Create notification builder
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, finalChannelId)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(finalMessage)
                        .setPriority(finalPriority)
                        .setAutoCancel(finalAutoCancel);
                
                // Add action intent if specified
                if (actionPackage != null && !actionPackage.isEmpty() && 
                        actionClass != null && !actionClass.isEmpty()) {
                    try {
                        Intent intent = new Intent();
                        intent.setClassName(actionPackage, actionClass);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        
                        PendingIntent pendingIntent = PendingIntent.getActivity(context, finalNotificationId,
                                intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        
                        builder.setContentIntent(pendingIntent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating action intent", e);
                    }
                }
                
                // Show notification
                notificationManager.notify(finalNotificationId, builder.build());
                
                // Prepare result
                final Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("notificationId", finalNotificationId);
                
                // Return result on main thread
                mainHandler.post(() -> callback.onComplete(result));
                
            } catch (Exception e) {
                Log.e(TAG, "Error showing notification", e);
                final String errorMessage = e.getMessage();
                mainHandler.post(() -> callback.onError(errorMessage));
            }
        });
    }
    
    @Override
    public void cancel() {
        // Not much to cancel here, notifications are shown immediately
    }
    
    @Override
    public String getHandlerType() {
        return "notification";
    }
}
package com.aiassistant.scheduler.executor.handlers;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import utils.ActionCallback;
import com.aiassistant.scheduler.executor.handlers.ActionHandler;
import utils.ActionResult;
import utils.ActionStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handler for controlling applications
 */
public class AppControlHandler implements ActionHandler {
    
    private static final String TAG = "AppControlHandler";
    
    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private boolean isCancelled = false;
    
    /**
     * Create a new app control handler
     */
    public AppControlHandler(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    @Override
    public boolean executeAction(Map<String, Object> params) {
        try {
            // Extract parameters
            String action = (String) params.get("action");
            String packageName = (String) params.get("packageName");
            
            // Validate parameters
            if (action == null || action.isEmpty()) {
                Log.e(TAG, "App control action is required");
                return false;
            }
            
            if (packageName == null || packageName.isEmpty()) {
                Log.e(TAG, "Package name is required");
                return false;
            }
            
            // Execute action based on type
            switch (action) {
                case "launch":
                    return launchApp(packageName, (String) params.get("className"), (String) params.get("action"));
                case "stop":
                    return stopApp(packageName);
                case "sendIntent":
                    return sendIntent(packageName, (String) params.get("intentAction"), 
                            (String) params.get("uri"), (Map<String, Object>) params.get("extras"));
                default:
                    Log.e(TAG, "Unsupported app control action: " + action);
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "App control error: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void executeAction(Map<String, Object> parameters, ActionCallback callback) {
        executorService.submit(() -> {
            try {
                if (isCancelled) {
                    callback.onError("Action was cancelled");
                    return;
                }
                
                // Extract parameters
                String action = (String) parameters.get("action");
                String packageName = (String) parameters.get("packageName");
                
                // Validate parameters
                if (action == null || action.isEmpty()) {
                    callback.onError("App control action is required");
                    return;
                }
                
                if (packageName == null || packageName.isEmpty()) {
                    callback.onError("Package name is required");
                    return;
                }
                
                // Execute action based on type
                boolean success = false;
                Map<String, Object> result = new HashMap<>();
                result.put("action", action);
                result.put("packageName", packageName);
                
                switch (action) {
                    case "launch":
                        success = launchApp(packageName, (String) parameters.get("className"), 
                                (String) parameters.get("action"));
                        break;
                    case "stop":
                        success = stopApp(packageName);
                        break;
                    case "sendIntent":
                        success = sendIntent(packageName, (String) parameters.get("intentAction"), 
                                (String) parameters.get("uri"), (Map<String, Object>) parameters.get("extras"));
                        break;
                    default:
                        callback.onError("Unsupported app control action: " + action);
                        return;
                }
                
                // Return result
                if (success) {
                    result.put("status", "success");
                    callback.onComplete(result);
                } else {
                    callback.onError("Failed to execute app control action: " + action);
                }
                
            } catch (Exception e) {
                callback.onError("App control error: " + e.getMessage());
            }
        });
    }
    
    @Override
    public void cancel() {
        isCancelled = true;
    }
    
    @Override
    public String getHandlerType() {
        return "APP_CONTROL";
    }
    
    /**
     * Launch an application
     * 
     * @param packageName Package name
     * @param className Optional component class name
     * @param action Optional intent action
     * @return true if app launched successfully
     */
    private boolean launchApp(String packageName, String className, String action) {
        try {
            // Create launch intent
            Intent intent;
            
            if (className != null && !className.isEmpty()) {
                // Launch specific component
                intent = new Intent();
                intent.setClassName(packageName, className);
            } else {
                // Get launch intent for package
                PackageManager pm = context.getPackageManager();
                intent = pm.getLaunchIntentForPackage(packageName);
                
                if (intent == null) {
                    Log.e(TAG, "No launch intent found for package: " + packageName);
                    return false;
                }
            }
            
            // Set action if provided
            if (action != null && !action.isEmpty()) {
                intent.setAction(action);
            }
            
            // Add flags
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // Launch app
            mainHandler.post(() -> context.startActivity(intent));
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching app: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Stop an application
     * 
     * @param packageName Package name
     * @return true if operation was successful
     */
    private boolean stopApp(String packageName) {
        try {
            // This is a limited implementation since Android doesn't easily allow
            // stopping other apps without special permissions
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            mainHandler.post(() -> context.startActivity(intent));
            
            // Note: This doesn't actually stop the app, it just returns to home screen
            // For actual app stopping, you would need to use ActivityManager with FORCE_STOP_PACKAGES
            // permission, which requires system app privileges
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error stopping app: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Send an intent to an application
     * 
     * @param packageName Target package name
     * @param intentAction Intent action
     * @param uriString Optional URI string
     * @param extras Optional extras
     * @return true if intent was sent successfully
     */
    private boolean sendIntent(String packageName, String intentAction, String uriString, Map<String, Object> extras) {
        try {
            // Create intent
            Intent intent = new Intent();
            
            // Set package
            intent.setPackage(packageName);
            
            // Set action
            if (intentAction != null && !intentAction.isEmpty()) {
                intent.setAction(intentAction);
            } else {
                Log.e(TAG, "Intent action is required");
                return false;
            }
            
            // Set URI if provided
            if (uriString != null && !uriString.isEmpty()) {
                intent.setData(Uri.parse(uriString));
            }
            
            // Add extras if provided
            if (extras != null) {
                for (Map.Entry<String, Object> entry : extras.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    
                    if (value instanceof String) {
                        intent.putExtra(key, (String) value);
                    } else if (value instanceof Integer) {
                        intent.putExtra(key, (Integer) value);
                    } else if (value instanceof Boolean) {
                        intent.putExtra(key, (Boolean) value);
                    } else if (value instanceof Float) {
                        intent.putExtra(key, (Float) value);
                    } else if (value instanceof Long) {
                        intent.putExtra(key, (Long) value);
                    } else if (value instanceof Double) {
                        intent.putExtra(key, (Double) value);
                    } else if (value != null) {
                        intent.putExtra(key, value.toString());
                    }
                }
            }
            
            // Check if there are apps that can handle this intent
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
            
            if (activities.isEmpty()) {
                Log.e(TAG, "No activities found that can handle this intent");
                return false;
            }
            
            // Add flags
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // Send intent
            mainHandler.post(() -> context.startActivity(intent));
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending intent: " + e.getMessage());
            return false;
        }
    }
}
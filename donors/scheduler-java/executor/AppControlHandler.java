package com.aiassistant.scheduler.executor;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Handler for app control actions
 */
public class AppControlHandler implements StandardizedActionHandler {
    
    private static final String TAG = "AppControlHandler";
    
    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final PackageManager packageManager;
    private final Map<String, Runnable> activeActions;
    
    /**
     * Constructor
     * 
     * @param context Android context
     */
    public AppControlHandler(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.packageManager = context.getPackageManager();
        this.activeActions = new ConcurrentHashMap<>();
    }
    
    @NonNull
    @Override
    public CompletableFuture<ActionResult> executeAction(
            @NonNull String actionId,
            @Nullable Map<String, Object> parameters) {
        
        CompletableFuture<ActionResult> future = new CompletableFuture<>();
        
        if (parameters == null) {
            future.complete(ActionResult.failure(
                    "App control parameters cannot be null",
                    0,
                    "INVALID_PARAMETERS"));
            return future;
        }
        
        // Extract parameters
        String action = (String) parameters.get("action");
        String packageName = (String) parameters.get("packageName");
        String className = (String) parameters.get("className");
        String uri = (String) parameters.get("uri");
        @SuppressWarnings("unchecked")
        Map<String, Object> extras = (Map<String, Object>) parameters.get("extras");
        Integer flags = (Integer) parameters.get("flags");
        
        // Validate parameters
        if (action == null || action.isEmpty()) {
            future.complete(ActionResult.failure(
                    "App control action is required",
                    0,
                    "MISSING_ACTION"));
            return future;
        }
        
        if (packageName == null || packageName.isEmpty()) {
            future.complete(ActionResult.failure(
                    "Package name is required",
                    0,
                    "MISSING_PACKAGE_NAME"));
            return future;
        }
        
        // Execute action based on type
        final long startTime = System.currentTimeMillis();
        
        executorService.execute(() -> {
            try {
                Map<String, Object> resultData = new HashMap<>();
                boolean success = false;
                String message = "Action executed successfully";
                
                switch (action.toLowerCase()) {
                    case "launch":
                        success = launchApp(packageName, className, uri, extras, flags, resultData);
                        break;
                        
                    case "check":
                        success = checkAppInstalled(packageName, resultData);
                        break;
                        
                    case "info":
                        success = getAppInfo(packageName, resultData);
                        break;
                        
                    case "settings":
                        success = openAppSettings(packageName, resultData);
                        break;
                        
                    case "uninstall":
                        success = uninstallApp(packageName, resultData);
                        break;
                        
                    case "market":
                        success = openInMarket(packageName, resultData);
                        break;
                        
                    default:
                        message = "Unknown app control action: " + action;
                        success = false;
                        break;
                }
                
                // Return result
                long executionTime = System.currentTimeMillis() - startTime;
                
                if (success) {
                    future.complete(ActionResult.success(
                            message,
                            resultData,
                            executionTime));
                } else {
                    future.complete(ActionResult.failure(
                            message,
                            executionTime,
                            "ACTION_FAILED"));
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error executing app control action", e);
                long executionTime = System.currentTimeMillis() - startTime;
                future.complete(ActionResult.failure(
                        "App control error: " + e.getMessage(),
                        executionTime,
                        "APP_CONTROL_ERROR"));
            }
        });
        
        return future;
    }
    
    /**
     * Launch an application
     * 
     * @param packageName Package name
     * @param className Optional component name
     * @param uri Optional URI to open
     * @param extras Optional extras for the intent
     * @param flags Optional intent flags
     * @param resultData Map to store result data
     * @return Whether the launch was successful
     */
    private boolean launchApp(
            @NonNull String packageName,
            @Nullable String className,
            @Nullable String uri,
            @Nullable Map<String, Object> extras,
            @Nullable Integer flags,
            @NonNull Map<String, Object> resultData) {
        
        Intent intent;
        
        // Check if app is installed
        if (!isAppInstalled(packageName)) {
            resultData.put("error", "App not installed: " + packageName);
            return false;
        }
        
        // Create intent based on parameters
        if (uri != null && !uri.isEmpty()) {
            // Launch with URI
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage(packageName);
        } else if (className != null && !className.isEmpty()) {
            // Launch specific component
            intent = new Intent();
            intent.setClassName(packageName, className);
        } else {
            // Launch app using main activity
            intent = packageManager.getLaunchIntentForPackage(packageName);
            if (intent == null) {
                resultData.put("error", "No launchable activity found for: " + packageName);
                return false;
            }
        }
        
        // Add flags
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (flags != null) {
            intent.addFlags(flags);
        }
        
        // Add extras
        if (extras != null) {
            Bundle bundle = new Bundle();
            for (Map.Entry<String, Object> entry : extras.entrySet()) {
                putExtraToBundle(bundle, entry.getKey(), entry.getValue());
            }
            intent.putExtras(bundle);
        }
        
        // Launch app
        try {
            context.startActivity(intent);
            resultData.put("packageName", packageName);
            resultData.put("message", "App launched successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error launching app", e);
            resultData.put("error", "Error launching app: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if an app is installed
     * 
     * @param packageName Package name
     * @param resultData Map to store result data
     * @return Whether the check was successful
     */
    private boolean checkAppInstalled(
            @NonNull String packageName,
            @NonNull Map<String, Object> resultData) {
        
        boolean installed = isAppInstalled(packageName);
        resultData.put("installed", installed);
        resultData.put("packageName", packageName);
        return true;
    }
    
    /**
     * Get app information
     * 
     * @param packageName Package name
     * @param resultData Map to store result data
     * @return Whether the operation was successful
     */
    private boolean getAppInfo(
            @NonNull String packageName,
            @NonNull Map<String, Object> resultData) {
        
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_META_DATA);
            
            resultData.put("packageName", packageInfo.packageName);
            resultData.put("versionName", packageInfo.versionName);
            resultData.put("versionCode", packageInfo.getLongVersionCode());
            resultData.put("firstInstallTime", packageInfo.firstInstallTime);
            resultData.put("lastUpdateTime", packageInfo.lastUpdateTime);
            
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            resultData.put("error", "App not found: " + packageName);
            return false;
        }
    }
    
    /**
     * Open app settings
     * 
     * @param packageName Package name
     * @param resultData Map to store result data
     * @return Whether the operation was successful
     */
    private boolean openAppSettings(
            @NonNull String packageName,
            @NonNull Map<String, Object> resultData) {
        
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            
            resultData.put("packageName", packageName);
            resultData.put("message", "App settings opened successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error opening app settings", e);
            resultData.put("error", "Error opening app settings: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Uninstall an app
     * 
     * @param packageName Package name
     * @param resultData Map to store result data
     * @return Whether the operation was successful
     */
    private boolean uninstallApp(
            @NonNull String packageName,
            @NonNull Map<String, Object> resultData) {
        
        // Check if app is installed
        if (!isAppInstalled(packageName)) {
            resultData.put("error", "App not installed: " + packageName);
            return false;
        }
        
        try {
            Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            
            resultData.put("packageName", packageName);
            resultData.put("message", "Uninstall request sent successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error uninstalling app", e);
            resultData.put("error", "Error uninstalling app: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Open app in Play Store
     * 
     * @param packageName Package name
     * @param resultData Map to store result data
     * @return Whether the operation was successful
     */
    private boolean openInMarket(
            @NonNull String packageName,
            @NonNull Map<String, Object> resultData) {
        
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            
            // Try to open in Play Store app
            intent.setData(Uri.parse("market://details?id=" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // Check if there's an app that can handle the intent
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent);
            } else {
                // Fall back to browser
                intent.setData(Uri.parse(
                        "https://play.google.com/store/apps/details?id=" + packageName));
                context.startActivity(intent);
            }
            
            resultData.put("packageName", packageName);
            resultData.put("message", "Opened in market successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error opening app in market", e);
            resultData.put("error", "Error opening app in market: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if an app is installed
     * 
     * @param packageName Package name
     * @return Whether the app is installed
     */
    private boolean isAppInstalled(@NonNull String packageName) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Put an extra to a bundle based on its type
     * 
     * @param bundle Bundle to add the extra to
     * @param key Extra key
     * @param value Extra value
     */
    private void putExtraToBundle(
            @NonNull Bundle bundle,
            @NonNull String key,
            @Nullable Object value) {
        
        if (value == null) {
            return;
        }
        
        if (value instanceof String) {
            bundle.putString(key, (String) value);
        } else if (value instanceof Integer) {
            bundle.putInt(key, (Integer) value);
        } else if (value instanceof Boolean) {
            bundle.putBoolean(key, (Boolean) value);
        } else if (value instanceof Long) {
            bundle.putLong(key, (Long) value);
        } else if (value instanceof Float) {
            bundle.putFloat(key, (Float) value);
        } else if (value instanceof Double) {
            bundle.putDouble(key, (Double) value);
        } else if (value instanceof String[]) {
            bundle.putStringArray(key, (String[]) value);
        } else if (value instanceof Integer[]) {
            // Convert to primitive array
            Integer[] intObjects = (Integer[]) value;
            int[] intPrimitives = new int[intObjects.length];
            for (int i = 0; i < intObjects.length; i++) {
                intPrimitives[i] = intObjects[i];
            }
            bundle.putIntArray(key, intPrimitives);
        } else {
            // For other types, convert to string
            bundle.putString(key, value.toString());
        }
    }
    
    @NonNull
    @Override
    public String getActionType() {
        return "app_control";
    }
    
    @Override
    public boolean canExecute(@NonNull String actionId) {
        // This handler can execute any action with the correct parameters
        return true;
    }
    
    @Override
    public boolean validateParameters(
            @NonNull String actionId,
            @Nullable Map<String, Object> parameters) {
        
        if (parameters == null) {
            return false;
        }
        
        // Required parameters: action and packageName
        return parameters.containsKey("action") && parameters.get("action") instanceof String &&
                !((String) parameters.get("action")).isEmpty() &&
                parameters.containsKey("packageName") && parameters.get("packageName") instanceof String &&
                !((String) parameters.get("packageName")).isEmpty();
    }
    
    @NonNull
    @Override
    public Map<String, Class<?>> getRequiredParameters(@NonNull String actionId) {
        Map<String, Class<?>> params = new HashMap<>();
        params.put("action", String.class);
        params.put("packageName", String.class);
        return params;
    }
    
    @Override
    public boolean cancelAction(@NonNull String actionId) {
        Runnable action = activeActions.get(actionId);
        if (action != null) {
            mainHandler.removeCallbacks(action);
            activeActions.remove(actionId);
            return true;
        }
        return false;
    }
    
    @Nullable
    @Override
    public ActionStatus getActionStatus(@NonNull String actionId) {
        if (activeActions.containsKey(actionId)) {
            return new ActionStatus(
                    actionId,
                    "running",
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    50,
                    "App control action in progress");
        }
        return null;
    }
    
    /**
     * Shutdown the handler and its executor
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
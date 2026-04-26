package com.aiassistant.scheduler.executor;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Handler for system actions
 */
public class SystemActionHandler implements StandardizedActionHandler {
    
    private static final String TAG = "SystemActionHandler";
    
    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Map<String, Runnable> activeActions;
    
    /**
     * Constructor
     * 
     * @param context Android context
     */
    public SystemActionHandler(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
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
                    "System action parameters cannot be null",
                    0,
                    "INVALID_PARAMETERS"));
            return future;
        }
        
        // Extract parameters
        String actionType = (String) parameters.get("action");
        
        // Validate parameters
        if (actionType == null || actionType.isEmpty()) {
            future.complete(ActionResult.failure(
                    "System action type is required",
                    0,
                    "MISSING_ACTION_TYPE"));
            return future;
        }
        
        // Execute action based on type
        final long startTime = System.currentTimeMillis();
        
        executorService.execute(() -> {
            try {
                Map<String, Object> resultData = new HashMap<>();
                boolean success = false;
                String message = "Action executed successfully";
                
                switch (actionType.toLowerCase()) {
                    case "wifi":
                        success = handleWifiAction(parameters, resultData);
                        break;
                        
                    case "bluetooth":
                        success = handleBluetoothAction(parameters, resultData);
                        break;
                        
                    case "volume":
                        success = handleVolumeAction(parameters, resultData);
                        break;
                        
                    case "location":
                        success = handleLocationAction(parameters, resultData);
                        break;
                        
                    case "airplane_mode":
                        success = handleAirplaneModeAction(parameters, resultData);
                        break;
                        
                    case "brightness":
                        success = handleBrightnessAction(parameters, resultData);
                        break;
                        
                    case "settings":
                        success = openSystemSettings(parameters, resultData);
                        break;
                        
                    default:
                        message = "Unknown system action type: " + actionType;
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
                Log.e(TAG, "Error executing system action", e);
                long executionTime = System.currentTimeMillis() - startTime;
                future.complete(ActionResult.failure(
                        "System action error: " + e.getMessage(),
                        executionTime,
                        "SYSTEM_ERROR"));
            }
        });
        
        return future;
    }
    
    /**
     * Handle Wi-Fi related actions
     * 
     * @param parameters Action parameters
     * @param resultData Map to store result data
     * @return Whether the action was successful
     */
    private boolean handleWifiAction(Map<String, Object> parameters, Map<String, Object> resultData) {
        Boolean enable = (Boolean) parameters.get("enable");
        
        if (enable == null) {
            // Get current Wi-Fi state
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            
            boolean isEnabled = wifiManager != null && wifiManager.isWifiEnabled();
            resultData.put("enabled", isEnabled);
            return true;
        } else {
            // Newer Android versions require the Settings panel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
                panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(panelIntent);
                resultData.put("message", "Opened Wi-Fi settings panel");
                return true;
            } else {
                // For older versions, we can toggle Wi-Fi directly
                try {
                    WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                            .getSystemService(Context.WIFI_SERVICE);
                    if (wifiManager != null) {
                        wifiManager.setWifiEnabled(enable);
                        resultData.put("enabled", enable);
                        return true;
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "Wi-Fi toggle permission denied", e);
                    resultData.put("error", "Permission denied");
                    return false;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Handle Bluetooth related actions
     * 
     * @param parameters Action parameters
     * @param resultData Map to store result data
     * @return Whether the action was successful
     */
    private boolean handleBluetoothAction(Map<String, Object> parameters, Map<String, Object> resultData) {
        Boolean enable = (Boolean) parameters.get("enable");
        
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            resultData.put("error", "Bluetooth not supported on this device");
            return false;
        }
        
        if (enable == null) {
            // Get current Bluetooth state
            boolean isEnabled = bluetoothAdapter.isEnabled();
            resultData.put("enabled", isEnabled);
            return true;
        } else {
            // Toggle Bluetooth
            try {
                boolean success;
                if (enable) {
                    success = bluetoothAdapter.enable();
                } else {
                    success = bluetoothAdapter.disable();
                }
                resultData.put("enabled", enable);
                return success;
            } catch (SecurityException e) {
                Log.e(TAG, "Bluetooth toggle permission denied", e);
                resultData.put("error", "Permission denied");
                return false;
            }
        }
    }
    
    /**
     * Handle volume related actions
     * 
     * @param parameters Action parameters
     * @param resultData Map to store result data
     * @return Whether the action was successful
     */
    private boolean handleVolumeAction(Map<String, Object> parameters, Map<String, Object> resultData) {
        String volumeType = (String) parameters.get("type");
        Integer level = (Integer) parameters.get("level");
        
        if (volumeType == null) {
            volumeType = "media"; // Default to media volume
        }
        
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            resultData.put("error", "Audio service not available");
            return false;
        }
        
        int streamType;
        switch (volumeType.toLowerCase()) {
            case "alarm":
                streamType = AudioManager.STREAM_ALARM;
                break;
            case "music":
            case "media":
                streamType = AudioManager.STREAM_MUSIC;
                break;
            case "notification":
                streamType = AudioManager.STREAM_NOTIFICATION;
                break;
            case "ring":
            case "ringtone":
                streamType = AudioManager.STREAM_RING;
                break;
            case "system":
                streamType = AudioManager.STREAM_SYSTEM;
                break;
            case "voice":
            case "call":
                streamType = AudioManager.STREAM_VOICE_CALL;
                break;
            default:
                streamType = AudioManager.STREAM_MUSIC;
                break;
        }
        
        if (level == null) {
            // Get current volume
            int currentVolume = audioManager.getStreamVolume(streamType);
            int maxVolume = audioManager.getStreamMaxVolume(streamType);
            
            resultData.put("current", currentVolume);
            resultData.put("max", maxVolume);
            resultData.put("type", volumeType);
            return true;
        } else {
            // Set volume
            try {
                int maxVolume = audioManager.getStreamMaxVolume(streamType);
                int newVolume = Math.min(Math.max(level, 0), maxVolume);
                
                audioManager.setStreamVolume(
                        streamType,
                        newVolume,
                        0 // No flags, silent operation
                );
                
                resultData.put("type", volumeType);
                resultData.put("level", newVolume);
                resultData.put("max", maxVolume);
                return true;
            } catch (SecurityException e) {
                Log.e(TAG, "Volume control permission denied", e);
                resultData.put("error", "Permission denied");
                return false;
            }
        }
    }
    
    /**
     * Handle location related actions
     * 
     * @param parameters Action parameters
     * @param resultData Map to store result data
     * @return Whether the action was successful
     */
    private boolean handleLocationAction(Map<String, Object> parameters, Map<String, Object> resultData) {
        // Cannot directly enable/disable location on newer Android versions
        // instead, open settings
        
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(settingsIntent);
        
        // Get current location state
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager != null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        resultData.put("gpsEnabled", isGpsEnabled);
        resultData.put("networkEnabled", isNetworkEnabled);
        resultData.put("message", "Opened location settings");
        
        return true;
    }
    
    /**
     * Handle airplane mode related actions
     * 
     * @param parameters Action parameters
     * @param resultData Map to store result data
     * @return Whether the action was successful
     */
    private boolean handleAirplaneModeAction(Map<String, Object> parameters, Map<String, Object> resultData) {
        // Cannot directly toggle airplane mode on newer Android versions
        // instead, open settings
        
        Intent settingsIntent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(settingsIntent);
        
        // Get current airplane mode state
        boolean isEnabled = Settings.System.getInt(
                context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        
        resultData.put("enabled", isEnabled);
        resultData.put("message", "Opened airplane mode settings");
        
        return true;
    }
    
    /**
     * Handle brightness related actions
     * 
     * @param parameters Action parameters
     * @param resultData Map to store result data
     * @return Whether the action was successful
     */
    private boolean handleBrightnessAction(Map<String, Object> parameters, Map<String, Object> resultData) {
        // Cannot directly control brightness on newer Android versions without special permissions
        // instead, open settings
        
        Intent settingsIntent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(settingsIntent);
        
        resultData.put("message", "Opened display settings");
        
        return true;
    }
    
    /**
     * Open system settings
     * 
     * @param parameters Action parameters
     * @param resultData Map to store result data
     * @return Whether the action was successful
     */
    private boolean openSystemSettings(Map<String, Object> parameters, Map<String, Object> resultData) {
        String settingsType = (String) parameters.get("type");
        
        if (settingsType == null || settingsType.isEmpty()) {
            settingsType = "all"; // Default to main settings
        }
        
        Intent settingsIntent;
        
        switch (settingsType.toLowerCase()) {
            case "wifi":
                settingsIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                break;
            case "bluetooth":
                settingsIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                break;
            case "location":
                settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                break;
            case "security":
                settingsIntent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                break;
            case "sound":
                settingsIntent = new Intent(Settings.ACTION_SOUND_SETTINGS);
                break;
            case "display":
                settingsIntent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
                break;
            case "battery":
                settingsIntent = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
                break;
            case "storage":
                settingsIntent = new Intent(Settings.ACTION_STORAGE_SETTINGS);
                break;
            case "apps":
                settingsIntent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                break;
            case "date":
            case "time":
                settingsIntent = new Intent(Settings.ACTION_DATE_SETTINGS);
                break;
            case "language":
                settingsIntent = new Intent(Settings.ACTION_LOCALE_SETTINGS);
                break;
            case "accessibility":
                settingsIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                break;
            case "all":
            default:
                settingsIntent = new Intent(Settings.ACTION_SETTINGS);
                break;
        }
        
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        try {
            context.startActivity(settingsIntent);
            resultData.put("type", settingsType);
            resultData.put("message", "Opened " + settingsType + " settings");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error opening settings", e);
            resultData.put("error", "Could not open settings: " + e.getMessage());
            return false;
        }
    }
    
    @NonNull
    @Override
    public String getActionType() {
        return "system";
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
        
        // The action type is the only required parameter
        return parameters.containsKey("action") && parameters.get("action") instanceof String &&
                !((String) parameters.get("action")).isEmpty();
    }
    
    @NonNull
    @Override
    public Map<String, Class<?>> getRequiredParameters(@NonNull String actionId) {
        Map<String, Class<?>> params = new HashMap<>();
        params.put("action", String.class);
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
                    "System action in progress");
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
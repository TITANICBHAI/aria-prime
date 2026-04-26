package com.aiassistant.scheduler.executor.handlers;

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
import java.util.Map;

/**
 * Handler for wait/sleep actions
 */
public class WaitHandler implements ActionHandler {
    private static final String TAG = "WaitHandler";
    
    @Override
    public boolean executeAction(Map<String, Object> params) {
        // Get duration in milliseconds
        long durationMs = getDurationMs(params);
        
        // No duration specified or invalid
        if (durationMs <= 0) {
            Log.e(TAG, "Invalid wait duration: " + durationMs);
            return false;
        }
        
        try {
            Log.d(TAG, "Waiting for " + durationMs + " ms");
            Thread.sleep(durationMs);
            return true;
        } catch (InterruptedException e) {
            Log.e(TAG, "Wait interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    @Override
    public void executeAction(Map<String, Object> parameters, ActionCallback callback) {
        // Get duration in milliseconds
        long durationMs = getDurationMs(parameters);
        
        // No duration specified or invalid
        if (durationMs <= 0) {
            callback.onError("Invalid wait duration: " + durationMs);
            return;
        }
        
        // Create handler for main thread callbacks
        Handler mainHandler = new Handler(Looper.getMainLooper());
        
        // Execute wait in a separate thread to not block
        new Thread(() -> {
            try {
                Log.d(TAG, "Waiting for " + durationMs + " ms");
                Thread.sleep(durationMs);
                
                // Prepare result
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("durationMs", durationMs);
                
                // Send success result on main thread
                mainHandler.post(() -> callback.onComplete(result));
            } catch (InterruptedException e) {
                Log.e(TAG, "Wait interrupted: " + e.getMessage());
                
                // Send error on main thread
                mainHandler.post(() -> callback.onError("Wait interrupted: " + e.getMessage()));
                
                // Restore interrupt flag
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    @Override
    public void cancel() {
        // Interrupting the current thread to cancel wait
        Thread.currentThread().interrupt();
        Log.d(TAG, "Wait operation cancelled");
    }
    
    @Override
    public String getHandlerType() {
        return "wait";
    }
    
    /**
     * Get duration in milliseconds from parameters
     */
    private long getDurationMs(Map<String, Object> parameters) {
        // Check for direct milliseconds parameter
        Object msObj = parameters.get("durationMs");
        if (msObj != null) {
            return parseLong(msObj, 0);
        }
        
        // Check for seconds parameter
        Object secondsObj = parameters.get("durationSeconds");
        if (secondsObj != null) {
            return parseLong(secondsObj, 0) * 1000;
        }
        
        // Check for minutes parameter
        Object minutesObj = parameters.get("durationMinutes");
        if (minutesObj != null) {
            return parseLong(minutesObj, 0) * 60 * 1000;
        }
        
        // Check for hours parameter
        Object hoursObj = parameters.get("durationHours");
        if (hoursObj != null) {
            return parseLong(hoursObj, 0) * 60 * 60 * 1000;
        }
        
        // No duration specified
        return 0;
    }
    
    /**
     * Parse a long value from an object
     */
    private long parseLong(Object obj, long defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
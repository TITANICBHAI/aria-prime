package com.aiassistant.scheduler.executor;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handler for executing custom user-defined actions
 */
public class CustomActionHandler implements ActionHandler {
    
    private static final String TAG = "CustomActionHandler";
    
    /**
     * Interface for custom action implementations
     */
    public interface CustomActionImplementation {
        Map<String, Object> execute(Map<String, Object> parameters) throws Exception;
    }
    
    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Map<String, CustomActionImplementation> customActions;
    
    /**
     * Constructor
     * 
     * @param context Android context
     */
    public CustomActionHandler(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.customActions = new HashMap<>();
    }
    
    /**
     * Register a custom action implementation
     * 
     * @param actionId Custom action ID
     * @param implementation Implementation
     */
    public void registerCustomAction(String actionId, CustomActionImplementation implementation) {
        if (actionId != null && !actionId.isEmpty() && implementation != null) {
            customActions.put(actionId, implementation);
        }
    }
    
    /**
     * Unregister a custom action implementation
     * 
     * @param actionId Custom action ID
     */
    public void unregisterCustomAction(String actionId) {
        if (actionId != null && !actionId.isEmpty()) {
            customActions.remove(actionId);
        }
    }
    
    /**
     * Check if a custom action is registered
     * 
     * @param actionId Custom action ID
     * @return true if registered
     */
    public boolean hasCustomAction(String actionId) {
        return actionId != null && !actionId.isEmpty() && customActions.containsKey(actionId);
    }
    
    @Override
    public boolean executeAction(Map<String, Object> parameters) {
        // Extract parameters
        String actionId = (String) parameters.get("actionId");
        
        // Validate parameters
        if (actionId == null || actionId.isEmpty()) {
            Log.e(TAG, "Custom action ID is required");
            return false;
        }
        
        // Check if action is registered
        if (!customActions.containsKey(actionId)) {
            Log.e(TAG, "Custom action not found: " + actionId);
            return false;
        }
        
        // Get implementation
        CustomActionImplementation implementation = customActions.get(actionId);
        
        try {
            // Call implementation
            Map<String, Object> result = implementation.execute(parameters);
            return result != null && !result.containsKey("error");
        } catch (Exception e) {
            Log.e(TAG, "Error executing custom action: " + actionId, e);
            return false;
        }
    }
    
    @Override
    public void executeAction(Map<String, Object> parameters, ActionCallback callback) {
        // Extract parameters
        String actionId = (String) parameters.get("actionId");
        
        // Validate parameters
        if (actionId == null || actionId.isEmpty()) {
            callback.onError("Custom action ID is required");
            return;
        }
        
        // Check if action is registered
        if (!customActions.containsKey(actionId)) {
            callback.onError("Custom action not found: " + actionId);
            return;
        }
        
        // Get implementation
        CustomActionImplementation implementation = customActions.get(actionId);
        
        // Execute custom action in background
        executorService.execute(() -> {
            try {
                // Call implementation
                Map<String, Object> result = implementation.execute(parameters);
                
                // Add execution metadata
                if (result == null) {
                    result = new HashMap<>();
                }
                
                result.put("actionId", actionId);
                result.put("executedAt", System.currentTimeMillis());
                
                // Return result on main thread
                final Map<String, Object> finalResult = result;
                mainHandler.post(() -> callback.onComplete(finalResult));
                
            } catch (Exception e) {
                Log.e(TAG, "Error executing custom action: " + actionId, e);
                final String errorMessage = e.getMessage();
                mainHandler.post(() -> callback.onError(errorMessage));
            }
        });
    }
    
    @Override
    public void cancel() {
        // Not much to cancel here, as the implementation depends on the custom action
    }
    
    @Override
    public String getHandlerType() {
        return "custom";
    }
}
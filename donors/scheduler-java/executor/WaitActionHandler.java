package com.aiassistant.scheduler.executor;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Handler for wait/delay actions
 */
public class WaitActionHandler implements ActionHandler {
    
    private static final String TAG = "WaitActionHandler";
    private static final long DEFAULT_WAIT_TIME = 5000; // 5 seconds
    private static final long MAX_WAIT_TIME = 3600000; // 1 hour
    
    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private Future<?> currentTask;
    private boolean cancelled;
    
    /**
     * Constructor
     * 
     * @param context Android context
     */
    public WaitActionHandler(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.cancelled = false;
    }
    
    @Override
    public boolean executeAction(Map<String, Object> params) {
        // Create a simple wait without callback
        try {
            // Extract parameters
            long waitTime = DEFAULT_WAIT_TIME; // Default 5 seconds
            
            if (params.containsKey("milliseconds")) {
                try {
                    waitTime = Long.parseLong(params.get("milliseconds").toString());
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid milliseconds parameter: " + params.get("milliseconds"));
                }
            } else if (params.containsKey("seconds")) {
                try {
                    waitTime = TimeUnit.SECONDS.toMillis(Long.parseLong(params.get("seconds").toString()));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid seconds parameter: " + params.get("seconds"));
                }
            } else if (params.containsKey("minutes")) {
                try {
                    waitTime = TimeUnit.MINUTES.toMillis(Long.parseLong(params.get("minutes").toString()));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid minutes parameter: " + params.get("minutes"));
                }
            }
            
            // Validate wait time
            if (waitTime <= 0) {
                waitTime = DEFAULT_WAIT_TIME;
            } else if (waitTime > MAX_WAIT_TIME) {
                waitTime = MAX_WAIT_TIME;
            }
            
            // Execute a blocking wait for simplicity in this version
            try {
                Thread.sleep(waitTime);
                return true;
            } catch (InterruptedException e) {
                Log.e(TAG, "Wait interrupted", e);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in executeAction", e);
            return false;
        }
    }
    
    @Override
    public void executeAction(Map<String, Object> parameters, ActionCallback callback) {
        // Extract parameters
        long waitTime = DEFAULT_WAIT_TIME; // Default 5 seconds
        
        if (parameters.containsKey("milliseconds")) {
            try {
                waitTime = Long.parseLong(parameters.get("milliseconds").toString());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid milliseconds parameter: " + parameters.get("milliseconds"));
            }
        } else if (parameters.containsKey("seconds")) {
            try {
                waitTime = TimeUnit.SECONDS.toMillis(Long.parseLong(parameters.get("seconds").toString()));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid seconds parameter: " + parameters.get("seconds"));
            }
        } else if (parameters.containsKey("minutes")) {
            try {
                waitTime = TimeUnit.MINUTES.toMillis(Long.parseLong(parameters.get("minutes").toString()));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid minutes parameter: " + parameters.get("minutes"));
            }
        }
        
        // Validate wait time
        if (waitTime <= 0) {
            waitTime = DEFAULT_WAIT_TIME;
        } else if (waitTime > MAX_WAIT_TIME) {
            waitTime = MAX_WAIT_TIME;
        }
        
        // Get display text (if any)
        String displayText = (String) parameters.get("displayText");
        
        // Execute wait in background
        final long finalWaitTime = waitTime;
        cancelled = false;
        
        currentTask = executorService.submit(() -> {
            try {
                // Record start time
                long startTime = System.currentTimeMillis();
                
                // Wait until the specified time has passed
                while (!cancelled && System.currentTimeMillis() - startTime < finalWaitTime) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    long remainingTime = finalWaitTime - elapsedTime;
                    
                    // If there's a callback, update progress
                    if (callback instanceof ProgressActionCallback) {
                        ProgressActionCallback progressCallback = (ProgressActionCallback) callback;
                        float progress = Math.min(1.0f, (float) elapsedTime / finalWaitTime);
                        
                        Map<String, Object> progressData = new HashMap<>();
                        progressData.put("progress", progress);
                        progressData.put("elapsedTime", elapsedTime);
                        progressData.put("remainingTime", remainingTime);
                        progressData.put("displayText", displayText);
                        
                        mainHandler.post(() -> progressCallback.onProgress(progressData));
                    }
                    
                    // Sleep for a short time to avoid hogging the CPU
                    Thread.sleep(100);
                }
                
                // If not cancelled, complete
                if (!cancelled) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("waitTime", finalWaitTime);
                    result.put("completed", true);
                    
                    mainHandler.post(() -> callback.onComplete(result));
                }
            } catch (InterruptedException e) {
                // This is expected when cancel is called
                if (!cancelled) {
                    mainHandler.post(() -> callback.onError("Wait interrupted: " + e.getMessage()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during wait", e);
                mainHandler.post(() -> callback.onError("Error during wait: " + e.getMessage()));
            }
        });
    }
    
    @Override
    public void cancel() {
        cancelled = true;
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
    }
    
    @Override
    public String getHandlerType() {
        return "wait";
    }
    
    /**
     * Interface for callbacks with progress updates
     */
    public interface ProgressActionCallback extends ActionCallback {
        void onProgress(Map<String, Object> progressData);
    }
}
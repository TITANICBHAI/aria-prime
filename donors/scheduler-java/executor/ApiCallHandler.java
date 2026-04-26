package com.aiassistant.scheduler.executor;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handler for API call actions
 */
public class ApiCallHandler implements StandardizedActionHandler {
    
    private static final String TAG = "ApiCallHandler";
    private static final int DEFAULT_TIMEOUT_MS = 30000;
    
    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Map<String, AtomicBoolean> activeRequests;
    
    /**
     * Constructor
     * 
     * @param context Android context
     */
    public ApiCallHandler(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.activeRequests = new ConcurrentHashMap<>();
    }
    
    @NonNull
    @Override
    public CompletableFuture<ActionResult> executeAction(
            @NonNull String actionId,
            @Nullable Map<String, Object> parameters) {
        
        CompletableFuture<ActionResult> future = new CompletableFuture<>();
        
        if (parameters == null) {
            future.complete(ActionResult.failure(
                    "API call parameters cannot be null",
                    0,
                    "INVALID_PARAMETERS"));
            return future;
        }
        
        // Extract parameters
        String url = (String) parameters.get("url");
        String method = (String) parameters.get("method");
        Map<String, String> headers = (Map<String, String>) parameters.get("headers");
        Object body = parameters.get("body");
        Integer timeoutMs = (Integer) parameters.get("timeoutMs");
        
        // Validate parameters
        if (url == null || url.isEmpty()) {
            future.complete(ActionResult.failure(
                    "API URL is required",
                    0,
                    "MISSING_URL"));
            return future;
        }
        
        if (method == null || method.isEmpty()) {
            method = "GET"; // Default to GET
        }
        
        if (timeoutMs == null || timeoutMs <= 0) {
            timeoutMs = DEFAULT_TIMEOUT_MS;
        }
        
        // Track this request
        AtomicBoolean cancelled = new AtomicBoolean(false);
        activeRequests.put(actionId, cancelled);
        
        // Execute API call in background
        final String finalMethod = method.toUpperCase();
        final int finalTimeoutMs = timeoutMs;
        final long startTime = System.currentTimeMillis();
        
        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                // Create connection
                URL apiUrl = new URL(url);
                connection = (HttpURLConnection) apiUrl.openConnection();
                connection.setRequestMethod(finalMethod);
                connection.setConnectTimeout(finalTimeoutMs);
                connection.setReadTimeout(finalTimeoutMs);
                
                // Set headers
                if (headers != null) {
                    for (Map.Entry<String, String> header : headers.entrySet()) {
                        connection.setRequestProperty(header.getKey(), header.getValue());
                    }
                }
                
                // Set content type if not specified
                if (body != null && headers == null || 
                        (headers != null && !headers.containsKey("Content-Type"))) {
                    connection.setRequestProperty("Content-Type", "application/json");
                }
                
                // Send body for POST, PUT, PATCH methods
                if (body != null && (finalMethod.equals("POST") || 
                        finalMethod.equals("PUT") || 
                        finalMethod.equals("PATCH"))) {
                    
                    connection.setDoOutput(true);
                    String bodyStr;
                    if (body instanceof String) {
                        bodyStr = (String) body;
                    } else {
                        // Convert body to JSON string
                        // This is a simplified implementation
                        bodyStr = body.toString();
                    }
                    
                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = bodyStr.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }
                }
                
                // Check if cancelled
                if (cancelled.get()) {
                    connection.disconnect();
                    long executionTime = System.currentTimeMillis() - startTime;
                    future.complete(ActionResult.failure(
                            "API call cancelled",
                            executionTime,
                            "CANCELLED"));
                    return;
                }
                
                // Get response
                int statusCode = connection.getResponseCode();
                
                // Read response
                StringBuilder response = new StringBuilder();
                try (InputStream is = (statusCode >= 200 && statusCode < 300) ? 
                        connection.getInputStream() : connection.getErrorStream();
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                
                // Process response
                long executionTime = System.currentTimeMillis() - startTime;
                
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("statusCode", statusCode);
                resultData.put("response", response.toString());
                resultData.put("headers", connection.getHeaderFields());
                resultData.put("url", url);
                resultData.put("method", finalMethod);
                
                if (statusCode >= 200 && statusCode < 300) {
                    future.complete(ActionResult.success(
                            "API call successful",
                            resultData,
                            executionTime));
                } else {
                    future.complete(ActionResult.failure(
                            "API call failed with status code: " + statusCode,
                            executionTime,
                            "HTTP_ERROR_" + statusCode));
                }
                
            } catch (IOException e) {
                Log.e(TAG, "API call error", e);
                long executionTime = System.currentTimeMillis() - startTime;
                future.complete(ActionResult.failure(
                        "API call error: " + e.getMessage(),
                        executionTime,
                        "NETWORK_ERROR"));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                activeRequests.remove(actionId);
            }
        });
        
        return future;
    }
    
    @NonNull
    @Override
    public String getActionType() {
        return "api_call";
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
        
        // The URL is the only required parameter
        return parameters.containsKey("url") && parameters.get("url") instanceof String &&
                !((String) parameters.get("url")).isEmpty();
    }
    
    @NonNull
    @Override
    public Map<String, Class<?>> getRequiredParameters(@NonNull String actionId) {
        Map<String, Class<?>> params = new HashMap<>();
        params.put("url", String.class);
        return params;
    }
    
    @Override
    public boolean cancelAction(@NonNull String actionId) {
        AtomicBoolean cancelled = activeRequests.get(actionId);
        if (cancelled != null) {
            cancelled.set(true);
            return true;
        }
        return false;
    }
    
    @Nullable
    @Override
    public ActionStatus getActionStatus(@NonNull String actionId) {
        if (activeRequests.containsKey(actionId)) {
            return new ActionStatus(
                    actionId,
                    "running",
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    50,
                    "API call in progress");
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
package com.aiassistant.scheduler.executor.handlers;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import utils.ActionCallback;
import utils.ActionResult;
import utils.ActionStatus;
import utils.StandardizedActionHandler;
import com.aiassistant.scheduler.executor.handlers.ActionHandler;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handler for API call actions
 */
public class ApiCallHandler implements ActionHandler {
    
    private static final String TAG = "ApiCallHandler";
    private final ExecutorService executorService;
    private boolean isCancelled = false;
    
    /**
     * Create a new API call handler
     */
    public ApiCallHandler() {
        this.executorService = Executors.newCachedThreadPool();
    }
    
    @Override
    public boolean executeAction(Map<String, Object> params) {
        try {
            // Execute API call
            String url = (String) params.get("url");
            String method = (String) params.get("method");
            Map<String, String> headers = getHeaders(params);
            String body = (String) params.get("body");
            
            // Validate parameters
            if (url == null || url.isEmpty()) {
                Log.e(TAG, "URL is required");
                return false;
            }
            
            if (method == null || method.isEmpty()) {
                // Default to GET
                method = "GET";
            }
            
            // Make the API call
            int timeout = 30000; // Default 30s timeout
            if (params.containsKey("timeout") && params.get("timeout") instanceof Number) {
                timeout = ((Number) params.get("timeout")).intValue();
            }
            
            // Perform the request
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            
            // Add headers
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            
            // Add body for POST, PUT, etc.
            if (body != null && !body.isEmpty() && 
                    (method.equals("POST") || method.equals("PUT") || method.equals("PATCH"))) {
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = body.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
            }
            
            // Get response
            int responseCode = connection.getResponseCode();
            
            // Read response body
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading response: " + e.getMessage());
                return false;
            } finally {
                connection.disconnect();
            }
            
            // Success
            return responseCode >= 200 && responseCode < 300;
            
        } catch (Exception e) {
            Log.e(TAG, "API call error: " + e.getMessage());
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
                
                // Extract API call parameters
                String url = (String) parameters.get("url");
                String method = (String) parameters.get("method");
                Map<String, String> headers = getHeaders(parameters);
                String body = (String) parameters.get("body");
                
                // Validate parameters
                if (url == null || url.isEmpty()) {
                    callback.onError("URL is required");
                    return;
                }
                
                if (method == null || method.isEmpty()) {
                    // Default to GET
                    method = "GET";
                }
                
                // Make the API call
                int timeout = 30000; // Default 30s timeout
                if (parameters.containsKey("timeout") && parameters.get("timeout") instanceof Number) {
                    timeout = ((Number) parameters.get("timeout")).intValue();
                }
                
                // Perform the request
                URL apiUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(timeout);
                connection.setReadTimeout(timeout);
                
                // Add headers
                if (headers != null) {
                    for (Map.Entry<String, String> header : headers.entrySet()) {
                        connection.setRequestProperty(header.getKey(), header.getValue());
                    }
                }
                
                // Add body for POST, PUT, etc.
                if (body != null && !body.isEmpty() && 
                        (method.equals("POST") || method.equals("PUT") || method.equals("PATCH"))) {
                    connection.setDoOutput(true);
                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = body.getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }
                }
                
                // Get response
                int responseCode = connection.getResponseCode();
                String responseMessage = connection.getResponseMessage();
                
                // Read response body
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                } catch (Exception e) {
                    callback.onError("Error reading response: " + e.getMessage());
                    connection.disconnect();
                    return;
                }
                
                // Process response
                Map<String, Object> result = new HashMap<>();
                result.put("status", responseCode);
                result.put("statusText", responseMessage);
                result.put("body", response.toString());
                
                // Add headers to result
                Map<String, String> responseHeaders = new HashMap<>();
                for (Map.Entry<String, java.util.List<String>> entry : connection.getHeaderFields().entrySet()) {
                    if (entry.getKey() != null) {
                        responseHeaders.put(entry.getKey(), String.join(", ", entry.getValue()));
                    }
                }
                result.put("headers", responseHeaders);
                
                // Disconnect
                connection.disconnect();
                
                // Return result
                if (responseCode >= 200 && responseCode < 300) {
                    callback.onComplete(result);
                } else {
                    callback.onError("API call failed with status " + responseCode);
                }
                
            } catch (Exception e) {
                callback.onError("API call error: " + e.getMessage());
            }
        });
    }
    
    @Override
    public void cancel() {
        isCancelled = true;
    }
    
    @Override
    public String getHandlerType() {
        return "API_CALL";
    }
    
    /**
     * Extract headers from parameters
     */
    private Map<String, String> getHeaders(Map<String, Object> params) {
        Map<String, String> headers = new HashMap<>();
        
        // Add Content-Type by default for POST/PUT requests
        if (params.containsKey("body") && params.get("body") != null) {
            String method = (String) params.get("method");
            if (method != null && (method.equals("POST") || method.equals("PUT") || method.equals("PATCH"))) {
                headers.put("Content-Type", "application/json");
            }
        }
        
        // Get headers from parameters
        Object headersObj = params.get("headers");
        if (headersObj instanceof Map) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> headersMap = (Map<String, Object>) headersObj;
                
                for (Map.Entry<String, Object> entry : headersMap.entrySet()) {
                    headers.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            } catch (ClassCastException e) {
                Log.e(TAG, "Invalid headers format: " + e.getMessage());
            }
        }
        
        return headers;
    }
}
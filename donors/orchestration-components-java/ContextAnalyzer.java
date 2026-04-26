package com.aiassistant.core.orchestration.components;

import android.content.Context;
import android.util.Log;
import com.aiassistant.core.orchestration.ComponentInterface;
import com.aiassistant.core.orchestration.ComponentStateSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContextAnalyzer implements ComponentInterface {
    private static final String TAG = "ContextAnalyzer";
    private final Context context;
    private boolean isRunning = false;
    private boolean isHealthy = true;
    private long lastHeartbeat = 0;
    private int stateVersion = 0;
    private Map<String, Object> currentContext = new HashMap<>();
    
    public ContextAnalyzer(Context context) {
        this.context = context.getApplicationContext();
    }
    
    @Override
    public String getComponentId() {
        return "ContextAnalyzer";
    }
    
    @Override
    public String getComponentName() {
        return "Contextual Information Analyzer";
    }
    
    @Override
    public List<String> getCapabilities() {
        List<String> capabilities = new ArrayList<>();
        capabilities.add("context_analysis");
        capabilities.add("situation_awareness");
        capabilities.add("environmental_sensing");
        return capabilities;
    }
    
    @Override
    public void initialize() {
        Log.d(TAG, "Initializing ContextAnalyzer");
        currentContext.clear();
        isHealthy = true;
    }
    
    @Override
    public void start() {
        Log.d(TAG, "Starting ContextAnalyzer");
        isRunning = true;
        lastHeartbeat = System.currentTimeMillis();
    }
    
    @Override
    public void stop() {
        Log.d(TAG, "Stopping ContextAnalyzer");
        isRunning = false;
    }
    
    @Override
    public ComponentStateSnapshot captureState() {
        Map<String, Object> state = new HashMap<>();
        state.put("isRunning", isRunning);
        state.put("isHealthy", isHealthy);
        state.put("lastHeartbeat", lastHeartbeat);
        state.put("currentContext", new HashMap<>(currentContext));
        return new ComponentStateSnapshot(getComponentId(), stateVersion++, state);
    }
    
    @Override
    public void restoreState(ComponentStateSnapshot snapshot) {
        if (snapshot != null && getComponentId().equals(snapshot.getComponentId())) {
            Map<String, Object> state = snapshot.getState();
            isRunning = (Boolean) state.getOrDefault("isRunning", false);
            isHealthy = (Boolean) state.getOrDefault("isHealthy", true);
            lastHeartbeat = (Long) state.getOrDefault("lastHeartbeat", 0L);
            Object ctx = state.get("currentContext");
            if (ctx instanceof Map) {
                currentContext = new HashMap<>((Map<String, Object>) ctx);
            }
            Log.d(TAG, "State restored from snapshot version: " + snapshot.getVersion());
        }
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        try {
            Log.d(TAG, "Executing context analysis");
            
            Map<String, Object> analyzedContext = analyzeContext(input);
            currentContext.putAll(analyzedContext);
            
            result.put("success", true);
            result.put("context", analyzedContext);
            result.put("context_type", determineContextType(analyzedContext));
            
            isHealthy = true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error executing context analysis", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            isHealthy = false;
        }
        return result;
    }
    
    private Map<String, Object> analyzeContext(Map<String, Object> input) {
        Map<String, Object> context = new HashMap<>();
        
        context.put("timestamp", System.currentTimeMillis());
        context.put("app_state", "active");
        
        if (input != null) {
            if (input.containsKey("network_type")) {
                context.put("network", input.get("network_type"));
            }
            if (input.containsKey("battery_level")) {
                context.put("battery", input.get("battery_level"));
            }
            if (input.containsKey("user_activity")) {
                context.put("activity", input.get("user_activity"));
            }
        }
        
        return context;
    }
    
    private String determineContextType(Map<String, Object> context) {
        if (context.containsKey("activity")) {
            String activity = String.valueOf(context.get("activity"));
            if (activity.contains("game")) return "gaming";
            if (activity.contains("call")) return "communication";
        }
        
        Object battery = context.get("battery");
        if (battery instanceof Integer && (Integer) battery < 20) {
            return "low_power";
        }
        
        return "general";
    }
    
    @Override
    public boolean isHealthy() {
        return isHealthy && (System.currentTimeMillis() - lastHeartbeat < 300000);
    }
    
    @Override
    public void heartbeat() {
        lastHeartbeat = System.currentTimeMillis();
        Log.v(TAG, "Heartbeat updated");
    }
    
    @Override
    public String getStatus() {
        return "ContextAnalyzer[running=" + isRunning + ", healthy=" + isHealthy + 
               ", contexts=" + currentContext.size() + "]";
    }
}

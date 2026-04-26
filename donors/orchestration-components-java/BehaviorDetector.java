package com.aiassistant.core.orchestration.components;

import android.content.Context;
import android.util.Log;
import com.aiassistant.core.orchestration.ComponentInterface;
import com.aiassistant.core.orchestration.ComponentStateSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BehaviorDetector implements ComponentInterface {
    private static final String TAG = "BehaviorDetector";
    private final Context context;
    private boolean isRunning = false;
    private boolean isHealthy = true;
    private long lastHeartbeat = 0;
    private int stateVersion = 0;
    private List<String> detectedPatterns = new ArrayList<>();
    
    public BehaviorDetector(Context context) {
        this.context = context.getApplicationContext();
    }
    
    @Override
    public String getComponentId() {
        return "BehaviorDetector";
    }
    
    @Override
    public String getComponentName() {
        return "Behavior Pattern Detector";
    }
    
    @Override
    public List<String> getCapabilities() {
        List<String> capabilities = new ArrayList<>();
        capabilities.add("pattern_detection");
        capabilities.add("behavior_analysis");
        capabilities.add("user_profiling");
        return capabilities;
    }
    
    @Override
    public void initialize() {
        Log.d(TAG, "Initializing BehaviorDetector");
        detectedPatterns.clear();
        isHealthy = true;
    }
    
    @Override
    public void start() {
        Log.d(TAG, "Starting BehaviorDetector");
        isRunning = true;
        lastHeartbeat = System.currentTimeMillis();
    }
    
    @Override
    public void stop() {
        Log.d(TAG, "Stopping BehaviorDetector");
        isRunning = false;
    }
    
    @Override
    public ComponentStateSnapshot captureState() {
        Map<String, Object> state = new HashMap<>();
        state.put("isRunning", isRunning);
        state.put("isHealthy", isHealthy);
        state.put("lastHeartbeat", lastHeartbeat);
        state.put("detectedPatterns", new ArrayList<>(detectedPatterns));
        return new ComponentStateSnapshot(getComponentId(), stateVersion++, state);
    }
    
    @Override
    public void restoreState(ComponentStateSnapshot snapshot) {
        if (snapshot != null && getComponentId().equals(snapshot.getComponentId())) {
            Map<String, Object> state = snapshot.getState();
            isRunning = (Boolean) state.getOrDefault("isRunning", false);
            isHealthy = (Boolean) state.getOrDefault("isHealthy", true);
            lastHeartbeat = (Long) state.getOrDefault("lastHeartbeat", 0L);
            Object patterns = state.get("detectedPatterns");
            if (patterns instanceof List) {
                detectedPatterns = new ArrayList<>((List<String>) patterns);
            }
            Log.d(TAG, "State restored from snapshot version: " + snapshot.getVersion());
        }
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        try {
            Log.d(TAG, "Executing behavior detection");
            
            String pattern = analyzeUserBehavior(input);
            if (pattern != null) {
                detectedPatterns.add(pattern);
                result.put("pattern_detected", pattern);
                result.put("success", true);
                Log.d(TAG, "Detected behavior pattern: " + pattern);
            } else {
                result.put("success", true);
                result.put("pattern_detected", "none");
            }
            
            result.put("total_patterns", detectedPatterns.size());
            isHealthy = true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error executing behavior detection", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            isHealthy = false;
        }
        return result;
    }
    
    private String analyzeUserBehavior(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        
        if (input.containsKey("frequent_action")) {
            return "frequent_" + input.get("frequent_action");
        }
        
        return "general_usage_pattern";
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
        return "BehaviorDetector[running=" + isRunning + ", healthy=" + isHealthy + 
               ", patterns=" + detectedPatterns.size() + "]";
    }
}

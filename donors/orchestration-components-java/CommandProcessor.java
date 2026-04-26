package com.aiassistant.core.orchestration.components;

import android.content.Context;
import android.util.Log;
import com.aiassistant.core.orchestration.ComponentInterface;
import com.aiassistant.core.orchestration.ComponentStateSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandProcessor implements ComponentInterface {
    private static final String TAG = "CommandProcessor";
    private final Context context;
    private boolean isRunning = false;
    private boolean isHealthy = true;
    private long lastHeartbeat = 0;
    private int stateVersion = 0;
    private int processedCommands = 0;
    
    public CommandProcessor(Context context) {
        this.context = context.getApplicationContext();
    }
    
    @Override
    public String getComponentId() {
        return "CommandProcessor";
    }
    
    @Override
    public String getComponentName() {
        return "Voice Command Processor";
    }
    
    @Override
    public List<String> getCapabilities() {
        List<String> capabilities = new ArrayList<>();
        capabilities.add("command_parsing");
        capabilities.add("intent_extraction");
        capabilities.add("parameter_extraction");
        return capabilities;
    }
    
    @Override
    public void initialize() {
        Log.d(TAG, "Initializing CommandProcessor");
        processedCommands = 0;
        isHealthy = true;
    }
    
    @Override
    public void start() {
        Log.d(TAG, "Starting CommandProcessor");
        isRunning = true;
        lastHeartbeat = System.currentTimeMillis();
    }
    
    @Override
    public void stop() {
        Log.d(TAG, "Stopping CommandProcessor");
        isRunning = false;
    }
    
    @Override
    public ComponentStateSnapshot captureState() {
        Map<String, Object> state = new HashMap<>();
        state.put("isRunning", isRunning);
        state.put("isHealthy", isHealthy);
        state.put("lastHeartbeat", lastHeartbeat);
        state.put("processedCommands", processedCommands);
        return new ComponentStateSnapshot(getComponentId(), stateVersion++, state);
    }
    
    @Override
    public void restoreState(ComponentStateSnapshot snapshot) {
        if (snapshot != null && getComponentId().equals(snapshot.getComponentId())) {
            Map<String, Object> state = snapshot.getState();
            isRunning = (Boolean) state.getOrDefault("isRunning", false);
            isHealthy = (Boolean) state.getOrDefault("isHealthy", true);
            lastHeartbeat = (Long) state.getOrDefault("lastHeartbeat", 0L);
            processedCommands = (Integer) state.getOrDefault("processedCommands", 0);
            Log.d(TAG, "State restored from snapshot version: " + snapshot.getVersion());
        }
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        try {
            Log.d(TAG, "Executing command processing");
            
            Map<String, Object> command = parseCommand(input);
            processedCommands++;
            
            result.put("success", true);
            result.put("command", command);
            result.put("total_processed", processedCommands);
            
            isHealthy = true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error executing command processing", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            isHealthy = false;
        }
        return result;
    }
    
    private Map<String, Object> parseCommand(Map<String, Object> input) {
        Map<String, Object> command = new HashMap<>();
        
        if (input != null && input.containsKey("recognized_text")) {
            String text = String.valueOf(input.get("recognized_text"));
            command.put("text", text);
            command.put("intent", extractIntent(text));
            command.put("parameters", extractParameters(text));
        } else {
            command.put("intent", "unknown");
            command.put("parameters", new HashMap<>());
        }
        
        return command;
    }
    
    private String extractIntent(String text) {
        if (text == null) return "unknown";
        text = text.toLowerCase();
        
        if (text.contains("play")) return "play_media";
        if (text.contains("stop")) return "stop_action";
        if (text.contains("open")) return "open_app";
        if (text.contains("search")) return "search_query";
        
        return "general_command";
    }
    
    private Map<String, String> extractParameters(String text) {
        Map<String, String> params = new HashMap<>();
        if (text != null) {
            params.put("raw_text", text);
            params.put("length", String.valueOf(text.length()));
        }
        return params;
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
        return "CommandProcessor[running=" + isRunning + ", healthy=" + isHealthy + 
               ", processed=" + processedCommands + "]";
    }
}

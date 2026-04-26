package com.aiassistant.core.orchestration.components;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;
import com.aiassistant.core.orchestration.ComponentInterface;
import com.aiassistant.core.orchestration.ComponentStateSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BatteryMonitor implements ComponentInterface {
    private static final String TAG = "BatteryMonitor";
    private final Context context;
    private boolean isRunning = false;
    private boolean isHealthy = true;
    private long lastHeartbeat = 0;
    private int stateVersion = 0;
    private int lastBatteryLevel = -1;
    private boolean isCharging = false;
    
    public BatteryMonitor(Context context) {
        this.context = context.getApplicationContext();
    }
    
    @Override
    public String getComponentId() {
        return "BatteryMonitor";
    }
    
    @Override
    public String getComponentName() {
        return "Battery Status Monitor";
    }
    
    @Override
    public List<String> getCapabilities() {
        List<String> capabilities = new ArrayList<>();
        capabilities.add("battery_monitoring");
        capabilities.add("charging_detection");
        capabilities.add("power_status");
        return capabilities;
    }
    
    @Override
    public void initialize() {
        Log.d(TAG, "Initializing BatteryMonitor");
        checkBatteryStatus();
        isHealthy = true;
    }
    
    @Override
    public void start() {
        Log.d(TAG, "Starting BatteryMonitor");
        isRunning = true;
        lastHeartbeat = System.currentTimeMillis();
    }
    
    @Override
    public void stop() {
        Log.d(TAG, "Stopping BatteryMonitor");
        isRunning = false;
    }
    
    @Override
    public ComponentStateSnapshot captureState() {
        Map<String, Object> state = new HashMap<>();
        state.put("isRunning", isRunning);
        state.put("isHealthy", isHealthy);
        state.put("lastHeartbeat", lastHeartbeat);
        state.put("lastBatteryLevel", lastBatteryLevel);
        state.put("isCharging", isCharging);
        return new ComponentStateSnapshot(getComponentId(), stateVersion++, state);
    }
    
    @Override
    public void restoreState(ComponentStateSnapshot snapshot) {
        if (snapshot != null && getComponentId().equals(snapshot.getComponentId())) {
            Map<String, Object> state = snapshot.getState();
            isRunning = (Boolean) state.getOrDefault("isRunning", false);
            isHealthy = (Boolean) state.getOrDefault("isHealthy", true);
            lastHeartbeat = (Long) state.getOrDefault("lastHeartbeat", 0L);
            lastBatteryLevel = (Integer) state.getOrDefault("lastBatteryLevel", -1);
            isCharging = (Boolean) state.getOrDefault("isCharging", false);
            Log.d(TAG, "State restored from snapshot version: " + snapshot.getVersion());
        }
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        try {
            Log.d(TAG, "Executing battery monitoring");
            
            checkBatteryStatus();
            
            result.put("success", true);
            result.put("battery_level", lastBatteryLevel);
            result.put("is_charging", isCharging);
            result.put("battery_status", getBatteryStatus());
            
            isHealthy = true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error executing battery monitoring", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            isHealthy = false;
        }
        return result;
    }
    
    private void checkBatteryStatus() {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                lastBatteryLevel = (int) ((level / (float) scale) * 100);
                
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == BatteryManager.BATTERY_STATUS_FULL;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking battery status", e);
            lastBatteryLevel = -1;
            isCharging = false;
        }
    }
    
    private String getBatteryStatus() {
        if (lastBatteryLevel < 0) return "unknown";
        if (isCharging) return "charging";
        if (lastBatteryLevel < 15) return "critical";
        if (lastBatteryLevel < 30) return "low";
        if (lastBatteryLevel < 80) return "normal";
        return "good";
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
        return "BatteryMonitor[running=" + isRunning + ", healthy=" + isHealthy + 
               ", level=" + lastBatteryLevel + "%, charging=" + isCharging + "]";
    }
}

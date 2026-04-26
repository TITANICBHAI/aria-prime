package com.aiassistant.core.orchestration;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HealthMonitor {
    private static final String TAG = "HealthMonitor";
    
    private final Context context;
    private final ComponentRegistry componentRegistry;
    private final Map<String, ComponentHealth> healthRecords;
    private final Map<String, CircuitBreaker> circuitBreakers;
    
    private EventRouter eventRouter;
    private boolean isRunning = false;
    
    public HealthMonitor(Context context, ComponentRegistry componentRegistry) {
        this.context = context.getApplicationContext();
        this.componentRegistry = componentRegistry;
        this.healthRecords = new ConcurrentHashMap<>();
        this.circuitBreakers = new ConcurrentHashMap<>();
    }
    
    public void setEventRouter(EventRouter eventRouter) {
        this.eventRouter = eventRouter;
    }
    
    public void start() {
        isRunning = true;
        Log.i(TAG, "Health Monitor started");
    }
    
    public void stop() {
        isRunning = false;
        Log.i(TAG, "Health Monitor stopped");
    }
    
    public void recordHeartbeat(String componentId) {
        ComponentHealth health = healthRecords.computeIfAbsent(
            componentId,
            k -> new ComponentHealth(componentId)
        );
        
        health.lastHeartbeat = System.currentTimeMillis();
        health.consecutiveFailures = 0;
        
        componentRegistry.updateHeartbeat(componentId);
    }
    
    public void recordError(String componentId, String errorType) {
        ComponentHealth health = healthRecords.computeIfAbsent(
            componentId,
            k -> new ComponentHealth(componentId)
        );
        
        health.errorCount++;
        health.consecutiveFailures++;
        health.lastError = System.currentTimeMillis();
        
        Log.w(TAG, "Error recorded for " + componentId + 
              " (consecutive: " + health.consecutiveFailures + ")");
        
        if (health.consecutiveFailures >= 3) {
            degradeComponent(componentId);
        }
        
        CircuitBreaker breaker = getOrCreateCircuitBreaker(componentId);
        breaker.recordFailure();
    }
    
    public void recordSuccess(String componentId) {
        ComponentHealth health = healthRecords.computeIfAbsent(
            componentId,
            k -> new ComponentHealth(componentId)
        );
        
        health.successCount++;
        health.consecutiveFailures = 0;
        
        CircuitBreaker breaker = circuitBreakers.get(componentId);
        if (breaker != null) {
            breaker.recordSuccess();
        }
    }
    
    private void degradeComponent(String componentId) {
        componentRegistry.updateComponentStatus(
            componentId,
            ComponentRegistry.ComponentStatus.DEGRADED
        );
        
        if (eventRouter != null) {
            OrchestrationEvent event = new OrchestrationEvent(
                "component.degraded",
                componentId,
                null
            );
            eventRouter.publish(event);
        }
        
        Log.w(TAG, "Component " + componentId + " marked as degraded");
    }
    
    public void isolateComponent(String componentId) {
        componentRegistry.updateComponentStatus(
            componentId,
            ComponentRegistry.ComponentStatus.ISOLATED
        );
        
        Log.w(TAG, "Component " + componentId + " isolated");
    }
    
    public void attemptWarmRestart(String componentId) {
        Log.i(TAG, "Attempting warm restart for " + componentId);
        
        ComponentHealth health = healthRecords.get(componentId);
        if (health != null) {
            health.restartCount++;
        }
        
        componentRegistry.updateComponentStatus(
            componentId,
            ComponentRegistry.ComponentStatus.INITIALIZING
        );
    }
    
    public void performHealthCheck() {
        long currentTime = System.currentTimeMillis();
        
        for (ComponentRegistry.RegisteredComponent component : componentRegistry.getAllComponents()) {
            String componentId = component.componentId;
            ComponentHealth health = healthRecords.get(componentId);
            
            if (health == null) {
                continue;
            }
            
            long heartbeatAge = currentTime - health.lastHeartbeat;
            
            if (heartbeatAge > 30000 && component.status == ComponentRegistry.ComponentStatus.ACTIVE) {
                Log.w(TAG, "Health check failed for " + componentId + 
                      " (heartbeat age: " + heartbeatAge + "ms)");
                
                if (eventRouter != null) {
                    OrchestrationEvent event = new OrchestrationEvent(
                        "health.check.failed",
                        componentId,
                        new HashMap<String, Object>() {{
                            put("heartbeat_age", heartbeatAge);
                        }}
                    );
                    eventRouter.publish(event);
                }
            }
            
            float errorRate = calculateErrorRate(health);
            if (errorRate > 0.5f) {
                degradeComponent(componentId);
            }
        }
    }
    
    private float calculateErrorRate(ComponentHealth health) {
        int total = health.successCount + health.errorCount;
        if (total == 0) {
            return 0.0f;
        }
        return (float) health.errorCount / total;
    }
    
    private CircuitBreaker getOrCreateCircuitBreaker(String componentId) {
        return circuitBreakers.computeIfAbsent(
            componentId,
            k -> new CircuitBreaker(componentId, 5, 60000)
        );
    }
    
    public CircuitBreaker getCircuitBreaker(String componentId) {
        return circuitBreakers.get(componentId);
    }
    
    public ComponentHealth getComponentHealth(String componentId) {
        return healthRecords.get(componentId);
    }
    
    public static class ComponentHealth {
        public final String componentId;
        public long lastHeartbeat;
        public long lastError;
        public int errorCount;
        public int successCount;
        public int consecutiveFailures;
        public int restartCount;
        
        public ComponentHealth(String componentId) {
            this.componentId = componentId;
            this.lastHeartbeat = System.currentTimeMillis();
            this.errorCount = 0;
            this.successCount = 0;
            this.consecutiveFailures = 0;
            this.restartCount = 0;
        }
    }
}

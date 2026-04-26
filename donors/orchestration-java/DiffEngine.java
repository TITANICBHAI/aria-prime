package com.aiassistant.core.orchestration;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiffEngine {
    private static final String TAG = "DiffEngine";
    
    private final Context context;
    private final Map<String, ComponentStateSnapshot> latestSnapshots;
    private final Map<String, ComponentStateSnapshot> expectedSnapshots;
    private final Map<String, Long> lastDiffCheck;
    private final long THROTTLE_INTERVAL_MS = 5000;
    
    private EventRouter eventRouter;
    
    public DiffEngine(Context context) {
        this.context = context.getApplicationContext();
        this.latestSnapshots = new ConcurrentHashMap<>();
        this.expectedSnapshots = new ConcurrentHashMap<>();
        this.lastDiffCheck = new ConcurrentHashMap<>();
    }
    
    public void setEventRouter(EventRouter eventRouter) {
        this.eventRouter = eventRouter;
    }
    
    public void captureSnapshot(ComponentStateSnapshot snapshot) {
        String componentId = snapshot.getComponentId();
        latestSnapshots.put(componentId, snapshot);
        
        Log.d(TAG, "Captured snapshot for " + componentId + " v" + snapshot.getVersion());
    }
    
    public void setExpectedState(ComponentStateSnapshot snapshot) {
        String componentId = snapshot.getComponentId();
        expectedSnapshots.put(componentId, snapshot);
        
        Log.d(TAG, "Set expected state for " + componentId + " v" + snapshot.getVersion());
    }
    
    public StateDiff checkDiff(String componentId) {
        long currentTime = System.currentTimeMillis();
        Long lastCheck = lastDiffCheck.get(componentId);
        
        if (lastCheck != null && (currentTime - lastCheck) < THROTTLE_INTERVAL_MS) {
            Log.d(TAG, "Throttling diff check for " + componentId);
            return null;
        }
        
        ComponentStateSnapshot expected = expectedSnapshots.get(componentId);
        ComponentStateSnapshot actual = latestSnapshots.get(componentId);
        
        if (expected == null || actual == null) {
            return null;
        }
        
        lastDiffCheck.put(componentId, currentTime);
        
        if (expected.getStateHash().equals(actual.getStateHash())) {
            return null;
        }
        
        StateDiff diff = computeDiff(expected, actual);
        
        if (diff != null && eventRouter != null) {
            OrchestrationEvent event = new OrchestrationEvent(
                "state.diff.detected",
                componentId,
                new HashMap<String, Object>() {{
                    put("diff", diff);
                    put("severity", diff.getSeverity().name());
                }}
            );
            eventRouter.publish(event);
        }
        
        return diff;
    }
    
    private StateDiff computeDiff(ComponentStateSnapshot expected, ComponentStateSnapshot actual) {
        List<StateDiff.FieldDiff> fieldDiffs = new ArrayList<>();
        Map<String, Object> expectedState = expected.getState();
        Map<String, Object> actualState = actual.getState();
        
        for (Map.Entry<String, Object> entry : expectedState.entrySet()) {
            String fieldName = entry.getKey();
            Object expectedValue = entry.getValue();
            Object actualValue = actualState.get(fieldName);
            
            if (expectedValue == null && actualValue == null) {
                continue;
            }
            
            if (expectedValue == null || actualValue == null || 
                !expectedValue.equals(actualValue)) {
                fieldDiffs.add(new StateDiff.FieldDiff(
                    fieldName,
                    expectedValue,
                    actualValue,
                    "value_mismatch"
                ));
            }
        }
        
        for (String key : actualState.keySet()) {
            if (!expectedState.containsKey(key)) {
                fieldDiffs.add(new StateDiff.FieldDiff(
                    key,
                    null,
                    actualState.get(key),
                    "unexpected_field"
                ));
            }
        }
        
        if (fieldDiffs.isEmpty()) {
            return null;
        }
        
        StateDiff.Severity severity = determineSeverity(fieldDiffs);
        String description = "State mismatch detected: " + fieldDiffs.size() + " field(s) differ";
        
        return new StateDiff(
            expected.getComponentId(),
            severity,
            description,
            fieldDiffs,
            expected,
            actual
        );
    }
    
    private StateDiff.Severity determineSeverity(List<StateDiff.FieldDiff> diffs) {
        int criticalFields = 0;
        for (StateDiff.FieldDiff diff : diffs) {
            if (isCriticalField(diff.fieldName)) {
                criticalFields++;
            }
        }
        
        if (criticalFields > 0) {
            return StateDiff.Severity.CRITICAL;
        } else if (diffs.size() > 3) {
            return StateDiff.Severity.WARNING;
        } else {
            return StateDiff.Severity.INFO;
        }
    }
    
    private boolean isCriticalField(String fieldName) {
        return fieldName.contains("error") || 
               fieldName.contains("critical") || 
               fieldName.contains("health") ||
               fieldName.contains("status");
    }
    
    public void performPeriodicDiffCheck() {
        Log.d(TAG, "Performing periodic diff check");
        
        for (String componentId : latestSnapshots.keySet()) {
            checkDiff(componentId);
        }
    }
    
    public ComponentStateSnapshot getLatestSnapshot(String componentId) {
        return latestSnapshots.get(componentId);
    }
    
    public ComponentStateSnapshot getExpectedSnapshot(String componentId) {
        return expectedSnapshots.get(componentId);
    }
}

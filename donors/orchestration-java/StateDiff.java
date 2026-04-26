package com.aiassistant.core.orchestration;

import java.util.ArrayList;
import java.util.List;

public class StateDiff {
    private final String componentId;
    private final long timestamp;
    private final Severity severity;
    private final String description;
    private final List<FieldDiff> fieldDiffs;
    private final ComponentStateSnapshot expectedState;
    private final ComponentStateSnapshot actualState;
    
    public StateDiff(String componentId, Severity severity, String description,
                     List<FieldDiff> fieldDiffs, ComponentStateSnapshot expectedState,
                     ComponentStateSnapshot actualState) {
        this.componentId = componentId;
        this.severity = severity;
        this.description = description;
        this.fieldDiffs = fieldDiffs != null ? new ArrayList<>(fieldDiffs) : new ArrayList<>();
        this.expectedState = expectedState;
        this.actualState = actualState;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getComponentId() {
        return componentId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public Severity getSeverity() {
        return severity;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<FieldDiff> getFieldDiffs() {
        return new ArrayList<>(fieldDiffs);
    }
    
    public ComponentStateSnapshot getExpectedState() {
        return expectedState;
    }
    
    public ComponentStateSnapshot getActualState() {
        return actualState;
    }
    
    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }
    
    public static class FieldDiff {
        public final String fieldName;
        public final Object expectedValue;
        public final Object actualValue;
        public final String diffType;
        
        public FieldDiff(String fieldName, Object expectedValue, Object actualValue, String diffType) {
            this.fieldName = fieldName;
            this.expectedValue = expectedValue;
            this.actualValue = actualValue;
            this.diffType = diffType;
        }
        
        @Override
        public String toString() {
            return "FieldDiff{" +
                    "field='" + fieldName + '\'' +
                    ", expected=" + expectedValue +
                    ", actual=" + actualValue +
                    ", type='" + diffType + '\'' +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "StateDiff{" +
                "componentId='" + componentId + '\'' +
                ", severity=" + severity +
                ", description='" + description + '\'' +
                ", fieldDiffs=" + fieldDiffs.size() +
                '}';
    }
}

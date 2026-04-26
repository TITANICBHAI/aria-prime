package com.aiassistant.scheduler.model;

import java.io.Serializable;

/**
 * Task priority levels
 */
public enum TaskPriority implements Serializable {
    LOW(0),        // Low priority task
    MEDIUM(1),     // Medium priority task
    HIGH(2),       // High priority task
    CRITICAL(3),   // Critical priority task
    SYSTEM(4);     // System-level priority task
    
    private final int value;
    
    TaskPriority(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    /**
     * Get priority from integer value
     */
    public static TaskPriority fromValue(int value) {
        for (TaskPriority priority : TaskPriority.values()) {
            if (priority.getValue() == value) {
                return priority;
            }
        }
        
        return MEDIUM; // Default
    }
}
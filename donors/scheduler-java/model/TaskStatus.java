package com.aiassistant.scheduler.model;

import java.io.Serializable;

/**
 * Task execution status
 */
public enum TaskStatus implements Serializable {
    PENDING("pending"),         // Task is waiting to be scheduled
    SCHEDULED("scheduled"),     // Task is scheduled for execution
    RUNNING("running"),         // Task is currently running
    COMPLETED("completed"),     // Task completed successfully
    FAILED("failed"),           // Task execution failed
    CANCELLED("cancelled"),     // Task was cancelled by the user
    PAUSED("paused"),           // Task execution is paused
    WAITING("waiting");         // Task is waiting for a condition
    
    private final String value;
    
    TaskStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Get task status from string value
     */
    public static TaskStatus fromString(String value) {
        for (TaskStatus status : TaskStatus.values()) {
            if (status.getValue().equalsIgnoreCase(value)) {
                return status;
            }
        }
        
        return PENDING; // Default
    }
}
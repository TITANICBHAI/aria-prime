package com.aiassistant.scheduler;

import java.io.Serializable;

/**
 * Status of a scheduled task
 * Based on TaskStatus enum from advanced_task_scheduler.py
 */
public enum TaskStatus implements Serializable {
    PENDING("pending"),                     // Task is pending, not yet scheduled
    SCHEDULED("scheduled"),                 // Task is scheduled for execution
    RUNNING("running"),                     // Task is currently running
    COMPLETED("completed"),                 // Task has completed successfully
    FAILED("failed"),                       // Task has failed
    CANCELLED("cancelled"),                 // Task was cancelled
    PAUSED("paused"),                       // Task is paused
    WAITING_FOR_CONDITION("waiting_for_condition"); // Task is waiting for a condition
    
    private final String value;
    
    TaskStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
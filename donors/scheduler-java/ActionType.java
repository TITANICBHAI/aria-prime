package com.aiassistant.scheduler;

import java.io.Serializable;

/**
 * Types of actions that can be performed by the scheduler
 * Based on ActionType enum from advanced_task_scheduler.py
 */
public enum ActionType implements Serializable {
    APP_CONTROL("app_control"),       // Control external apps
    SYSTEM_ACTION("system_action"),   // Device settings, etc.
    API_CALL("api_call"),             // External API request
    NOTIFICATION("notification"),     // Show notification
    CUSTOM("custom"),                 // Custom user-defined action
    SCRIPT("script"),                 // Run a script
    WAIT("wait"),                     // Wait for a duration
    CONDITION("condition"),           // Conditional logic
    ITERATION("iteration"),           // Loop through items
    INPUT("input"),                   // Request user input
    FUNCTION("function");             // Call a Java/Kotlin function
    
    private final String value;
    
    ActionType(String value) {
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
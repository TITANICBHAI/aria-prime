package com.aiassistant.scheduler.model;

import java.io.Serializable;

/**
 * Types of triggers that can start a task
 */
public enum TriggerType implements Serializable {
    IMMEDIATE("immediate"),                // Execute immediately
    SCHEDULED("scheduled"),                // Execute at scheduled time
    APP_LAUNCH("app_launch"),              // Trigger when app is launched
    APP_EXIT("app_exit"),                  // Trigger when app is exited
    NOTIFICATION("notification"),          // Trigger on notification
    LOCATION("location"),                  // Trigger on location change
    BATTERY("battery"),                    // Trigger on battery level change
    CONNECTIVITY("connectivity"),          // Trigger on connectivity change
    SCREEN_STATE("screen_state"),          // Trigger on screen state change
    TIME_RANGE("time_range"),              // Trigger during time range
    DATA_CONDITION("data_condition"),      // Trigger when data condition is met
    SENSOR("sensor"),                      // Trigger on sensor data
    DEVICE_STATE("device_state"),          // Trigger on device state change
    USER_PRESENCE("user_presence"),        // Trigger based on user presence
    ACTIVITY_RECOGNITION("activity"),      // Trigger based on activity recognition
    CUSTOM("custom");                      // Custom trigger type
    
    private final String value;
    
    TriggerType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Get trigger type from string value
     */
    public static TriggerType fromString(String value) {
        for (TriggerType type : TriggerType.values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        
        return IMMEDIATE; // Default
    }
}
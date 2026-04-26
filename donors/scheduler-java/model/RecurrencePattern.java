package com.aiassistant.scheduler.model;

import java.io.Serializable;

/**
 * Task recurrence patterns
 */
public enum RecurrencePattern implements Serializable {
    NONE("none"),           // No recurrence (one-time task)
    MINUTES("minutes"),     // Recur every X minutes
    HOURLY("hourly"),       // Recur every X hours
    DAILY("daily"),         // Recur every X days
    WEEKLY("weekly"),       // Recur every X weeks
    MONTHLY("monthly"),     // Recur every X months
    CUSTOM("custom");       // Custom recurrence pattern (using seconds)
    
    private final String value;
    
    RecurrencePattern(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Get recurrence pattern from string value
     */
    public static RecurrencePattern fromString(String value) {
        for (RecurrencePattern pattern : RecurrencePattern.values()) {
            if (pattern.getValue().equalsIgnoreCase(value)) {
                return pattern;
            }
        }
        
        return NONE; // Default
    }
}
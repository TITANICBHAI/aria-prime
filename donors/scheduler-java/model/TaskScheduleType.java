package com.aiassistant.scheduler.model;

/**
 * Task scheduling types
 * Based on ScheduleType enum from advanced_task_scheduler.py
 */
public enum TaskScheduleType {
    ONCE("once"),             // Run once at a specific time
    DAILY("daily"),           // Run daily at a specific time
    WEEKLY("weekly"),         // Run weekly on specific days
    MONTHLY("monthly"),       // Run monthly on specific days
    INTERVAL("interval"),     // Run every X minutes/hours
    CRON("cron"),             // Advanced cron-like scheduling
    TRIGGER("trigger"),       // Triggered by event
    CONDITION("condition"),   // Run when condition is met
    WORKFLOW("workflow");     // Part of a larger workflow
    
    private final String value;
    
    TaskScheduleType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static TaskScheduleType fromString(String text) {
        for (TaskScheduleType type : TaskScheduleType.values()) {
            if (type.value.equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No TaskScheduleType with value: " + text);
    }
}
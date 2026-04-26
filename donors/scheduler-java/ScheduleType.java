package com.aiassistant.scheduler;

import java.io.Serializable;

/**
 * Types of task scheduling
 * Based on ScheduleType enum from advanced_task_scheduler.py
 */
public enum ScheduleType implements Serializable {
    ONCE("once"),           // Run only once at a specific time
    DAILY("daily"),         // Run every day at a specific time
    WEEKLY("weekly"),       // Run on specific days of the week
    MONTHLY("monthly"),     // Run on specific days of the month
    INTERVAL("interval"),   // Run every X minutes/hours
    CRON("cron"),           // Advanced cron-like scheduling
    TRIGGER("trigger"),     // Triggered by event
    CONDITION("condition"), // Run when condition is met
    WORKFLOW("workflow");   // Part of a larger workflow
    
    private final String value;
    
    ScheduleType(String value) {
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
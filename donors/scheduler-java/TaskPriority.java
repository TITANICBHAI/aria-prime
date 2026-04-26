package com.aiassistant.scheduler;

import java.io.Serializable;

/**
 * Priority levels for tasks
 * Based on TaskPriority enum from advanced_task_scheduler.py
 */
public enum TaskPriority implements Serializable {
    LOW(0),       // Low priority tasks that can be delayed if needed
    MEDIUM(1),    // Standard priority tasks
    HIGH(2),      // Important tasks that should be executed promptly
    CRITICAL(3),  // Critical tasks that must be executed immediately
    SYSTEM(4);    // Reserved for system tasks
    
    private final int value;
    
    TaskPriority(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
}
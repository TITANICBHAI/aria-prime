package com.aiassistant.scheduler;

import java.io.Serializable;

/**
 * Event types that can trigger tasks
 * Based on TriggerType enum from advanced_task_scheduler.py
 */
public enum TriggerType implements Serializable {
    APP_LAUNCH("app_launch"),
    APP_CLOSE("app_close"),
    NOTIFICATION("notification"),
    LOCATION("location"),
    TIME("time"),
    BATTERY("battery"),
    CONNECTIVITY("connectivity"),
    SENSOR("sensor"),
    USER_ACTION("user_action"),
    AI_PREDICTION("ai_prediction"),
    EXTERNAL_EVENT("external_event");
    
    private final String value;
    
    TriggerType(String value) {
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
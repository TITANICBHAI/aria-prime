package com.aiassistant.scheduler;

import androidx.annotation.NonNull;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum for different types of task triggers
 */
public enum TaskTriggerType {
    // Time-based triggers
    EXACT_TIME("exact_time"),
    INTERVAL("interval"),
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    
    // Event-based triggers
    APP_LAUNCH("app_launch"),
    APP_EXIT("app_exit"),
    NOTIFICATION("notification"),
    USER_PRESENT("user_present"),
    SCREEN_ON("screen_on"),
    SCREEN_OFF("screen_off"),
    
    // Location-based triggers
    LOCATION_ENTER("location_enter"),
    LOCATION_EXIT("location_exit"),
    
    // Connectivity-based triggers
    CONNECTIVITY_CHANGE("connectivity_change"),
    WIFI_CONNECTED("wifi_connected"),
    WIFI_DISCONNECTED("wifi_disconnected"),
    BLUETOOTH_CONNECTED("bluetooth_connected"),
    BLUETOOTH_DISCONNECTED("bluetooth_disconnected"),
    
    // Battery-based triggers
    BATTERY_LOW("battery_low"),
    BATTERY_OKAY("battery_okay"),
    CHARGING("charging"),
    DISCHARGING("discharging"),
    
    // Custom triggers
    AI_RECOMMENDATION("ai_recommendation"),
    USER_INITIATED("user_initiated"),
    MANUAL("manual"),
    API_RESPONSE("api_response"),
    DATA_CHANGE("data_change"),
    CONDITION_MET("condition_met"),
    SCHEDULED_TASK_COMPLETED("scheduled_task_completed"),
    
    // Unknown or custom
    UNKNOWN("unknown");
    
    private final String value;
    private static final Map<String, TaskTriggerType> VALUE_MAP = new HashMap<>();
    
    static {
        for (TaskTriggerType type : TaskTriggerType.values()) {
            VALUE_MAP.put(type.value, type);
        }
    }
    
    TaskTriggerType(String value) {
        this.value = value;
    }
    
    @NonNull
    public String getValue() {
        return value;
    }
    
    /**
     * Get the TaskTriggerType from a string value
     * 
     * @param value String representation of trigger type
     * @return TaskTriggerType or UNKNOWN if no match
     */
    @NonNull
    public static TaskTriggerType fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        
        String normalizedValue = value.toLowerCase().trim();
        TaskTriggerType type = VALUE_MAP.get(normalizedValue);
        
        if (type == null) {
            // Try to match with common variations
            if (normalizedValue.contains("time") && normalizedValue.contains("exact")) {
                return EXACT_TIME;
            } else if (normalizedValue.contains("interval")) {
                return INTERVAL;
            } else if (normalizedValue.contains("daily")) {
                return DAILY;
            } else if (normalizedValue.contains("weekly")) {
                return WEEKLY;
            } else if (normalizedValue.contains("monthly")) {
                return MONTHLY;
            } else if (normalizedValue.contains("launch") || 
                    (normalizedValue.contains("app") && normalizedValue.contains("start"))) {
                return APP_LAUNCH;
            } else if (normalizedValue.contains("exit") || 
                    (normalizedValue.contains("app") && normalizedValue.contains("close"))) {
                return APP_EXIT;
            } else if (normalizedValue.contains("notif")) {
                return NOTIFICATION;
            } else if (normalizedValue.contains("user") && 
                    (normalizedValue.contains("present") || normalizedValue.contains("unlock"))) {
                return USER_PRESENT;
            } else if (normalizedValue.contains("screen") && normalizedValue.contains("on")) {
                return SCREEN_ON;
            } else if (normalizedValue.contains("screen") && normalizedValue.contains("off")) {
                return SCREEN_OFF;
            } else if (normalizedValue.contains("location") && 
                    (normalizedValue.contains("enter") || normalizedValue.contains("arrive"))) {
                return LOCATION_ENTER;
            } else if (normalizedValue.contains("location") && 
                    (normalizedValue.contains("exit") || normalizedValue.contains("leave"))) {
                return LOCATION_EXIT;
            } else if (normalizedValue.contains("connection") || normalizedValue.contains("connectivity")) {
                return CONNECTIVITY_CHANGE;
            } else if (normalizedValue.contains("wifi") && 
                    (normalizedValue.contains("connect") || normalizedValue.contains("on"))) {
                return WIFI_CONNECTED;
            } else if (normalizedValue.contains("wifi") && 
                    (normalizedValue.contains("disconnect") || normalizedValue.contains("off"))) {
                return WIFI_DISCONNECTED;
            } else if (normalizedValue.contains("bluetooth") && 
                    (normalizedValue.contains("connect") || normalizedValue.contains("on"))) {
                return BLUETOOTH_CONNECTED;
            } else if (normalizedValue.contains("bluetooth") && 
                    (normalizedValue.contains("disconnect") || normalizedValue.contains("off"))) {
                return BLUETOOTH_DISCONNECTED;
            } else if (normalizedValue.contains("battery") && normalizedValue.contains("low")) {
                return BATTERY_LOW;
            } else if (normalizedValue.contains("battery") && 
                    (normalizedValue.contains("okay") || normalizedValue.contains("ok"))) {
                return BATTERY_OKAY;
            } else if (normalizedValue.contains("charg") && !normalizedValue.contains("dis")) {
                return CHARGING;
            } else if (normalizedValue.contains("discharg")) {
                return DISCHARGING;
            } else if (normalizedValue.contains("ai") || normalizedValue.contains("recommend")) {
                return AI_RECOMMENDATION;
            } else if (normalizedValue.contains("user") && normalizedValue.contains("initiate")) {
                return USER_INITIATED;
            } else if (normalizedValue.contains("manual")) {
                return MANUAL;
            } else if (normalizedValue.contains("api") || normalizedValue.contains("response")) {
                return API_RESPONSE;
            } else if (normalizedValue.contains("data") && normalizedValue.contains("change")) {
                return DATA_CHANGE;
            } else if (normalizedValue.contains("condition") || normalizedValue.contains("met")) {
                return CONDITION_MET;
            } else if (normalizedValue.contains("task") && normalizedValue.contains("complete")) {
                return SCHEDULED_TASK_COMPLETED;
            }
            
            return UNKNOWN;
        }
        
        return type;
    }
    
    /**
     * Get a user-friendly display name for the trigger type
     * 
     * @return Display name
     */
    public String getDisplayName() {
        switch (this) {
            case EXACT_TIME:
                return "Exact Time";
            case INTERVAL:
                return "Interval";
            case DAILY:
                return "Daily";
            case WEEKLY:
                return "Weekly";
            case MONTHLY:
                return "Monthly";
            case APP_LAUNCH:
                return "App Launch";
            case APP_EXIT:
                return "App Exit";
            case NOTIFICATION:
                return "Notification";
            case USER_PRESENT:
                return "User Present";
            case SCREEN_ON:
                return "Screen On";
            case SCREEN_OFF:
                return "Screen Off";
            case LOCATION_ENTER:
                return "Location Enter";
            case LOCATION_EXIT:
                return "Location Exit";
            case CONNECTIVITY_CHANGE:
                return "Connectivity Change";
            case WIFI_CONNECTED:
                return "WiFi Connected";
            case WIFI_DISCONNECTED:
                return "WiFi Disconnected";
            case BLUETOOTH_CONNECTED:
                return "Bluetooth Connected";
            case BLUETOOTH_DISCONNECTED:
                return "Bluetooth Disconnected";
            case BATTERY_LOW:
                return "Battery Low";
            case BATTERY_OKAY:
                return "Battery Okay";
            case CHARGING:
                return "Charging";
            case DISCHARGING:
                return "Discharging";
            case AI_RECOMMENDATION:
                return "AI Recommendation";
            case USER_INITIATED:
                return "User Initiated";
            case MANUAL:
                return "Manual";
            case API_RESPONSE:
                return "API Response";
            case DATA_CHANGE:
                return "Data Change";
            case CONDITION_MET:
                return "Condition Met";
            case SCHEDULED_TASK_COMPLETED:
                return "Task Completed";
            default:
                return "Unknown";
        }
    }
    
    @Override
    public String toString() {
        return value;
    }
}
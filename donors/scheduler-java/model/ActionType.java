package com.aiassistant.scheduler.model;

import java.io.Serializable;

/**
 * Types of actions that can be performed by the scheduler
 */
public enum ActionType implements Serializable {
    SYSTEM("system"),               // System-level action
    APP_CONTROL("app_control"),     // Control an app
    NOTIFICATION("notification"),   // Send a notification
    EMAIL("email"),                 // Send an email
    SMS("sms"),                     // Send an SMS
    API_CALL("api_call"),           // Make an API call
    WAIT("wait"),                   // Wait for a specified time
    CONDITION("condition"),         // Check a condition
    CUSTOM("custom"),               // Custom action with custom handler
    AI_ACTION("ai_action"),         // AI-driven action (using learning engine)
    WEB_REQUEST("web_request"),     // Make a web request
    DATABASE("database"),           // Perform a database operation
    FILE_OPERATION("file"),         // Perform a file operation
    INTENT("intent"),               // Send an Android intent
    SCRIPT("script");               // Run a script
    
    private final String value;
    
    ActionType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Get action type from string value
     */
    public static ActionType fromString(String value) {
        for (ActionType type : ActionType.values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        
        return CUSTOM; // Default
    }
}
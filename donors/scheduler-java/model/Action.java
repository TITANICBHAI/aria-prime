package com.aiassistant.scheduler.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Action to be executed by the scheduler
 */
public class Action implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Action properties
    private String actionId;
    private String name;
    private String description;
    private ActionType actionType;
    private Map<String, Object> parameters;
    private Condition condition;          // Condition that must be met to execute this action
    private int maxRetries;               // Maximum number of retries
    private long retryDelayMs;            // Delay between retries in milliseconds
    private long timeoutMs;               // Timeout for action execution in milliseconds
    private String fallbackActionId;      // ID of fallback action to execute if this fails
    private long delayAfterMs;            // Delay after execution in milliseconds
    private boolean requiresConfirmation; // Whether this action requires user confirmation
    
    /**
     * Create a new action
     */
    public Action(ActionType actionType) {
        this.actionId = "act_" + System.currentTimeMillis();
        this.actionType = actionType;
        this.parameters = new HashMap<>();
        this.maxRetries = 0;
        this.retryDelayMs = 1000;
        this.timeoutMs = 30000; // 30 seconds default
        this.delayAfterMs = 0;
        this.requiresConfirmation = false;
    }
    
    /**
     * Create a new action with parameters
     */
    public Action(ActionType actionType, Map<String, Object> parameters) {
        this.actionId = "act_" + System.currentTimeMillis();
        this.actionType = actionType;
        this.parameters = parameters != null ? parameters : new HashMap<>();
        this.maxRetries = 0;
        this.retryDelayMs = 1000;
        this.timeoutMs = 30000; // 30 seconds default
        this.delayAfterMs = 0;
        this.requiresConfirmation = false;
    }
    
    /**
     * Empty constructor for serialization
     */
    public Action() {
        this.actionId = "act_" + System.currentTimeMillis();
        this.parameters = new HashMap<>();
        this.maxRetries = 0;
        this.retryDelayMs = 1000;
        this.timeoutMs = 30000; // 30 seconds default
        this.delayAfterMs = 0;
        this.requiresConfirmation = false;
    }
    
    /**
     * Set a parameter value
     */
    public void setParameter(String key, Object value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.put(key, value);
    }
    
    /**
     * Get a parameter value
     */
    public Object getParameter(String key) {
        if (parameters == null) {
            return null;
        }
        return parameters.get(key);
    }
    
    /**
     * Get a parameter value with default
     */
    public Object getParameter(String key, Object defaultValue) {
        if (parameters == null) {
            return defaultValue;
        }
        return parameters.getOrDefault(key, defaultValue);
    }
    
    /**
     * Check if action has parameter
     */
    public boolean hasParameter(String key) {
        return parameters != null && parameters.containsKey(key);
    }
    
    /**
     * Create a wait action
     */
    public static Action wait(long milliseconds) {
        Action action = new Action(ActionType.WAIT);
        action.setParameter("duration_ms", milliseconds);
        action.setName("Wait for " + milliseconds + " ms");
        return action;
    }
    
    /**
     * Create a notification action
     */
    public static Action notification(String title, String message) {
        Action action = new Action(ActionType.NOTIFICATION);
        action.setParameter("title", title);
        action.setParameter("message", message);
        action.setName("Send notification: " + title);
        return action;
    }
    
    /**
     * Create an app control action
     */
    public static Action appControl(String packageName, String action) {
        Action appAction = new Action(ActionType.APP_CONTROL);
        appAction.setParameter("package_name", packageName);
        appAction.setParameter("action", action);
        appAction.setName("App control: " + action + " " + packageName);
        return appAction;
    }
    
    /**
     * Create an API call action
     */
    public static Action apiCall(String url, String method, Map<String, Object> data) {
        Action action = new Action(ActionType.API_CALL);
        action.setParameter("url", url);
        action.setParameter("method", method);
        action.setParameter("data", data);
        action.setName("API call: " + method + " " + url);
        return action;
    }
    
    /**
     * Create an email action
     */
    public static Action email(String to, String subject, String body) {
        Action action = new Action(ActionType.EMAIL);
        action.setParameter("to", to);
        action.setParameter("subject", subject);
        action.setParameter("body", body);
        action.setName("Send email: " + subject);
        return action;
    }
    
    /**
     * Create an SMS action
     */
    public static Action sms(String to, String message) {
        Action action = new Action(ActionType.SMS);
        action.setParameter("to", to);
        action.setParameter("message", message);
        action.setName("Send SMS to " + to);
        return action;
    }
    
    /**
     * Create a system action
     */
    public static Action system(String command) {
        Action action = new Action(ActionType.SYSTEM);
        action.setParameter("command", command);
        action.setName("System: " + command);
        return action;
    }
    
    /**
     * Create a custom action
     */
    public static Action custom(String handler, Map<String, Object> data) {
        Action action = new Action(ActionType.CUSTOM);
        action.setParameter("handler", handler);
        action.setParameter("data", data);
        action.setName("Custom: " + handler);
        return action;
    }
    
    // Getters and setters
    
    public String getActionId() {
        return actionId;
    }
    
    public void setActionId(String actionId) {
        this.actionId = actionId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public ActionType getActionType() {
        return actionType;
    }
    
    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public Condition getCondition() {
        return condition;
    }
    
    public void setCondition(Condition condition) {
        this.condition = condition;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public long getRetryDelayMs() {
        return retryDelayMs;
    }
    
    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }
    
    public long getTimeoutMs() {
        return timeoutMs;
    }
    
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
    
    public String getFallbackActionId() {
        return fallbackActionId;
    }
    
    public void setFallbackActionId(String fallbackActionId) {
        this.fallbackActionId = fallbackActionId;
    }
    
    public long getDelayAfterMs() {
        return delayAfterMs;
    }
    
    public void setDelayAfterMs(long delayAfterMs) {
        this.delayAfterMs = delayAfterMs;
    }
    
    public boolean isRequiresConfirmation() {
        return requiresConfirmation;
    }
    
    public void setRequiresConfirmation(boolean requiresConfirmation) {
        this.requiresConfirmation = requiresConfirmation;
    }
}
package com.aiassistant.scheduler;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An action that can be performed by the task scheduler
 * Based on Action class from advanced_task_scheduler.py
 */
public class Action implements Serializable {
    private String actionId;
    private ActionType actionType;
    private Map<String, Object> parameters;
    private Condition condition;
    private String fallbackAction;  // ID of fallback action if this fails
    private Integer timeoutSeconds;
    private int retryCount;
    private int retryDelaySeconds;
    private boolean requiresConfirmation;
    
    /**
     * Create an action with generated ID
     */
    public Action(ActionType actionType) {
        this.actionId = UUID.randomUUID().toString();
        this.actionType = actionType;
        this.parameters = new HashMap<>();
        this.condition = null;
        this.fallbackAction = null;
        this.timeoutSeconds = null;
        this.retryCount = 0;
        this.retryDelaySeconds = 0;
        this.requiresConfirmation = false;
    }
    
    /**
     * Create an action with specified ID
     */
    public Action(String actionId, ActionType actionType) {
        this.actionId = actionId;
        this.actionType = actionType;
        this.parameters = new HashMap<>();
        this.condition = null;
        this.fallbackAction = null;
        this.timeoutSeconds = null;
        this.retryCount = 0;
        this.retryDelaySeconds = 0;
        this.requiresConfirmation = false;
    }
    
    /**
     * Create an action with parameters
     */
    public Action(ActionType actionType, Map<String, Object> parameters) {
        this.actionId = UUID.randomUUID().toString();
        this.actionType = actionType;
        this.parameters = parameters != null ? parameters : new HashMap<>();
        this.condition = null;
        this.fallbackAction = null;
        this.timeoutSeconds = null;
        this.retryCount = 0;
        this.retryDelaySeconds = 0;
        this.requiresConfirmation = false;
    }
    
    /**
     * Check if the action should execute based on its condition
     *
     * @param context Dictionary of context variables
     * @return True if the action should execute, False otherwise
     */
    public boolean shouldExecute(Map<String, Object> context) {
        if (condition == null) {
            return true;
        }
        
        return condition.evaluate(context);
    }
    
    /**
     * Add a parameter to the action
     */
    public void addParameter(String key, Object value) {
        parameters.put(key, value);
    }
    
    // Factory methods for common action types
    
    /**
     * Create a notification action
     */
    public static Action createNotification(String title, String message) {
        Action action = new Action(ActionType.NOTIFICATION);
        action.addParameter("title", title);
        action.addParameter("message", message);
        return action;
    }
    
    /**
     * Create an app control action
     */
    public static Action createAppControl(String appPackage, String command) {
        Action action = new Action(ActionType.APP_CONTROL);
        action.addParameter("package", appPackage);
        action.addParameter("command", command);
        return action;
    }
    
    /**
     * Create a system action
     */
    public static Action createSystemAction(String setting, Object value) {
        Action action = new Action(ActionType.SYSTEM_ACTION);
        action.addParameter("setting", setting);
        action.addParameter("value", value);
        return action;
    }
    
    /**
     * Create a wait action
     */
    public static Action createWait(int seconds) {
        Action action = new Action(ActionType.WAIT);
        action.addParameter("duration_seconds", seconds);
        return action;
    }
    
    /**
     * Create a condition action
     */
    public static Action createCondition(Condition condition, String trueActionId, String falseActionId) {
        Action action = new Action(ActionType.CONDITION);
        action.setCondition(condition);
        action.addParameter("true_action", trueActionId);
        action.addParameter("false_action", falseActionId);
        return action;
    }
    
    /**
     * Create an API call action
     */
    public static Action createApiCall(String url, String method, Map<String, Object> data) {
        Action action = new Action(ActionType.API_CALL);
        action.addParameter("url", url);
        action.addParameter("method", method);
        if (data != null) {
            action.addParameter("data", data);
        }
        return action;
    }
    
    // Getters and setters
    
    public String getActionId() {
        return actionId;
    }
    
    public void setActionId(String actionId) {
        this.actionId = actionId;
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
    
    public String getFallbackAction() {
        return fallbackAction;
    }
    
    public void setFallbackAction(String fallbackAction) {
        this.fallbackAction = fallbackAction;
    }
    
    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }
    
    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public int getRetryDelaySeconds() {
        return retryDelaySeconds;
    }
    
    public void setRetryDelaySeconds(int retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }
    
    public boolean isRequiresConfirmation() {
        return requiresConfirmation;
    }
    
    public void setRequiresConfirmation(boolean requiresConfirmation) {
        this.requiresConfirmation = requiresConfirmation;
    }
}
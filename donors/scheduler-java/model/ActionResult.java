package com.aiassistant.scheduler.model;

import java.io.Serializable;

/**
 * Result of an action execution
 * Based on ActionResult class from advanced_task_scheduler.py
 */
public class ActionResult implements Serializable {
    private String actionId;
    private boolean success;
    private Object result;
    private String error;
    private float executionTime;
    
    public ActionResult() {
        this.success = false;
        this.executionTime = 0.0f;
    }
    
    public ActionResult(String actionId, boolean success) {
        this.actionId = actionId;
        this.success = success;
        this.executionTime = 0.0f;
    }
    
    public ActionResult(String actionId, boolean success, Object result) {
        this(actionId, success);
        this.result = result;
    }
    
    public ActionResult(String actionId, boolean success, Object result, String error, float executionTime) {
        this(actionId, success, result);
        this.error = error;
        this.executionTime = executionTime;
    }
    
    // Getters and Setters
    
    public String getActionId() {
        return actionId;
    }
    
    public void setActionId(String actionId) {
        this.actionId = actionId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public Object getResult() {
        return result;
    }
    
    public void setResult(Object result) {
        this.result = result;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public float getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(float executionTime) {
        this.executionTime = executionTime;
    }
}
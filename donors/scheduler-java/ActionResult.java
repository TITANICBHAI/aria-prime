package com.aiassistant.scheduler;

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
    private double executionTime;
    
    /**
     * Create a successful result
     */
    public ActionResult(String actionId, boolean success) {
        this.actionId = actionId;
        this.success = success;
        this.result = null;
        this.error = null;
        this.executionTime = 0.0;
    }
    
    /**
     * Create a result with data
     */
    public ActionResult(String actionId, boolean success, Object result) {
        this.actionId = actionId;
        this.success = success;
        this.result = result;
        this.error = null;
        this.executionTime = 0.0;
    }
    
    /**
     * Create a result with error
     */
    public ActionResult(String actionId, boolean success, String error) {
        this.actionId = actionId;
        this.success = success;
        this.result = null;
        this.error = error;
        this.executionTime = 0.0;
    }
    
    /**
     * Create a complete result
     */
    public ActionResult(String actionId, boolean success, Object result, String error, double executionTime) {
        this.actionId = actionId;
        this.success = success;
        this.result = result;
        this.error = error;
        this.executionTime = executionTime;
    }
    
    /**
     * Create a successful result
     */
    public static ActionResult success(String actionId) {
        return new ActionResult(actionId, true);
    }
    
    /**
     * Create a successful result with data
     */
    public static ActionResult success(String actionId, Object result) {
        return new ActionResult(actionId, true, result);
    }
    
    /**
     * Create a successful result with execution time
     */
    public static ActionResult success(String actionId, Object result, double executionTime) {
        ActionResult actionResult = new ActionResult(actionId, true, result);
        actionResult.setExecutionTime(executionTime);
        return actionResult;
    }
    
    /**
     * Create a failed result
     */
    public static ActionResult failure(String actionId, String error) {
        return new ActionResult(actionId, false, error);
    }
    
    /**
     * Create a failed result with execution time
     */
    public static ActionResult failure(String actionId, String error, double executionTime) {
        ActionResult actionResult = new ActionResult(actionId, false, null);
        actionResult.setError(error);
        actionResult.setExecutionTime(executionTime);
        return actionResult;
    }
    
    // Getters and setters
    
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
    
    public double getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(double executionTime) {
        this.executionTime = executionTime;
    }
}
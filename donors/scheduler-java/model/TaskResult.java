package com.aiassistant.scheduler.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Result of a task execution
 * Stores detailed information about the execution outcome
 */
public class TaskResult implements Serializable {
    private String resultId;
    private String taskId;
    private boolean success;
    private Date startTime;
    private Date endTime;
    private float executionTime;  // In seconds
    private String errorMessage;
    private List<ActionResult> actionResults;
    private Map<String, Object> outputData;
    private Map<String, Object> metrics;
    private String executionEnvironment;
    
    public TaskResult() {
        this.resultId = UUID.randomUUID().toString();
        this.startTime = new Date();
        this.actionResults = new ArrayList<>();
        this.outputData = new HashMap<>();
        this.metrics = new HashMap<>();
        this.success = true;  // Default to success, set to false on error
    }
    
    public TaskResult(String taskId) {
        this();
        this.taskId = taskId;
    }
    
    /**
     * Mark the result as completed
     * 
     * @param success Whether the task was successful
     * @param errorMessage Error message if the task failed
     */
    public void complete(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.endTime = new Date();
        this.executionTime = (this.endTime.getTime() - this.startTime.getTime()) / 1000f;
    }
    
    /**
     * Add an action result to this task result
     * 
     * @param actionResult Result of an action execution
     */
    public void addActionResult(ActionResult actionResult) {
        this.actionResults.add(actionResult);
        
        // If any action failed, mark the task result as failed
        if (!actionResult.isSuccess() && this.success) {
            this.success = false;
            this.errorMessage = "Action failed: " + actionResult.getError();
        }
    }
    
    /**
     * Add output data from the task execution
     * 
     * @param key Key for the output data
     * @param value Value of the output data
     */
    public void addOutputData(String key, Object value) {
        this.outputData.put(key, value);
    }
    
    /**
     * Add a metric from the task execution
     * 
     * @param key Key for the metric
     * @param value Value of the metric
     */
    public void addMetric(String key, Object value) {
        this.metrics.put(key, value);
    }
    
    // Getters and Setters
    
    public String getResultId() {
        return resultId;
    }
    
    public void setResultId(String resultId) {
        this.resultId = resultId;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public Date getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }
    
    public Date getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
    
    public float getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(float executionTime) {
        this.executionTime = executionTime;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public List<ActionResult> getActionResults() {
        return actionResults;
    }
    
    public void setActionResults(List<ActionResult> actionResults) {
        this.actionResults = actionResults != null ? actionResults : new ArrayList<>();
    }
    
    public Map<String, Object> getOutputData() {
        return outputData;
    }
    
    public void setOutputData(Map<String, Object> outputData) {
        this.outputData = outputData != null ? outputData : new HashMap<>();
    }
    
    public Map<String, Object> getMetrics() {
        return metrics;
    }
    
    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics != null ? metrics : new HashMap<>();
    }
    
    public String getExecutionEnvironment() {
        return executionEnvironment;
    }
    
    public void setExecutionEnvironment(String executionEnvironment) {
        this.executionEnvironment = executionEnvironment;
    }
}
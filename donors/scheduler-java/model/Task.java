package com.aiassistant.scheduler.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Advanced scheduled task with improved capabilities
 * Based on ScheduledTask class from advanced_task_scheduler.py
 */
public class Task implements Serializable {
    private String id;
    private String name;
    private String description;
    private TaskScheduleType scheduleType;
    private ActionSequence actionSequence;
    private TaskPriority priority;
    private TaskStatus status;
    private Date createdAt;
    private Date updatedAt;
    private Date scheduledTime;
    private Date nextRun;
    private Date lastRun;
    private Map<String, Object> scheduleParams;
    private Map<String, Object> taskParams;
    private List<Trigger> triggers;
    private Condition condition;
    private boolean enabled;
    private int retryCount;
    private int retryDelay;
    private int errorCount;
    private int successCount;
    private boolean requiresCharging;
    private int batteryLevelMin;
    private boolean requiresNetwork;
    private String targetNetworkType;
    private List<String> tags;
    private String userId;
    private String deviceId;
    
    // Learning-related fields
    private float confidenceScore;
    private Map<String, Float> learningData;
    
    public Task() {
        this.id = UUID.randomUUID().toString();
        this.scheduleParams = new HashMap<>();
        this.taskParams = new HashMap<>();
        this.triggers = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.enabled = true;
        this.status = TaskStatus.PENDING;
        this.priority = TaskPriority.NORMAL;
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.retryCount = 0;
        this.retryDelay = 0;
        this.errorCount = 0;
        this.successCount = 0;
        this.requiresCharging = false;
        this.batteryLevelMin = 0;
        this.requiresNetwork = false;
        this.confidenceScore = 0.5f;
        this.learningData = new HashMap<>();
    }
    
    /**
     * Check if the task should execute based on its conditions
     *
     * @param context Dictionary of context variables
     * @return True if the task should execute, False otherwise
     */
    public boolean shouldExecute(Map<String, Object> context) {
        if (condition != null) {
            return condition.evaluate(context);
        }
        return true;
    }
    
    /**
     * Calculate the next run time based on schedule type and parameters
     *
     * @return The next run time or null if no future runs
     */
    public Date calculateNextRun() {
        Calendar calendar = Calendar.getInstance();
        
        // If scheduled time is in the future, use that
        if (scheduledTime != null && scheduledTime.after(new Date())) {
            return scheduledTime;
        }
        
        // Use current time as base for calculation
        calendar.setTime(new Date());
        
        switch (scheduleType) {
            case ONCE:
                // For one-time tasks, return the scheduled time
                return scheduledTime;
                
            case DAILY:
                // Set time to specified hour and minute
                if (scheduleParams.containsKey("hour") && scheduleParams.containsKey("minute")) {
                    int hour = (int) scheduleParams.get("hour");
                    int minute = (int) scheduleParams.get("minute");
                    
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    
                    // If already past this time today, move to tomorrow
                    if (calendar.getTime().before(new Date())) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1);
                    }
                } else {
                    // Default to same time tomorrow
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }
                break;
                
            case WEEKLY:
                // Set to specified day of week and time
                if (scheduleParams.containsKey("day_of_week") && 
                    scheduleParams.containsKey("hour") && 
                    scheduleParams.containsKey("minute")) {
                    
                    int dayOfWeek = (int) scheduleParams.get("day_of_week");
                    int hour = (int) scheduleParams.get("hour");
                    int minute = (int) scheduleParams.get("minute");
                    
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    
                    // Calculate days until next occurrence
                    int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                    int daysUntil = (dayOfWeek - currentDayOfWeek + 7) % 7;
                    
                    // If today and already past time, add a week
                    if (daysUntil == 0 && calendar.getTime().before(new Date())) {
                        daysUntil = 7;
                    }
                    
                    calendar.add(Calendar.DAY_OF_YEAR, daysUntil);
                } else {
                    // Default to same day next week
                    calendar.add(Calendar.WEEK_OF_YEAR, 1);
                }
                break;
                
            case MONTHLY:
                // Set to specified day of month and time
                if (scheduleParams.containsKey("day_of_month") && 
                    scheduleParams.containsKey("hour") && 
                    scheduleParams.containsKey("minute")) {
                    
                    int dayOfMonth = (int) scheduleParams.get("day_of_month");
                    int hour = (int) scheduleParams.get("hour");
                    int minute = (int) scheduleParams.get("minute");
                    
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    
                    // If this date already passed, move to next month
                    if (calendar.getTime().before(new Date())) {
                        calendar.add(Calendar.MONTH, 1);
                    }
                } else {
                    // Default to same day next month
                    calendar.add(Calendar.MONTH, 1);
                }
                break;
                
            case INTERVAL:
                // Calculate next run based on interval
                if (scheduleParams.containsKey("interval_minutes")) {
                    int intervalMinutes = (int) scheduleParams.get("interval_minutes");
                    
                    // If we have a last run, use that as reference
                    if (lastRun != null) {
                        calendar.setTime(lastRun);
                        calendar.add(Calendar.MINUTE, intervalMinutes);
                    } else {
                        // Otherwise use current time + interval
                        calendar.add(Calendar.MINUTE, intervalMinutes);
                    }
                } else if (scheduleParams.containsKey("interval_hours")) {
                    int intervalHours = (int) scheduleParams.get("interval_hours");
                    
                    // If we have a last run, use that as reference
                    if (lastRun != null) {
                        calendar.setTime(lastRun);
                        calendar.add(Calendar.HOUR_OF_DAY, intervalHours);
                    } else {
                        // Otherwise use current time + interval
                        calendar.add(Calendar.HOUR_OF_DAY, intervalHours);
                    }
                } else {
                    // Default to every hour
                    calendar.add(Calendar.HOUR_OF_DAY, 1);
                }
                break;
                
            case CRON:
                // Cron-like scheduling would require a cron parser library
                // For now, default to same time tomorrow
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                break;
                
            case TRIGGER:
            case CONDITION:
            case WORKFLOW:
                // These types don't have a fixed schedule
                return null;
                
            default:
                // Unknown schedule type
                return null;
        }
        
        return calendar.getTime();
    }
    
    /**
     * Apply learning from task execution
     *
     * @param executionData Data from the execution
     * @param learningRate Learning rate (0.0-1.0)
     */
    public void learnFromExecution(Map<String, Object> executionData, float learningRate) {
        if (executionData.containsKey("success")) {
            boolean success = (boolean) executionData.get("success");
            
            if (success) {
                successCount++;
                
                // Increase confidence based on success
                confidenceScore = confidenceScore + (1.0f - confidenceScore) * learningRate;
            } else {
                errorCount++;
                
                // Decrease confidence based on failure
                confidenceScore = confidenceScore * (1.0f - learningRate);
            }
        }
        
        // Learn from other execution data
        for (Map.Entry<String, Object> entry : executionData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Number) {
                float numValue = ((Number) value).floatValue();
                
                // Update learning data with exponential moving average
                if (learningData.containsKey(key)) {
                    float oldValue = learningData.get(key);
                    float newValue = oldValue * (1.0f - learningRate) + numValue * learningRate;
                    learningData.put(key, newValue);
                } else {
                    learningData.put(key, numValue);
                }
            }
        }
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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
    
    public TaskScheduleType getScheduleType() {
        return scheduleType;
    }
    
    public void setScheduleType(TaskScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }
    
    public ActionSequence getActionSequence() {
        return actionSequence;
    }
    
    public void setActionSequence(ActionSequence actionSequence) {
        this.actionSequence = actionSequence;
    }
    
    public TaskPriority getPriority() {
        return priority;
    }
    
    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Date getScheduledTime() {
        return scheduledTime;
    }
    
    public void setScheduledTime(Date scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
    
    public Date getNextRun() {
        return nextRun;
    }
    
    public void setNextRun(Date nextRun) {
        this.nextRun = nextRun;
    }
    
    public Date getLastRun() {
        return lastRun;
    }
    
    public void setLastRun(Date lastRun) {
        this.lastRun = lastRun;
    }
    
    public Map<String, Object> getScheduleParams() {
        return scheduleParams;
    }
    
    public void setScheduleParams(Map<String, Object> scheduleParams) {
        this.scheduleParams = scheduleParams != null ? scheduleParams : new HashMap<>();
    }
    
    public void addScheduleParam(String key, Object value) {
        this.scheduleParams.put(key, value);
    }
    
    public Map<String, Object> getTaskParams() {
        return taskParams;
    }
    
    public void setTaskParams(Map<String, Object> taskParams) {
        this.taskParams = taskParams != null ? taskParams : new HashMap<>();
    }
    
    public void addTaskParam(String key, Object value) {
        this.taskParams.put(key, value);
    }
    
    public List<Trigger> getTriggers() {
        return triggers;
    }
    
    public void setTriggers(List<Trigger> triggers) {
        this.triggers = triggers != null ? triggers : new ArrayList<>();
    }
    
    public void addTrigger(Trigger trigger) {
        this.triggers.add(trigger);
    }
    
    public Condition getCondition() {
        return condition;
    }
    
    public void setCondition(Condition condition) {
        this.condition = condition;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public int getRetryDelay() {
        return retryDelay;
    }
    
    public void setRetryDelay(int retryDelay) {
        this.retryDelay = retryDelay;
    }
    
    public int getErrorCount() {
        return errorCount;
    }
    
    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }
    
    public int getSuccessCount() {
        return successCount;
    }
    
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }
    
    public boolean isRequiresCharging() {
        return requiresCharging;
    }
    
    public void setRequiresCharging(boolean requiresCharging) {
        this.requiresCharging = requiresCharging;
    }
    
    public int getBatteryLevelMin() {
        return batteryLevelMin;
    }
    
    public void setBatteryLevelMin(int batteryLevelMin) {
        this.batteryLevelMin = batteryLevelMin;
    }
    
    public boolean isRequiresNetwork() {
        return requiresNetwork;
    }
    
    public void setRequiresNetwork(boolean requiresNetwork) {
        this.requiresNetwork = requiresNetwork;
    }
    
    public String getTargetNetworkType() {
        return targetNetworkType;
    }
    
    public void setTargetNetworkType(String targetNetworkType) {
        this.targetNetworkType = targetNetworkType;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }
    
    public void addTag(String tag) {
        this.tags.add(tag);
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public float getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(float confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
    public Map<String, Float> getLearningData() {
        return learningData;
    }
    
    public void setLearningData(Map<String, Float> learningData) {
        this.learningData = learningData != null ? learningData : new HashMap<>();
    }
}
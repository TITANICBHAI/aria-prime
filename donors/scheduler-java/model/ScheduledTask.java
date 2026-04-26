package com.aiassistant.scheduler.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Scheduled task for execution by the scheduler
 */
public class ScheduledTask implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Task properties
    private String taskId;
    private String name;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private ActionSequence sequence;
    private Trigger trigger;
    private Condition preExecutionCondition;
    private Condition postExecutionCondition;
    private boolean recurring;
    private RecurrencePattern recurrencePattern;
    private int recurrenceInterval;
    private Date scheduledTime;
    private Date createdTime;
    private Date lastExecutionTime;
    private Date lastCompletionTime;
    private Date expirationTime;
    private int executionCount;
    private int successCount;
    private int failureCount;
    private long maxExecutionTimeMs;
    private Map<String, Object> metadata;
    private String category;
    private String icon;
    private boolean enabled;
    
    /**
     * Create a new scheduled task
     */
    public ScheduledTask(String name, ActionSequence sequence) {
        this.taskId = "task_" + System.currentTimeMillis();
        this.name = name;
        this.sequence = sequence;
        this.priority = TaskPriority.MEDIUM;
        this.status = TaskStatus.PENDING;
        this.recurring = false;
        this.recurrencePattern = RecurrencePattern.NONE;
        this.recurrenceInterval = 0;
        this.scheduledTime = new Date();
        this.createdTime = new Date();
        this.executionCount = 0;
        this.successCount = 0;
        this.failureCount = 0;
        this.maxExecutionTimeMs = 0; // No limit
        this.metadata = new HashMap<>();
        this.category = "custom";
        this.icon = "task";
        this.enabled = true;
    }
    
    /**
     * Create a new scheduled task with a trigger
     */
    public ScheduledTask(String name, ActionSequence sequence, Trigger trigger) {
        this(name, sequence);
        this.trigger = trigger;
    }
    
    /**
     * Empty constructor for serialization
     */
    public ScheduledTask() {
        this.taskId = "task_" + System.currentTimeMillis();
        this.priority = TaskPriority.MEDIUM;
        this.status = TaskStatus.PENDING;
        this.recurring = false;
        this.recurrencePattern = RecurrencePattern.NONE;
        this.recurrenceInterval = 0;
        this.scheduledTime = new Date();
        this.createdTime = new Date();
        this.executionCount = 0;
        this.successCount = 0;
        this.failureCount = 0;
        this.maxExecutionTimeMs = 0; // No limit
        this.metadata = new HashMap<>();
        this.category = "custom";
        this.icon = "task";
        this.enabled = true;
    }
    
    /**
     * Set a metadata value
     */
    public void setMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }
    
    /**
     * Get a metadata value
     */
    public Object getMetadata(String key) {
        if (metadata == null) {
            return null;
        }
        return metadata.get(key);
    }
    
    /**
     * Get a metadata value with default
     */
    public Object getMetadata(String key, Object defaultValue) {
        if (metadata == null) {
            return defaultValue;
        }
        return metadata.getOrDefault(key, defaultValue);
    }
    
    /**
     * Create a one-time task
     */
    public static ScheduledTask oneTime(String name, ActionSequence sequence, Date scheduledTime) {
        ScheduledTask task = new ScheduledTask(name, sequence);
        task.setScheduledTime(scheduledTime);
        task.setRecurring(false);
        return task;
    }
    
    /**
     * Create a recurring task
     */
    public static ScheduledTask recurring(String name, ActionSequence sequence, 
                                        RecurrencePattern pattern, int interval) {
        ScheduledTask task = new ScheduledTask(name, sequence);
        task.setRecurring(true);
        task.setRecurrencePattern(pattern);
        task.setRecurrenceInterval(interval);
        return task;
    }
    
    /**
     * Create a daily task
     */
    public static ScheduledTask daily(String name, ActionSequence sequence, int hour, int minute) {
        ScheduledTask task = new ScheduledTask(name, sequence);
        task.setRecurring(true);
        task.setRecurrencePattern(RecurrencePattern.DAILY);
        task.setRecurrenceInterval(1);
        
        // Set scheduled time to today at specified hour/minute
        Date scheduledTime = new Date();
        scheduledTime.setHours(hour);
        scheduledTime.setMinutes(minute);
        scheduledTime.setSeconds(0);
        
        // If time is in the past, move to tomorrow
        if (scheduledTime.before(new Date())) {
            scheduledTime.setTime(scheduledTime.getTime() + (24 * 60 * 60 * 1000));
        }
        
        task.setScheduledTime(scheduledTime);
        return task;
    }
    
    /**
     * Create an immediate task
     */
    public static ScheduledTask immediate(String name, ActionSequence sequence) {
        ScheduledTask task = new ScheduledTask(name, sequence);
        task.setScheduledTime(new Date());
        task.setRecurring(false);
        task.setTrigger(Trigger.immediate());
        return task;
    }
    
    /**
     * Create a high priority task
     */
    public static ScheduledTask highPriority(String name, ActionSequence sequence) {
        ScheduledTask task = new ScheduledTask(name, sequence);
        task.setPriority(TaskPriority.HIGH);
        return task;
    }
    
    // Getters and setters
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
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
    
    public ActionSequence getSequence() {
        return sequence;
    }
    
    public void setSequence(ActionSequence sequence) {
        this.sequence = sequence;
    }
    
    public Trigger getTrigger() {
        return trigger;
    }
    
    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }
    
    public Condition getPreExecutionCondition() {
        return preExecutionCondition;
    }
    
    public void setPreExecutionCondition(Condition preExecutionCondition) {
        this.preExecutionCondition = preExecutionCondition;
    }
    
    public Condition getPostExecutionCondition() {
        return postExecutionCondition;
    }
    
    public void setPostExecutionCondition(Condition postExecutionCondition) {
        this.postExecutionCondition = postExecutionCondition;
    }
    
    public boolean isRecurring() {
        return recurring;
    }
    
    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }
    
    public RecurrencePattern getRecurrencePattern() {
        return recurrencePattern;
    }
    
    public void setRecurrencePattern(RecurrencePattern recurrencePattern) {
        this.recurrencePattern = recurrencePattern;
    }
    
    public int getRecurrenceInterval() {
        return recurrenceInterval;
    }
    
    public void setRecurrenceInterval(int recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
    }
    
    public Date getScheduledTime() {
        return scheduledTime;
    }
    
    public void setScheduledTime(Date scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
    
    public Date getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }
    
    public Date getLastExecutionTime() {
        return lastExecutionTime;
    }
    
    public void setLastExecutionTime(Date lastExecutionTime) {
        this.lastExecutionTime = lastExecutionTime;
    }
    
    public Date getLastCompletionTime() {
        return lastCompletionTime;
    }
    
    public void setLastCompletionTime(Date lastCompletionTime) {
        this.lastCompletionTime = lastCompletionTime;
    }
    
    public Date getExpirationTime() {
        return expirationTime;
    }
    
    public void setExpirationTime(Date expirationTime) {
        this.expirationTime = expirationTime;
    }
    
    public int getExecutionCount() {
        return executionCount;
    }
    
    public void setExecutionCount(int executionCount) {
        this.executionCount = executionCount;
    }
    
    public int getSuccessCount() {
        return successCount;
    }
    
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }
    
    public int getFailureCount() {
        return failureCount;
    }
    
    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }
    
    public long getMaxExecutionTimeMs() {
        return maxExecutionTimeMs;
    }
    
    public void setMaxExecutionTimeMs(long maxExecutionTimeMs) {
        this.maxExecutionTimeMs = maxExecutionTimeMs;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
package com.aiassistant.scheduler;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a scheduled task with its execution details
 * Based on the ScheduledTask class from advanced_task_scheduler.py
 */
public class ScheduledTask implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String taskId;
    private String name;
    private String description;
    private ScheduleType scheduleType;
    private ActionSequence sequence;
    private TaskPriority priority;
    private TaskStatus status;
    private long createdAt;
    private long updatedAt;
    private long triggerTime;
    private long lastRunStarted;
    private long lastRunCompleted;
    private boolean lastRunSuccess;
    private String lastRunMessage;
    private int executionCount;
    private long intervalMillis;
    private int hourOfDay;
    private int minute;
    private int[] daysOfWeek;
    private Condition condition;
    private String owner;
    private boolean enabled;
    
    /**
     * Create a new scheduled task
     */
    public ScheduledTask(String taskId, String name, String description,
                         ScheduleType scheduleType, ActionSequence sequence,
                         TaskPriority priority) {
        this.taskId = taskId;
        this.name = name;
        this.description = description;
        this.scheduleType = scheduleType;
        this.sequence = sequence;
        this.priority = priority;
        this.status = TaskStatus.PENDING;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.triggerTime = 0;
        this.lastRunStarted = 0;
        this.lastRunCompleted = 0;
        this.lastRunSuccess = false;
        this.lastRunMessage = "";
        this.executionCount = 0;
        this.intervalMillis = 0;
        this.hourOfDay = 0;
        this.minute = 0;
        this.daysOfWeek = new int[0];
        this.condition = null;
        this.owner = "user";
        this.enabled = true;
    }
    
    // Getters and setters
    
    public String getTaskId() {
        return taskId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public ScheduleType getScheduleType() {
        return scheduleType;
    }
    
    public ActionSequence getSequence() {
        return sequence;
    }
    
    public void setSequence(ActionSequence sequence) {
        this.sequence = sequence;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public TaskPriority getPriority() {
        return priority;
    }
    
    public void setPriority(TaskPriority priority) {
        this.priority = priority;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public long getTriggerTime() {
        return triggerTime;
    }
    
    public void setTriggerTime(long triggerTime) {
        this.triggerTime = triggerTime;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getLastRunStarted() {
        return lastRunStarted;
    }
    
    public void setLastRunStarted(long lastRunStarted) {
        this.lastRunStarted = lastRunStarted;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getLastRunCompleted() {
        return lastRunCompleted;
    }
    
    public void setLastRunCompleted(long lastRunCompleted) {
        this.lastRunCompleted = lastRunCompleted;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public boolean isLastRunSuccess() {
        return lastRunSuccess;
    }
    
    public void setLastRunSuccess(boolean lastRunSuccess) {
        this.lastRunSuccess = lastRunSuccess;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getLastRunMessage() {
        return lastRunMessage;
    }
    
    public void setLastRunMessage(String lastRunMessage) {
        this.lastRunMessage = lastRunMessage;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public int getExecutionCount() {
        return executionCount;
    }
    
    public void incrementExecutionCount() {
        this.executionCount++;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getIntervalMillis() {
        return intervalMillis;
    }
    
    public void setIntervalMillis(long intervalMillis) {
        this.intervalMillis = intervalMillis;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public int getHourOfDay() {
        return hourOfDay;
    }
    
    public void setHourOfDay(int hourOfDay) {
        this.hourOfDay = hourOfDay;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public int getMinute() {
        return minute;
    }
    
    public void setMinute(int minute) {
        this.minute = minute;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public int[] getDaysOfWeek() {
        return daysOfWeek;
    }
    
    public void setDaysOfWeek(int[] daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Condition getCondition() {
        return condition;
    }
    
    public void setCondition(Condition condition) {
        this.condition = condition;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getOwner() {
        return owner;
    }
    
    public void setOwner(String owner) {
        this.owner = owner;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * Get next run time as a formatted string
     */
    public String getNextRunTimeString() {
        if (triggerTime > 0) {
            return new Date(triggerTime).toString();
        }
        return "Not scheduled";
    }
    
    /**
     * Get last run time as a formatted string
     */
    public String getLastRunTimeString() {
        if (lastRunCompleted > 0) {
            return new Date(lastRunCompleted).toString();
        }
        return "Never run";
    }
    
    @Override
    public String toString() {
        return name + " (" + taskId + "): " + status;
    }
}
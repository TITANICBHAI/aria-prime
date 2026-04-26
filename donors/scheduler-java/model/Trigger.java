package com.aiassistant.scheduler.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Trigger for starting a task
 */
public class Trigger implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Trigger properties
    private String triggerId;
    private String name;
    private String description;
    private TriggerType triggerType;
    private Map<String, Object> parameters;
    private Condition condition;
    
    /**
     * Create a new trigger
     */
    public Trigger(TriggerType triggerType) {
        this.triggerId = "trig_" + System.currentTimeMillis();
        this.triggerType = triggerType;
        this.parameters = new HashMap<>();
    }
    
    /**
     * Create a new trigger with parameters
     */
    public Trigger(TriggerType triggerType, Map<String, Object> parameters) {
        this.triggerId = "trig_" + System.currentTimeMillis();
        this.triggerType = triggerType;
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }
    
    /**
     * Create a new trigger with condition
     */
    public Trigger(TriggerType triggerType, Map<String, Object> parameters, Condition condition) {
        this.triggerId = "trig_" + System.currentTimeMillis();
        this.triggerType = triggerType;
        this.parameters = parameters != null ? parameters : new HashMap<>();
        this.condition = condition;
    }
    
    /**
     * Empty constructor for serialization
     */
    public Trigger() {
        this.triggerId = "trig_" + System.currentTimeMillis();
        this.parameters = new HashMap<>();
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
     * Check if trigger has parameter
     */
    public boolean hasParameter(String key) {
        return parameters != null && parameters.containsKey(key);
    }
    
    /**
     * Create an immediate trigger
     */
    public static Trigger immediate() {
        return new Trigger(TriggerType.IMMEDIATE);
    }
    
    /**
     * Create a scheduled trigger
     */
    public static Trigger scheduled() {
        return new Trigger(TriggerType.SCHEDULED);
    }
    
    /**
     * Create an app launch trigger
     */
    public static Trigger appLaunch(String packageName) {
        Trigger trigger = new Trigger(TriggerType.APP_LAUNCH);
        trigger.setParameter("package_name", packageName);
        return trigger;
    }
    
    /**
     * Create a time range trigger
     */
    public static Trigger timeRange(int startHour, int startMinute, int endHour, int endMinute) {
        Trigger trigger = new Trigger(TriggerType.TIME_RANGE);
        trigger.setParameter("start_hour", startHour);
        trigger.setParameter("start_minute", startMinute);
        trigger.setParameter("end_hour", endHour);
        trigger.setParameter("end_minute", endMinute);
        return trigger;
    }
    
    /**
     * Create a battery level trigger
     */
    public static Trigger batteryLevel(int level, boolean below) {
        Trigger trigger = new Trigger(TriggerType.BATTERY);
        trigger.setParameter("level", level);
        trigger.setParameter("below", below);
        return trigger;
    }
    
    /**
     * Create a connectivity change trigger
     */
    public static Trigger connectivity(String type) {
        Trigger trigger = new Trigger(TriggerType.CONNECTIVITY);
        trigger.setParameter("type", type);
        return trigger;
    }
    
    // Getters and setters
    
    public String getTriggerId() {
        return triggerId;
    }
    
    public void setTriggerId(String triggerId) {
        this.triggerId = triggerId;
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
    
    public TriggerType getTriggerType() {
        return triggerType;
    }
    
    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
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
}
package com.aiassistant.scheduler;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A trigger that can start a task
 * Based on Trigger class from advanced_task_scheduler.py
 */
public class Trigger implements Serializable {
    private TriggerType triggerType;
    private Map<String, Object> parameters;
    private Condition condition;
    
    /**
     * Create a trigger with just a type
     */
    public Trigger(TriggerType triggerType) {
        this.triggerType = triggerType;
        this.parameters = new HashMap<>();
        this.condition = null;
    }
    
    /**
     * Create a trigger with parameters
     */
    public Trigger(TriggerType triggerType, Map<String, Object> parameters) {
        this.triggerType = triggerType;
        this.parameters = parameters != null ? parameters : new HashMap<>();
        this.condition = null;
    }
    
    /**
     * Create a trigger with parameters and condition
     */
    public Trigger(TriggerType triggerType, Map<String, Object> parameters, Condition condition) {
        this.triggerType = triggerType;
        this.parameters = parameters != null ? parameters : new HashMap<>();
        this.condition = condition;
    }
    
    /**
     * Check if an event matches this trigger
     *
     * @param event Event data
     * @param context Context data
     * @return True if the event matches, False otherwise
     */
    public boolean matches(Map<String, Object> event, Map<String, Object> context) {
        // Check event type
        if (!event.containsKey("type") || !event.get("type").equals(triggerType.getValue())) {
            return false;
        }
        
        // Check parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (!event.containsKey(key) || !event.get(key).equals(value)) {
                return false;
            }
        }
        
        // Check condition if present
        if (condition != null) {
            return condition.evaluate(context);
        }
        
        return true;
    }
    
    // Getters and setters
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
    
    /**
     * Add a parameter to the trigger
     */
    public void addParameter(String key, Object value) {
        parameters.put(key, value);
    }
    
    /**
     * Create a time-based trigger
     */
    public static Trigger timeTrigger(String timeSpec) {
        Trigger trigger = new Trigger(TriggerType.TIME);
        trigger.addParameter("time_spec", timeSpec);
        return trigger;
    }
    
    /**
     * Create an app launch trigger
     */
    public static Trigger appLaunchTrigger(String packageName) {
        Trigger trigger = new Trigger(TriggerType.APP_LAUNCH);
        trigger.addParameter("package_name", packageName);
        return trigger;
    }
    
    /**
     * Create an app close trigger
     */
    public static Trigger appCloseTrigger(String packageName) {
        Trigger trigger = new Trigger(TriggerType.APP_CLOSE);
        trigger.addParameter("package_name", packageName);
        return trigger;
    }
    
    /**
     * Create a notification trigger
     */
    public static Trigger notificationTrigger(String packageName, String titlePattern) {
        Trigger trigger = new Trigger(TriggerType.NOTIFICATION);
        trigger.addParameter("package_name", packageName);
        if (titlePattern != null) {
            trigger.addParameter("title_pattern", titlePattern);
        }
        return trigger;
    }
    
    /**
     * Create a battery level trigger
     */
    public static Trigger batteryTrigger(int level, boolean isLow) {
        Trigger trigger = new Trigger(TriggerType.BATTERY);
        trigger.addParameter("level", level);
        trigger.addParameter("is_low", isLow);
        return trigger;
    }
}
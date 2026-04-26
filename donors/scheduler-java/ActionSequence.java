package com.aiassistant.scheduler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A sequence of actions to be executed
 * Based on ActionSequence class from advanced_task_scheduler.py
 */
public class ActionSequence implements Serializable {
    private String sequenceId;
    private String name;
    private String description;
    private List<Action> actions;
    private boolean parallel;  // Execute actions in parallel or sequence
    private Integer maxDurationSeconds;
    private boolean requiresConfirmation;
    private boolean stopOnError;
    private String version;
    private String createdBy;
    private String icon;
    private String category;
    
    /**
     * Create a new action sequence
     */
    public ActionSequence(String name) {
        this.sequenceId = UUID.randomUUID().toString();
        this.name = name;
        this.description = "";
        this.actions = new ArrayList<>();
        this.parallel = false;
        this.maxDurationSeconds = null;
        this.requiresConfirmation = false;
        this.stopOnError = true;
        this.version = "1.0";
        this.createdBy = "user";
        this.icon = "action";
        this.category = "custom";
    }
    
    /**
     * Create a new action sequence with specified ID
     */
    public ActionSequence(String sequenceId, String name) {
        this.sequenceId = sequenceId;
        this.name = name;
        this.description = "";
        this.actions = new ArrayList<>();
        this.parallel = false;
        this.maxDurationSeconds = null;
        this.requiresConfirmation = false;
        this.stopOnError = true;
        this.version = "1.0";
        this.createdBy = "user";
        this.icon = "action";
        this.category = "custom";
    }
    
    /**
     * Get an action by its ID
     *
     * @param actionId The action ID to look for
     * @return The action or null if not found
     */
    public Action getActionById(String actionId) {
        for (Action action : actions) {
            if (action.getActionId().equals(actionId)) {
                return action;
            }
        }
        return null;
    }
    
    /**
     * Add an action to the sequence
     *
     * @param action The action to add
     */
    public void addAction(Action action) {
        actions.add(action);
    }
    
    /**
     * Remove an action from the sequence
     *
     * @param actionId The ID of the action to remove
     * @return True if the action was removed, false if not found
     */
    public boolean removeAction(String actionId) {
        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i).getActionId().equals(actionId)) {
                actions.remove(i);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Update an action in the sequence
     *
     * @param action The updated action
     * @return True if the action was updated, false if not found
     */
    public boolean updateAction(Action action) {
        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i).getActionId().equals(action.getActionId())) {
                actions.set(i, action);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Create a simple notification sequence
     *
     * @param name The sequence name
     * @param title The notification title
     * @param message The notification message
     * @return The created sequence
     */
    public static ActionSequence createNotificationSequence(String name, String title, String message) {
        ActionSequence sequence = new ActionSequence(name);
        sequence.addAction(Action.createNotification(title, message));
        return sequence;
    }
    
    /**
     * Create an app automation sequence
     *
     * @param name The sequence name
     * @param appPackage The app package name
     * @param actions List of commands to execute
     * @return The created sequence
     */
    public static ActionSequence createAppSequence(String name, String appPackage, List<String> actions) {
        ActionSequence sequence = new ActionSequence(name);
        // Add app launch action
        sequence.addAction(Action.createAppControl(appPackage, "launch"));
        
        // Add each command
        for (String command : actions) {
            sequence.addAction(Action.createAppControl(appPackage, command));
        }
        
        return sequence;
    }
    
    // Getters and setters
    
    public String getSequenceId() {
        return sequenceId;
    }
    
    public void setSequenceId(String sequenceId) {
        this.sequenceId = sequenceId;
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
    
    public List<Action> getActions() {
        return actions;
    }
    
    public void setActions(List<Action> actions) {
        this.actions = actions;
    }
    
    public boolean isParallel() {
        return parallel;
    }
    
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }
    
    public Integer getMaxDurationSeconds() {
        return maxDurationSeconds;
    }
    
    public void setMaxDurationSeconds(Integer maxDurationSeconds) {
        this.maxDurationSeconds = maxDurationSeconds;
    }
    
    public boolean isRequiresConfirmation() {
        return requiresConfirmation;
    }
    
    public void setRequiresConfirmation(boolean requiresConfirmation) {
        this.requiresConfirmation = requiresConfirmation;
    }
    
    public boolean isStopOnError() {
        return stopOnError;
    }
    
    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
}
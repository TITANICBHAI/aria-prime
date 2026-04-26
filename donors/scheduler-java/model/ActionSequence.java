package com.aiassistant.scheduler.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sequence of actions to be executed by the scheduler
 */
public class ActionSequence implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Sequence properties
    private String sequenceId;
    private String name;
    private String description;
    private List<Action> actions;
    private boolean stopOnError;          // Stop sequence if an action fails
    private boolean runParallel;          // Run actions in parallel
    private boolean runOnUiThread;        // Run actions on UI thread
    private long maxDurationMs;           // Maximum duration in milliseconds
    private boolean requiresConfirmation; // Whether this sequence requires user confirmation
    private String version;               // Version of the sequence
    private String icon;                  // Icon for the sequence
    private String category;              // Category for the sequence
    
    /**
     * Create a new action sequence
     */
    public ActionSequence(String name) {
        this.sequenceId = "seq_" + System.currentTimeMillis();
        this.name = name;
        this.actions = new ArrayList<>();
        this.stopOnError = true;
        this.runParallel = false;
        this.runOnUiThread = false;
        this.maxDurationMs = 0; // No limit
        this.requiresConfirmation = false;
        this.version = "1.0";
        this.icon = "action_sequence";
        this.category = "custom";
    }
    
    /**
     * Empty constructor for serialization
     */
    public ActionSequence() {
        this.sequenceId = "seq_" + System.currentTimeMillis();
        this.actions = new ArrayList<>();
        this.stopOnError = true;
        this.runParallel = false;
        this.runOnUiThread = false;
        this.maxDurationMs = 0; // No limit
        this.requiresConfirmation = false;
        this.version = "1.0";
        this.icon = "action_sequence";
        this.category = "custom";
    }
    
    /**
     * Add an action to the sequence
     */
    public void addAction(Action action) {
        if (actions == null) {
            actions = new ArrayList<>();
        }
        actions.add(action);
    }
    
    /**
     * Remove an action from the sequence
     */
    public boolean removeAction(String actionId) {
        if (actions == null) {
            return false;
        }
        
        return actions.removeIf(action -> action.getActionId().equals(actionId));
    }
    
    /**
     * Get action by ID
     */
    public Action getAction(String actionId) {
        if (actions == null) {
            return null;
        }
        
        for (Action action : actions) {
            if (action.getActionId().equals(actionId)) {
                return action;
            }
        }
        
        return null;
    }
    
    /**
     * Get action by index
     */
    public Action getAction(int index) {
        if (actions == null || index < 0 || index >= actions.size()) {
            return null;
        }
        
        return actions.get(index);
    }
    
    /**
     * Move action up in the sequence
     */
    public boolean moveActionUp(String actionId) {
        if (actions == null) {
            return false;
        }
        
        for (int i = 1; i < actions.size(); i++) {
            if (actions.get(i).getActionId().equals(actionId)) {
                Action action = actions.get(i);
                actions.set(i, actions.get(i - 1));
                actions.set(i - 1, action);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Move action down in the sequence
     */
    public boolean moveActionDown(String actionId) {
        if (actions == null) {
            return false;
        }
        
        for (int i = 0; i < actions.size() - 1; i++) {
            if (actions.get(i).getActionId().equals(actionId)) {
                Action action = actions.get(i);
                actions.set(i, actions.get(i + 1));
                actions.set(i + 1, action);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Create a simple sequence with a single action
     */
    public static ActionSequence single(String name, Action action) {
        ActionSequence sequence = new ActionSequence(name);
        sequence.addAction(action);
        return sequence;
    }
    
    /**
     * Create a notification sequence
     */
    public static ActionSequence notification(String title, String message) {
        ActionSequence sequence = new ActionSequence("Notification: " + title);
        sequence.addAction(Action.notification(title, message));
        return sequence;
    }
    
    /**
     * Create an email sequence
     */
    public static ActionSequence email(String to, String subject, String body) {
        ActionSequence sequence = new ActionSequence("Email: " + subject);
        sequence.addAction(Action.email(to, subject, body));
        return sequence;
    }
    
    /**
     * Create an API call sequence
     */
    public static ActionSequence apiCall(String url, String method, Map<String, Object> data) {
        ActionSequence sequence = new ActionSequence("API call: " + method + " " + url);
        sequence.addAction(Action.apiCall(url, method, data));
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
    
    public boolean isStopOnError() {
        return stopOnError;
    }
    
    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }
    
    public boolean isRunParallel() {
        return runParallel;
    }
    
    public void setRunParallel(boolean runParallel) {
        this.runParallel = runParallel;
    }
    
    public boolean isRunOnUiThread() {
        return runOnUiThread;
    }
    
    public void setRunOnUiThread(boolean runOnUiThread) {
        this.runOnUiThread = runOnUiThread;
    }
    
    public long getMaxDurationMs() {
        return maxDurationMs;
    }
    
    public void setMaxDurationMs(long maxDurationMs) {
        this.maxDurationMs = maxDurationMs;
    }
    
    public boolean isRequiresConfirmation() {
        return requiresConfirmation;
    }
    
    public void setRequiresConfirmation(boolean requiresConfirmation) {
        this.requiresConfirmation = requiresConfirmation;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
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
package com.aiassistant.ml;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an action suggestion from the AI system
 */
public class ActionSuggestion {
    
    public enum SuggestionType {
        APP_LAUNCH,
        APP_ACTION,
        SYSTEM_ACTION,
        NOTIFICATION,
        TASK_SCHEDULE,
        API_CALL,
        EMAIL,
        CUSTOM
    }
    
    public enum ConfidenceLevel {
        LOW,
        MEDIUM,
        HIGH,
        VERY_HIGH
    }
    
    private String id;
    private String title;
    private String description;
    private SuggestionType type;
    private float confidenceScore;
    private ConfidenceLevel confidenceLevel;
    private Map<String, Object> parameters;
    private long timestamp;
    private String source;
    private boolean executed;
    private boolean dismissed;
    private Map<String, Object> metadata;
    
    /**
     * Default constructor
     */
    public ActionSuggestion() {
        this.id = generateId();
        this.parameters = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
        this.executed = false;
        this.dismissed = false;
        this.metadata = new HashMap<>();
    }
    
    /**
     * Constructor with basic parameters
     * 
     * @param title Suggestion title
     * @param description Suggestion description
     * @param type Suggestion type
     * @param confidenceScore Confidence score (0.0 - 1.0)
     */
    public ActionSuggestion(String title, String description, SuggestionType type, float confidenceScore) {
        this();
        this.title = title;
        this.description = description;
        this.type = type;
        this.confidenceScore = confidenceScore;
        this.confidenceLevel = calculateConfidenceLevel(confidenceScore);
    }
    
    /**
     * Generate a unique ID for the suggestion
     */
    private String generateId() {
        return "suggestion_" + System.currentTimeMillis() + "_" + Math.round(Math.random() * 10000);
    }
    
    /**
     * Calculate confidence level from score
     */
    private ConfidenceLevel calculateConfidenceLevel(float score) {
        if (score >= 0.9f) {
            return ConfidenceLevel.VERY_HIGH;
        } else if (score >= 0.7f) {
            return ConfidenceLevel.HIGH;
        } else if (score >= 0.5f) {
            return ConfidenceLevel.MEDIUM;
        } else {
            return ConfidenceLevel.LOW;
        }
    }
    
    /**
     * Add a parameter to the suggestion
     * 
     * @param key Parameter key
     * @param value Parameter value
     * @return This suggestion (for chaining)
     */
    public ActionSuggestion addParameter(String key, Object value) {
        if (key != null && !key.isEmpty()) {
            parameters.put(key, value);
        }
        return this;
    }
    
    /**
     * Add multiple parameters to the suggestion
     * 
     * @param params Parameters to add
     * @return This suggestion (for chaining)
     */
    public ActionSuggestion addParameters(Map<String, Object> params) {
        if (params != null) {
            parameters.putAll(params);
        }
        return this;
    }
    
    /**
     * Add metadata to the suggestion
     * 
     * @param key Metadata key
     * @param value Metadata value
     * @return This suggestion (for chaining)
     */
    public ActionSuggestion addMetadata(String key, Object value) {
        if (key != null && !key.isEmpty()) {
            metadata.put(key, value);
        }
        return this;
    }
    
    /**
     * Check if the suggestion has a specific parameter
     * 
     * @param key Parameter key
     * @return true if parameter exists
     */
    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }
    
    /**
     * Get a parameter value
     * 
     * @param key Parameter key
     * @return Parameter value or null if not found
     */
    public Object getParameter(String key) {
        return parameters.get(key);
    }
    
    /**
     * Get a parameter value with type casting
     * 
     * @param key Parameter key
     * @param defaultValue Default value if parameter not found or wrong type
     * @return Parameter value or default if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, T defaultValue) {
        Object value = parameters.get(key);
        
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
    
    /**
     * Mark the suggestion as executed
     */
    public void markExecuted() {
        this.executed = true;
    }
    
    /**
     * Mark the suggestion as dismissed
     */
    public void markDismissed() {
        this.dismissed = true;
    }
    
    /**
     * Get suggestion ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set suggestion ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get suggestion title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Set suggestion title
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * Get suggestion description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set suggestion description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Get suggestion type
     */
    public SuggestionType getType() {
        return type;
    }
    
    /**
     * Set suggestion type
     */
    public void setType(SuggestionType type) {
        this.type = type;
    }
    
    /**
     * Get confidence score
     */
    public float getConfidenceScore() {
        return confidenceScore;
    }
    
    /**
     * Set confidence score
     */
    public void setConfidenceScore(float confidenceScore) {
        this.confidenceScore = confidenceScore;
        this.confidenceLevel = calculateConfidenceLevel(confidenceScore);
    }
    
    /**
     * Get confidence level
     */
    public ConfidenceLevel getConfidenceLevel() {
        return confidenceLevel;
    }
    
    /**
     * Get parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    /**
     * Set parameters
     */
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }
    
    /**
     * Get timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Set timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Get source
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Set source
     */
    public void setSource(String source) {
        this.source = source;
    }
    
    /**
     * Check if executed
     */
    public boolean isExecuted() {
        return executed;
    }
    
    /**
     * Set executed flag
     */
    public void setExecuted(boolean executed) {
        this.executed = executed;
    }
    
    /**
     * Check if dismissed
     */
    public boolean isDismissed() {
        return dismissed;
    }
    
    /**
     * Set dismissed flag
     */
    public void setDismissed(boolean dismissed) {
        this.dismissed = dismissed;
    }
    
    /**
     * Get metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Set metadata
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
    
    @Override
    public String toString() {
        return "ActionSuggestion{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", confidence=" + confidenceScore +
                ", executed=" + executed +
                ", dismissed=" + dismissed +
                '}';
    }
}
package com.aiassistant.ml;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Class representing a game action for the RL system
 */
public class GameAction {
    private final String actionId;
    private final String actionName;
    private final GameType gameType;
    private final Map<String, Object> parameters;
    private final float priority;
    private final boolean continuous;
    private final long cooldownMs;
    
    /**
     * Create a new GameAction
     * 
     * @param actionId Unique identifier for this action
     * @param actionName Name of the action
     * @param gameType Game type this action is for
     * @param parameters Parameters for the action
     * @param priority Priority of the action (0.0-1.0, higher is more important)
     * @param continuous Whether this is a continuous action
     * @param cooldownMs Cooldown time in milliseconds
     */
    public GameAction(
            @NonNull String actionId,
            @NonNull String actionName,
            @NonNull GameType gameType,
            @Nullable Map<String, Object> parameters,
            float priority,
            boolean continuous,
            long cooldownMs) {
        this.actionId = actionId;
        this.actionName = actionName;
        this.gameType = gameType;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
        this.priority = Math.max(0.0f, Math.min(1.0f, priority));
        this.continuous = continuous;
        this.cooldownMs = Math.max(0, cooldownMs);
    }
    
    /**
     * Create a simple GameAction with default values
     * 
     * @param actionId Unique identifier for this action
     * @param actionName Name of the action
     * @param gameType Game type this action is for
     */
    public GameAction(
            @NonNull String actionId,
            @NonNull String actionName,
            @NonNull GameType gameType) {
        this(actionId, actionName, gameType, null, 0.5f, false, 0);
    }

    @NonNull
    public String getActionId() {
        return actionId;
    }

    @NonNull
    public String getActionName() {
        return actionName;
    }

    @NonNull
    public GameType getGameType() {
        return gameType;
    }

    @NonNull
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }

    public float getPriority() {
        return priority;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public long getCooldownMs() {
        return cooldownMs;
    }
    
    /**
     * Get a specific parameter value
     * 
     * @param key Parameter key
     * @return Parameter value or null if not found
     */
    @Nullable
    public Object getParameter(String key) {
        return parameters.get(key);
    }
    
    /**
     * Get a specific parameter value as an integer
     * 
     * @param key Parameter key
     * @param defaultValue Default value if parameter is missing or not an integer
     * @return Parameter value as integer
     */
    public int getParameterInt(String key, int defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * Get a specific parameter value as a float
     * 
     * @param key Parameter key
     * @param defaultValue Default value if parameter is missing or not a float
     * @return Parameter value as float
     */
    public float getParameterFloat(String key, float defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * Get a specific parameter value as a boolean
     * 
     * @param key Parameter key
     * @param defaultValue Default value if parameter is missing or not a boolean
     * @return Parameter value as boolean
     */
    public boolean getParameterBoolean(String key, boolean defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
    
    /**
     * Create a new GameAction with updated parameters
     * 
     * @param newParameters New parameters
     * @return New GameAction instance
     */
    @NonNull
    public GameAction withParameters(@NonNull Map<String, Object> newParameters) {
        return new GameAction(
                actionId, actionName, gameType, newParameters,
                priority, continuous, cooldownMs);
    }
    
    /**
     * Create a new GameAction with an additional parameter
     * 
     * @param key Parameter key
     * @param value Parameter value
     * @return New GameAction instance
     */
    @NonNull
    public GameAction withParameter(String key, Object value) {
        Map<String, Object> newParameters = new HashMap<>(parameters);
        newParameters.put(key, value);
        return new GameAction(
                actionId, actionName, gameType, newParameters,
                priority, continuous, cooldownMs);
    }
    
    /**
     * Create a new GameAction with updated priority
     * 
     * @param newPriority New priority value
     * @return New GameAction instance
     */
    @NonNull
    public GameAction withPriority(float newPriority) {
        return new GameAction(
                actionId, actionName, gameType, parameters,
                newPriority, continuous, cooldownMs);
    }
    
    /**
     * Create a new GameAction with updated continuous flag
     * 
     * @param newContinuous New continuous value
     * @return New GameAction instance
     */
    @NonNull
    public GameAction withContinuous(boolean newContinuous) {
        return new GameAction(
                actionId, actionName, gameType, parameters,
                priority, newContinuous, cooldownMs);
    }
    
    /**
     * Create a new GameAction with updated cooldown
     * 
     * @param newCooldownMs New cooldown value in milliseconds
     * @return New GameAction instance
     */
    @NonNull
    public GameAction withCooldown(long newCooldownMs) {
        return new GameAction(
                actionId, actionName, gameType, parameters,
                priority, continuous, newCooldownMs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameAction that = (GameAction) o;
        return Float.compare(that.priority, priority) == 0 &&
                continuous == that.continuous &&
                cooldownMs == that.cooldownMs &&
                actionId.equals(that.actionId) &&
                actionName.equals(that.actionName) &&
                gameType == that.gameType &&
                parameters.equals(that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionId, actionName, gameType, parameters,
                priority, continuous, cooldownMs);
    }

    @Override
    public String toString() {
        return "GameAction{" +
                "actionId='" + actionId + '\'' +
                ", actionName='" + actionName + '\'' +
                ", gameType=" + gameType +
                ", priority=" + priority +
                ", continuous=" + continuous +
                ", cooldownMs=" + cooldownMs +
                '}';
    }
    
    /**
     * Builder class for GameAction
     */
    public static class Builder {
        private String actionId;
        private String actionName;
        private GameType gameType = GameType.UNKNOWN;
        private Map<String, Object> parameters = new HashMap<>();
        private float priority = 0.5f;
        private boolean continuous = false;
        private long cooldownMs = 0;
        
        /**
         * Create a builder with required elements
         * 
         * @param actionId Unique identifier for this action
         * @param actionName Name of the action
         */
        public Builder(@NonNull String actionId, @NonNull String actionName) {
            this.actionId = actionId;
            this.actionName = actionName;
        }
        
        /**
         * Set the game type
         * 
         * @param type Game type
         * @return This Builder instance
         */
        @NonNull
        public Builder setGameType(@NonNull GameType type) {
            this.gameType = type;
            return this;
        }
        
        /**
         * Add a parameter
         * 
         * @param key Parameter key
         * @param value Parameter value
         * @return This Builder instance
         */
        @NonNull
        public Builder addParameter(@NonNull String key, @Nullable Object value) {
            this.parameters.put(key, value);
            return this;
        }
        
        /**
         * Add all parameters from a map
         * 
         * @param params Map of parameters to add
         * @return This Builder instance
         */
        @NonNull
        public Builder addParameters(@Nullable Map<String, Object> params) {
            if (params != null) {
                this.parameters.putAll(params);
            }
            return this;
        }
        
        /**
         * Set the priority
         * 
         * @param priority Priority value (0.0-1.0)
         * @return This Builder instance
         */
        @NonNull
        public Builder setPriority(float priority) {
            this.priority = Math.max(0.0f, Math.min(1.0f, priority));
            return this;
        }
        
        /**
         * Set whether this is a continuous action
         * 
         * @param continuous Whether this is a continuous action
         * @return This Builder instance
         */
        @NonNull
        public Builder setContinuous(boolean continuous) {
            this.continuous = continuous;
            return this;
        }
        
        /**
         * Set the cooldown time
         * 
         * @param cooldownMs Cooldown time in milliseconds
         * @return This Builder instance
         */
        @NonNull
        public Builder setCooldown(long cooldownMs) {
            this.cooldownMs = Math.max(0, cooldownMs);
            return this;
        }
        
        /**
         * Build the GameAction
         * 
         * @return GameAction instance
         */
        @NonNull
        public GameAction build() {
            return new GameAction(
                    actionId, actionName, gameType, parameters,
                    priority, continuous, cooldownMs);
        }
    }
}
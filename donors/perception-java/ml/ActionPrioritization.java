package com.aiassistant.ml;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sophisticated Action Prioritization System
 *
 * This system analyzes context, game state, and historical data to dynamically
 * prioritize actions for optimal decision-making in real time.
 */
public class ActionPrioritization {
    private static final String TAG = "ActionPrioritization";
    
    // Singleton instance
    private static ActionPrioritization instance;
    
    // Context
    private final Context context;
    
    // Priority configuration
    private final Map<String, Float> actionTypePriorities = new HashMap<>();
    private final Map<String, Float> contextualPriorities = new HashMap<>();
    private final Map<String, Float> gameSpecificPriorities = new HashMap<>();
    private final Map<String, Float> emergencyPriorities = new HashMap<>();
    
    // Success tracking
    private final Map<String, ActionSuccessRecord> successRecords = new ConcurrentHashMap<>();
    
    // User feedback tracking
    private final Map<String, UserFeedbackRecord> feedbackRecords = new ConcurrentHashMap<>();
    
    // Action counter
    private final AtomicInteger actionCounter = new AtomicInteger(0);
    
    // Current game context
    private String currentGameType = "unknown";
    
    // Critical state indicators
    private volatile boolean isLowHealth = false;
    private volatile boolean isLowResource = false;
    private volatile boolean isUnderAttack = false;
    private volatile boolean isInBossFight = false;
    
    /**
     * Action priority thresholds
     */
    public static class PriorityThresholds {
        public static final float CRITICAL = 0.9f;
        public static final float HIGH = 0.7f;
        public static final float MEDIUM = 0.5f;
        public static final float LOW = 0.3f;
        public static final float BACKGROUND = 0.1f;
    }
    
    /**
     * Action success record
     */
    public static class ActionSuccessRecord {
        private final String actionKey;
        private int attempts;
        private int successes;
        private float avgReward;
        private long lastAttemptTime;
        private long totalExecutionTimeMs;
        
        public ActionSuccessRecord(String actionKey) {
            this.actionKey = actionKey;
            this.attempts = 0;
            this.successes = 0;
            this.avgReward = 0.0f;
            this.lastAttemptTime = 0;
            this.totalExecutionTimeMs = 0;
        }
        
        public void recordAttempt(boolean success, float reward, long executionTimeMs) {
            attempts++;
            if (success) {
                successes++;
            }
            
            // Update average reward with exponential moving average
            avgReward = (avgReward * 0.9f) + (reward * 0.1f);
            lastAttemptTime = System.currentTimeMillis();
            totalExecutionTimeMs += executionTimeMs;
        }
        
        public float getSuccessRate() {
            return attempts > 0 ? (float) successes / attempts : 0.0f;
        }
        
        public float getAvgReward() {
            return avgReward;
        }
        
        public long getAvgExecutionTimeMs() {
            return attempts > 0 ? totalExecutionTimeMs / attempts : 0;
        }
        
        public long getLastAttemptTime() {
            return lastAttemptTime;
        }
        
        public int getAttempts() {
            return attempts;
        }
        
        public int getSuccesses() {
            return successes;
        }
        
        public String getActionKey() {
            return actionKey;
        }
    }
    
    /**
     * User feedback record
     */
    public static class UserFeedbackRecord {
        private final String actionKey;
        private int positiveFeedbackCount;
        private int negativeFeedbackCount;
        private float userSatisfactionScore;
        private long lastFeedbackTime;
        
        public UserFeedbackRecord(String actionKey) {
            this.actionKey = actionKey;
            this.positiveFeedbackCount = 0;
            this.negativeFeedbackCount = 0;
            this.userSatisfactionScore = 0.5f; // Neutral to start
            this.lastFeedbackTime = 0;
        }
        
        public void recordFeedback(boolean positive, float satisfactionDelta) {
            if (positive) {
                positiveFeedbackCount++;
            } else {
                negativeFeedbackCount++;
            }
            
            // Update satisfaction score with bounds checking
            userSatisfactionScore += satisfactionDelta;
            userSatisfactionScore = Math.max(0.0f, Math.min(1.0f, userSatisfactionScore));
            
            lastFeedbackTime = System.currentTimeMillis();
        }
        
        public float getFeedbackRatio() {
            int total = positiveFeedbackCount + negativeFeedbackCount;
            return total > 0 ? (float) positiveFeedbackCount / total : 0.5f;
        }
        
        public float getSatisfactionScore() {
            return userSatisfactionScore;
        }
        
        public long getLastFeedbackTime() {
            return lastFeedbackTime;
        }
        
        public int getPositiveFeedbackCount() {
            return positiveFeedbackCount;
        }
        
        public int getNegativeFeedbackCount() {
            return negativeFeedbackCount;
        }
        
        public String getActionKey() {
            return actionKey;
        }
    }
    
    /**
     * Prioritized action
     */
    public static class PrioritizedAction {
        private final String actionId;
        private final PredictiveActionSystem.GameAction gameAction;
        private final float basePriority;
        private float contextualPriority;
        private float finalPriority;
        private final Map<String, Float> priorityFactors;
        private final long creationTime;
        
        public PrioritizedAction(String actionId, PredictiveActionSystem.GameAction gameAction, 
                                float basePriority) {
            this.actionId = actionId;
            this.gameAction = gameAction;
            this.basePriority = basePriority;
            this.contextualPriority = basePriority;
            this.finalPriority = basePriority;
            this.priorityFactors = new HashMap<>();
            this.creationTime = System.currentTimeMillis();
        }
        
        public void addPriorityFactor(String factor, float value) {
            priorityFactors.put(factor, value);
            recalculatePriority();
        }
        
        private void recalculatePriority() {
            float multiplier = 1.0f;
            for (float factor : priorityFactors.values()) {
                multiplier *= factor;
            }
            finalPriority = basePriority * multiplier;
        }
        
        public String getActionId() {
            return actionId;
        }
        
        public PredictiveActionSystem.GameAction getGameAction() {
            return gameAction;
        }
        
        public float getBasePriority() {
            return basePriority;
        }
        
        public float getContextualPriority() {
            return contextualPriority;
        }
        
        public void setContextualPriority(float priority) {
            this.contextualPriority = priority;
            recalculatePriority();
        }
        
        public float getFinalPriority() {
            return finalPriority;
        }
        
        public Map<String, Float> getPriorityFactors() {
            return Collections.unmodifiableMap(priorityFactors);
        }
        
        public long getCreationTime() {
            return creationTime;
        }
        
        public long getAge() {
            return System.currentTimeMillis() - creationTime;
        }
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized ActionPrioritization getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new ActionPrioritization(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private ActionPrioritization(Context context) {
        this.context = context;
        
        // Initialize default priorities
        initializeDefaultPriorities();
        
        Log.i(TAG, "Action prioritization system created");
    }
    
    /**
     * Initialize default priorities
     */
    private void initializeDefaultPriorities() {
        // Action type priorities
        actionTypePriorities.put("attack", 0.8f);
        actionTypePriorities.put("defend", 0.85f);
        actionTypePriorities.put("heal", 0.9f);
        actionTypePriorities.put("collect", 0.7f);
        actionTypePriorities.put("move", 0.6f);
        actionTypePriorities.put("interact", 0.65f);
        actionTypePriorities.put("use_item", 0.75f);
        
        // Contextual priorities
        contextualPriorities.put("low_health", 2.0f);
        contextualPriorities.put("low_resource", 1.5f);
        contextualPriorities.put("under_attack", 1.8f);
        contextualPriorities.put("boss_fight", 1.7f);
        contextualPriorities.put("exploration", 0.6f);
        contextualPriorities.put("grinding", 0.4f);
        
        // Game specific priorities
        gameSpecificPriorities.put("action_game_attack", 1.2f);
        gameSpecificPriorities.put("rpg_heal", 1.5f);
        gameSpecificPriorities.put("racing_boost", 1.3f);
        gameSpecificPriorities.put("strategy_build", 1.1f);
        
        // Emergency priorities
        emergencyPriorities.put("critical_health", 3.0f);
        emergencyPriorities.put("instant_death_avoid", 5.0f);
        emergencyPriorities.put("game_over_prevention", 4.0f);
    }
    
    /**
     * Set current game type
     */
    public void setGameType(String gameType) {
        this.currentGameType = gameType;
        Log.i(TAG, "Game type set to: " + gameType);
    }
    
    /**
     * Set critical state indicators
     */
    public void setCriticalStateIndicators(boolean lowHealth, boolean lowResource, 
                                          boolean underAttack, boolean inBossFight) {
        this.isLowHealth = lowHealth;
        this.isLowResource = lowResource;
        this.isUnderAttack = underAttack;
        this.isInBossFight = inBossFight;
    }
    
    /**
     * Prioritize a list of actions
     */
    public List<PrioritizedAction> prioritizeActions(List<PredictiveActionSystem.GameAction> actions, 
                                                    Map<String, Object> gameState) {
        List<PrioritizedAction> prioritizedActions = new ArrayList<>();
        
        // Create prioritized actions
        for (PredictiveActionSystem.GameAction action : actions) {
            String actionId = "action_" + actionCounter.incrementAndGet();
            float basePriority = action.getPriority();
            
            PrioritizedAction prioritizedAction = new PrioritizedAction(actionId, action, basePriority);
            
            // Apply action type priority
            String actionType = getActionTypeFromAction(action);
            if (actionTypePriorities.containsKey(actionType)) {
                prioritizedAction.addPriorityFactor("action_type", 
                        actionTypePriorities.get(actionType));
            }
            
            // Apply contextual priorities
            applyContextualPriorities(prioritizedAction, action, gameState);
            
            // Apply game-specific priorities
            applyGameSpecificPriorities(prioritizedAction, action);
            
            // Apply success record priorities
            applySuccessRecordPriorities(prioritizedAction, action);
            
            // Apply user feedback priorities
            applyUserFeedbackPriorities(prioritizedAction, action);
            
            // Add to list
            prioritizedActions.add(prioritizedAction);
        }
        
        // Sort by final priority (descending)
        Collections.sort(prioritizedActions, new Comparator<PrioritizedAction>() {
            @Override
            public int compare(PrioritizedAction a1, PrioritizedAction a2) {
                return Float.compare(a2.getFinalPriority(), a1.getFinalPriority());
            }
        });
        
        return prioritizedActions;
    }
    
    /**
     * Get action type from action
     */
    private String getActionTypeFromAction(PredictiveActionSystem.GameAction action) {
        // Extract action type from parameters or type
        String type = action.getType().toString().toLowerCase();
        Map<String, Object> params = action.getParameters();
        
        if (params.containsKey("action_type")) {
            return params.get("action_type").toString().toLowerCase();
        }
        
        // Try to infer from parameters
        if (params.containsKey("target_type")) {
            String targetType = params.get("target_type").toString().toLowerCase();
            if (targetType.contains("enemy")) {
                return "attack";
            } else if (targetType.contains("item") || targetType.contains("resource")) {
                return "collect";
            }
        }
        
        // Map basic types
        switch (type) {
            case "tap":
                if (params.containsKey("target") && params.get("target").toString().contains("enemy")) {
                    return "attack";
                }
                return "interact";
                
            case "swipe":
                return "move";
                
            case "long_press":
                return "use_item";
                
            default:
                return type;
        }
    }
    
    /**
     * Apply contextual priorities
     */
    private void applyContextualPriorities(PrioritizedAction prioritizedAction, 
                                         PredictiveActionSystem.GameAction action, 
                                         Map<String, Object> gameState) {
        String actionType = getActionTypeFromAction(action);
        
        // Apply critical state indicators
        if (isLowHealth) {
            if (actionType.equals("heal")) {
                prioritizedAction.addPriorityFactor("low_health", 
                        contextualPriorities.get("low_health"));
            }
        }
        
        if (isLowResource) {
            if (actionType.equals("collect")) {
                prioritizedAction.addPriorityFactor("low_resource", 
                        contextualPriorities.get("low_resource"));
            }
        }
        
        if (isUnderAttack) {
            if (actionType.equals("defend") || actionType.equals("attack")) {
                prioritizedAction.addPriorityFactor("under_attack", 
                        contextualPriorities.get("under_attack"));
            }
        }
        
        if (isInBossFight) {
            prioritizedAction.addPriorityFactor("boss_fight", 
                    contextualPriorities.get("boss_fight"));
        }
        
        // Check for critical health
        if (gameState.containsKey("health")) {
            float health = 0;
            Object healthObj = gameState.get("health");
            if (healthObj instanceof Number) {
                health = ((Number) healthObj).floatValue();
            } else if (healthObj instanceof String) {
                try {
                    health = Float.parseFloat((String) healthObj);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
            
            if (health < 0.2f) { // Critical health threshold
                if (actionType.equals("heal")) {
                    prioritizedAction.addPriorityFactor("critical_health", 
                            emergencyPriorities.get("critical_health"));
                }
            }
        }
    }
    
    /**
     * Apply game-specific priorities
     */
    private void applyGameSpecificPriorities(PrioritizedAction prioritizedAction, 
                                           PredictiveActionSystem.GameAction action) {
        String actionType = getActionTypeFromAction(action);
        String gameActionKey = currentGameType + "_" + actionType;
        
        if (gameSpecificPriorities.containsKey(gameActionKey)) {
            prioritizedAction.addPriorityFactor("game_specific", 
                    gameSpecificPriorities.get(gameActionKey));
        }
    }
    
    /**
     * Apply success record priorities
     */
    private void applySuccessRecordPriorities(PrioritizedAction prioritizedAction, 
                                            PredictiveActionSystem.GameAction action) {
        String actionType = getActionTypeFromAction(action);
        String actionKey = getActionKey(action);
        
        ActionSuccessRecord record = successRecords.get(actionKey);
        if (record != null && record.getAttempts() > 0) {
            // Apply success rate
            float successRate = record.getSuccessRate();
            float successFactor = 0.7f + (successRate * 0.6f); // Range: 0.7 to 1.3
            prioritizedAction.addPriorityFactor("success_rate", successFactor);
            
            // Apply reward
            float reward = record.getAvgReward();
            float rewardFactor = 0.8f + (reward * 0.4f); // Range: 0.8 to 1.2
            prioritizedAction.addPriorityFactor("avg_reward", rewardFactor);
        }
    }
    
    /**
     * Apply user feedback priorities
     */
    private void applyUserFeedbackPriorities(PrioritizedAction prioritizedAction, 
                                           PredictiveActionSystem.GameAction action) {
        String actionKey = getActionKey(action);
        UserFeedbackRecord record = feedbackRecords.get(actionKey);
        
        if (record != null) {
            // Apply satisfaction score
            float satisfaction = record.getSatisfactionScore();
            float satisfactionFactor = 0.5f + satisfaction; // Range: 0.5 to 1.5
            prioritizedAction.addPriorityFactor("user_feedback", satisfactionFactor);
        }
    }
    
    /**
     * Record action result
     */
    public void recordActionResult(PredictiveActionSystem.GameAction action, 
                                  boolean success, float reward, long executionTimeMs) {
        String actionKey = getActionKey(action);
        
        // Update or create success record
        ActionSuccessRecord record = successRecords.get(actionKey);
        if (record == null) {
            record = new ActionSuccessRecord(actionKey);
            successRecords.put(actionKey, record);
        }
        
        record.recordAttempt(success, reward, executionTimeMs);
    }
    
    /**
     * Record user feedback
     */
    public void recordUserFeedback(PredictiveActionSystem.GameAction action, 
                                  boolean positive, float satisfactionDelta) {
        String actionKey = getActionKey(action);
        
        // Update or create feedback record
        UserFeedbackRecord record = feedbackRecords.get(actionKey);
        if (record == null) {
            record = new UserFeedbackRecord(actionKey);
            feedbackRecords.put(actionKey, record);
        }
        
        record.recordFeedback(positive, satisfactionDelta);
    }
    
    /**
     * Get key for an action (for recording/lookup)
     */
    private String getActionKey(PredictiveActionSystem.GameAction action) {
        String actionType = getActionTypeFromAction(action);
        
        // For more specific keys, include target info if available
        Map<String, Object> params = action.getParameters();
        if (params.containsKey("target_id")) {
            return actionType + "_" + params.get("target_id");
        } else if (params.containsKey("target_type")) {
            return actionType + "_" + params.get("target_type");
        }
        
        return actionType;
    }
    
    /**
     * Get action priority adjustment based on user feedback
     */
    public float getActionPriorityAdjustment(String actionType) {
        // Calculate average satisfaction for this action type
        float totalSatisfaction = 0;
        int count = 0;
        
        for (UserFeedbackRecord record : feedbackRecords.values()) {
            if (record.getActionKey().startsWith(actionType)) {
                totalSatisfaction += record.getSatisfactionScore();
                count++;
            }
        }
        
        if (count == 0) {
            return 1.0f; // Neutral
        }
        
        return 0.7f + ((totalSatisfaction / count) * 0.6f); // Range: 0.7 to 1.3
    }
    
    /**
     * Get strategic action recommendations
     */
    public List<PrioritizedAction> getStrategicRecommendations(Map<String, Object> gameState, 
                                                              List<RuleExtractionSystem.GameRule> rules) {
        List<PrioritizedAction> recommendations = new ArrayList<>();
        
        // Create recommendations from rules
        for (RuleExtractionSystem.GameRule rule : rules) {
            // Only consider high confidence rules
            if (rule.getConfidence() < 0.7f) {
                continue;
            }
            
            // Strategy or goal rules
            if (rule.getType() == RuleExtractionSystem.GameRule.RuleType.STRATEGY ||
                rule.getType() == RuleExtractionSystem.GameRule.RuleType.GOAL) {
                
                // Extract action from rule
                PredictiveActionSystem.GameAction action = createActionFromRule(rule, gameState);
                if (action != null) {
                    String actionId = "strategy_" + actionCounter.incrementAndGet();
                    float basePriority = 0.7f + (rule.getConfidence() * 0.3f); // 0.7 to 1.0
                    
                    PrioritizedAction recommendation = new PrioritizedAction(actionId, action, basePriority);
                    recommendation.addPriorityFactor("rule_confidence", rule.getConfidence());
                    
                    // Higher priority for strategies with more observations
                    float observationFactor = Math.min(1.0f + (rule.getObservationCount() / 100.0f), 1.5f);
                    recommendation.addPriorityFactor("observation_count", observationFactor);
                    
                    recommendations.add(recommendation);
                }
            }
        }
        
        // Sort by final priority
        Collections.sort(recommendations, new Comparator<PrioritizedAction>() {
            @Override
            public int compare(PrioritizedAction a1, PrioritizedAction a2) {
                return Float.compare(a2.getFinalPriority(), a1.getFinalPriority());
            }
        });
        
        return recommendations;
    }
    
    /**
     * Create action from rule
     */
    private PredictiveActionSystem.GameAction createActionFromRule(RuleExtractionSystem.GameRule rule, 
                                                                 Map<String, Object> gameState) {
        try {
            Map<String, Object> params = new HashMap<>();
            
            // Extract parameters from rule
            if (rule instanceof RuleExtractionSystem.PatternRule) {
                RuleExtractionSystem.PatternRule patternRule = (RuleExtractionSystem.PatternRule) rule;
                List<Map<String, Object>> pattern = patternRule.getPattern();
                
                if (!pattern.isEmpty()) {
                    // Use first step as parameters
                    params.putAll(pattern.get(0));
                }
            } else if (rule instanceof RuleExtractionSystem.ContextualRule) {
                RuleExtractionSystem.ContextualRule contextualRule = (RuleExtractionSystem.ContextualRule) rule;
                if (!contextualRule.getActions().isEmpty()) {
                    params.put("action_type", contextualRule.getActions().get(0));
                }
            }
            
            // Determine action type
            PredictiveActionSystem.ActionType actionType = PredictiveActionSystem.ActionType.TAP;
            if (params.containsKey("action_type")) {
                String type = params.get("action_type").toString().toLowerCase();
                if (type.contains("swipe")) {
                    actionType = PredictiveActionSystem.ActionType.SWIPE;
                } else if (type.contains("long") || type.contains("press")) {
                    actionType = PredictiveActionSystem.ActionType.LONG_PRESS;
                }
            }
            
            // Create action
            return new PredictiveActionSystem.GameAction(
                    actionType, params, rule.getConfidence(), rule.getConfidence());
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating action from rule: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get action success statistics
     */
    public Map<String, Object> getActionSuccessStats() {
        Map<String, Object> stats = new HashMap<>();
        Map<String, Float> successRates = new HashMap<>();
        Map<String, Float> avgRewards = new HashMap<>();
        Map<String, Long> avgExecutionTimes = new HashMap<>();
        
        for (ActionSuccessRecord record : successRecords.values()) {
            if (record.getAttempts() > 0) {
                successRates.put(record.getActionKey(), record.getSuccessRate());
                avgRewards.put(record.getActionKey(), record.getAvgReward());
                avgExecutionTimes.put(record.getActionKey(), record.getAvgExecutionTimeMs());
            }
        }
        
        stats.put("successRates", successRates);
        stats.put("avgRewards", avgRewards);
        stats.put("avgExecutionTimes", avgExecutionTimes);
        
        return stats;
    }
    
    /**
     * Get user feedback statistics
     */
    public Map<String, Object> getUserFeedbackStats() {
        Map<String, Object> stats = new HashMap<>();
        Map<String, Float> feedbackRatios = new HashMap<>();
        Map<String, Float> satisfactionScores = new HashMap<>();
        
        for (UserFeedbackRecord record : feedbackRecords.values()) {
            feedbackRatios.put(record.getActionKey(), record.getFeedbackRatio());
            satisfactionScores.put(record.getActionKey(), record.getSatisfactionScore());
        }
        
        stats.put("feedbackRatios", feedbackRatios);
        stats.put("satisfactionScores", satisfactionScores);
        
        return stats;
    }
    
    /**
     * Get all statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("actionCounter", actionCounter.get());
        stats.put("currentGameType", currentGameType);
        stats.put("successRecordCount", successRecords.size());
        stats.put("feedbackRecordCount", feedbackRecords.size());
        stats.put("isLowHealth", isLowHealth);
        stats.put("isLowResource", isLowResource);
        stats.put("isUnderAttack", isUnderAttack);
        stats.put("isInBossFight", isInBossFight);
        
        stats.put("actionSuccessStats", getActionSuccessStats());
        stats.put("userFeedbackStats", getUserFeedbackStats());
        
        return stats;
    }
}
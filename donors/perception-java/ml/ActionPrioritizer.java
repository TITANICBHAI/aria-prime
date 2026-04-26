package com.aiassistant.ml;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sophisticated Action Prioritization System
 * 
 * This system analyzes context, user feedback, and historical performance to
 * prioritize actions for optimal decision-making in various gaming scenarios.
 */
public class ActionPrioritizer {
    private static final String TAG = "ActionPrioritizer";
    
    // Singleton instance
    private static ActionPrioritizer instance;
    
    // Context
    private final Context context;
    
    // Historical action data
    private final Map<String, ActionData> actionHistory = new ConcurrentHashMap<>();
    
    // Context-based priority modifiers
    private final Map<String, Map<String, Float>> contextualPriorities = new HashMap<>();
    
    // User feedback records
    private final Map<String, UserFeedbackRecord> userFeedback = new ConcurrentHashMap<>();
    
    // Recent actions queue
    private final LinkedHashMap<String, ActionData> recentActions = new LinkedHashMap<String, ActionData>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ActionData> eldest) {
            return size() > 100; // Keep last 100 actions
        }
    };
    
    // Stats
    private final AtomicInteger totalActionsEvaluated = new AtomicInteger(0);
    private final AtomicInteger feedbackCount = new AtomicInteger(0);
    
    // Constants
    private static final float DEFAULT_PRIORITY = 0.5f;
    private static final float MIN_PRIORITY = 0.0f;
    private static final float MAX_PRIORITY = 1.0f;
    
    /**
     * Action data structure
     */
    public static class ActionData {
        final String actionId;
        final String actionType;
        final Map<String, Object> parameters;
        float baseScore;
        int useCount;
        int successCount;
        long lastUsedTimestamp;
        long creationTimestamp;
        
        public ActionData(String actionId, String actionType, Map<String, Object> parameters) {
            this.actionId = actionId;
            this.actionType = actionType;
            this.parameters = new HashMap<>(parameters);
            this.baseScore = DEFAULT_PRIORITY;
            this.useCount = 0;
            this.successCount = 0;
            this.lastUsedTimestamp = System.currentTimeMillis();
            this.creationTimestamp = System.currentTimeMillis();
        }
        
        public float getSuccessRate() {
            return useCount > 0 ? (float) successCount / useCount : 0.0f;
        }
        
        public void recordUse(boolean success) {
            useCount++;
            if (success) {
                successCount++;
            }
            lastUsedTimestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * User feedback record
     */
    public static class UserFeedbackRecord {
        final String actionId;
        final String feedbackType;
        final float score;
        final long timestamp;
        final String context;
        
        public UserFeedbackRecord(String actionId, String feedbackType, 
                                 float score, String context) {
            this.actionId = actionId;
            this.feedbackType = feedbackType;
            this.score = score;
            this.timestamp = System.currentTimeMillis();
            this.context = context;
        }
    }
    
    /**
     * Context-action pair
     */
    private static class ContextActionPair {
        final String contextKey;
        final String actionId;
        
        public ContextActionPair(String contextKey, String actionId) {
            this.contextKey = contextKey;
            this.actionId = actionId;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ContextActionPair that = (ContextActionPair) o;
            return contextKey.equals(that.contextKey) && actionId.equals(that.actionId);
        }
        
        @Override
        public int hashCode() {
            int result = contextKey.hashCode();
            result = 31 * result + actionId.hashCode();
            return result;
        }
    }
    
    /**
     * Prioritized action result
     */
    public static class PrioritizedAction {
        private final String actionId;
        private final String actionType;
        private final Map<String, Object> parameters;
        private final float priority;
        private final float confidence;
        private final Map<String, Float> priorityFactors;
        
        public PrioritizedAction(String actionId, String actionType, 
                                Map<String, Object> parameters,
                                float priority, float confidence,
                                Map<String, Float> priorityFactors) {
            this.actionId = actionId;
            this.actionType = actionType;
            this.parameters = parameters;
            this.priority = priority;
            this.confidence = confidence;
            this.priorityFactors = priorityFactors;
        }
        
        public String getActionId() {
            return actionId;
        }
        
        public String getActionType() {
            return actionType;
        }
        
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        public float getPriority() {
            return priority;
        }
        
        public float getConfidence() {
            return confidence;
        }
        
        public Map<String, Float> getPriorityFactors() {
            return priorityFactors;
        }
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized ActionPrioritizer getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new ActionPrioritizer(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private ActionPrioritizer(Context context) {
        this.context = context;
        
        // Initialize default contextual priorities
        initializeDefaultContexts();
        
        Log.i(TAG, "Action prioritizer created");
    }
    
    /**
     * Initialize default contexts
     */
    private void initializeDefaultContexts() {
        // Combat context
        Map<String, Float> combatPriorities = new HashMap<>();
        combatPriorities.put("attack", 0.9f);
        combatPriorities.put("defend", 0.85f);
        combatPriorities.put("dodge", 0.8f);
        combatPriorities.put("heal", 0.7f);
        contextualPriorities.put("combat", combatPriorities);
        
        // Exploration context
        Map<String, Float> explorationPriorities = new HashMap<>();
        explorationPriorities.put("move", 0.8f);
        explorationPriorities.put("examine", 0.7f);
        explorationPriorities.put("collect", 0.6f);
        contextualPriorities.put("exploration", explorationPriorities);
        
        // Menu context
        Map<String, Float> menuPriorities = new HashMap<>();
        menuPriorities.put("select", 0.8f);
        menuPriorities.put("back", 0.6f);
        menuPriorities.put("scroll", 0.5f);
        contextualPriorities.put("menu", menuPriorities);
        
        // Racing context
        Map<String, Float> racingPriorities = new HashMap<>();
        racingPriorities.put("accelerate", 0.9f);
        racingPriorities.put("turn", 0.85f);
        racingPriorities.put("brake", 0.8f);
        racingPriorities.put("nitro", 0.7f);
        contextualPriorities.put("racing", racingPriorities);
        
        // Puzzle context
        Map<String, Float> puzzlePriorities = new HashMap<>();
        puzzlePriorities.put("interact", 0.8f);
        puzzlePriorities.put("examine", 0.7f);
        puzzlePriorities.put("combine", 0.6f);
        contextualPriorities.put("puzzle", puzzlePriorities);
    }
    
    /**
     * Register an action
     */
    public void registerAction(String actionId, String actionType, Map<String, Object> parameters) {
        if (!actionHistory.containsKey(actionId)) {
            ActionData actionData = new ActionData(actionId, actionType, parameters);
            actionHistory.put(actionId, actionData);
        }
    }
    
    /**
     * Record action execution
     */
    public void recordActionExecution(String actionId, boolean success) {
        ActionData actionData = actionHistory.get(actionId);
        if (actionData != null) {
            actionData.recordUse(success);
            
            // Update recent actions
            synchronized (recentActions) {
                recentActions.put(actionId, actionData);
            }
        }
    }
    
    /**
     * Record user feedback
     */
    public void recordUserFeedback(String actionId, String feedbackType, 
                                  float score, String context) {
        UserFeedbackRecord record = new UserFeedbackRecord(
                actionId, feedbackType, score, context);
        
        // Store feedback
        String feedbackKey = actionId + "_" + feedbackCount.incrementAndGet();
        userFeedback.put(feedbackKey, record);
        
        // Update action base score
        ActionData actionData = actionHistory.get(actionId);
        if (actionData != null) {
            // Blend new feedback with existing score
            actionData.baseScore = actionData.baseScore * 0.8f + score * 0.2f;
            
            // Ensure score remains within bounds
            actionData.baseScore = Math.max(MIN_PRIORITY, 
                    Math.min(MAX_PRIORITY, actionData.baseScore));
        }
        
        Log.d(TAG, "Recorded user feedback for action " + actionId + 
                ": " + feedbackType + " = " + score);
    }
    
    /**
     * Set contextual priority
     */
    public void setContextualPriority(String context, String actionType, float priority) {
        // Ensure priority is within bounds
        priority = Math.max(MIN_PRIORITY, Math.min(MAX_PRIORITY, priority));
        
        // Get or create context map
        Map<String, Float> contextMap = contextualPriorities.get(context);
        if (contextMap == null) {
            contextMap = new HashMap<>();
            contextualPriorities.put(context, contextMap);
        }
        
        // Set priority
        contextMap.put(actionType, priority);
    }
    
    /**
     * Get contextual priority
     */
    public float getContextualPriority(String context, String actionType) {
        Map<String, Float> contextMap = contextualPriorities.get(context);
        if (contextMap != null && contextMap.containsKey(actionType)) {
            return contextMap.get(actionType);
        }
        return DEFAULT_PRIORITY;
    }
    
    /**
     * Prioritize actions based on current context and history
     */
    public List<PrioritizedAction> prioritizeActions(List<Map<String, Object>> candidateActions, 
                                                    String currentContext,
                                                    Map<String, Object> gameState) {
        if (candidateActions == null || candidateActions.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Increment counter
        totalActionsEvaluated.addAndGet(candidateActions.size());
        
        // Create priority queue for actions
        PriorityQueue<PrioritizedAction> priorityQueue = new PriorityQueue<>(
                Comparator.comparing(PrioritizedAction::getPriority).reversed());
        
        // Process each candidate action
        for (Map<String, Object> actionInfo : candidateActions) {
            String actionId = (String) actionInfo.get("actionId");
            String actionType = (String) actionInfo.get("actionType");
            Map<String, Object> parameters = (Map<String, Object>) actionInfo.get("parameters");
            float inputPriority = ((Number) actionInfo.getOrDefault("priority", DEFAULT_PRIORITY)).floatValue();
            float inputConfidence = ((Number) actionInfo.getOrDefault("confidence", DEFAULT_PRIORITY)).floatValue();
            
            // Calculate final priority
            float finalPriority = calculatePriority(
                    actionId, actionType, parameters, inputPriority, currentContext, gameState);
            
            // Create factors map for explaining priority
            Map<String, Float> priorityFactors = new HashMap<>();
            priorityFactors.put("inputPriority", inputPriority);
            priorityFactors.put("contextBonus", getContextualPriority(currentContext, actionType));
            
            ActionData actionData = actionHistory.get(actionId);
            if (actionData != null) {
                priorityFactors.put("baseScore", actionData.baseScore);
                priorityFactors.put("successRate", actionData.getSuccessRate());
            }
            
            // Apply recency bias
            float recencyBias = calculateRecencyBias(actionId);
            priorityFactors.put("recencyBias", recencyBias);
            
            // Create prioritized action
            PrioritizedAction prioritizedAction = new PrioritizedAction(
                    actionId, actionType, parameters, finalPriority, inputConfidence, priorityFactors);
            
            // Add to queue
            priorityQueue.offer(prioritizedAction);
        }
        
        // Extract sorted actions
        List<PrioritizedAction> result = new ArrayList<>();
        while (!priorityQueue.isEmpty()) {
            result.add(priorityQueue.poll());
        }
        
        return result;
    }
    
    /**
     * Calculate action priority
     */
    private float calculatePriority(String actionId, String actionType, 
                                   Map<String, Object> parameters,
                                   float inputPriority, String currentContext,
                                   Map<String, Object> gameState) {
        float priority = inputPriority;
        
        // 1. Apply context-based modifier
        float contextBonus = getContextualPriority(currentContext, actionType);
        priority *= contextBonus;
        
        // 2. Apply historical success rate
        ActionData actionData = actionHistory.get(actionId);
        if (actionData != null && actionData.useCount > 0) {
            // Success rate impact: higher priority for historically successful actions
            float successRateImpact = actionData.getSuccessRate() * 0.3f;
            
            // Base score impact: influenced by user feedback
            float baseScoreImpact = actionData.baseScore * 0.4f;
            
            priority = (priority * 0.3f) + (successRateImpact + baseScoreImpact);
        }
        
        // 3. Apply recency bias (slightly favor actions not used recently)
        float recencyBias = calculateRecencyBias(actionId);
        priority *= recencyBias;
        
        // 4. Apply situation-specific adjustments
        if (gameState != null) {
            // Health-based adjustments
            Number healthObj = (Number) gameState.get("health");
            if (healthObj != null) {
                float health = healthObj.floatValue();
                if (health < 0.3f && actionType.equals("heal")) {
                    // Prioritize healing when health is low
                    priority *= 1.5f;
                }
            }
            
            // Enemy proximity adjustments
            Boolean enemyNearby = (Boolean) gameState.get("enemyNearby");
            if (Boolean.TRUE.equals(enemyNearby)) {
                if (actionType.equals("attack") || actionType.equals("defend")) {
                    // Prioritize combat actions when enemies are nearby
                    priority *= 1.3f;
                }
            }
            
            // Resource-based adjustments
            Number resourceObj = (Number) gameState.get("resources");
            if (resourceObj != null) {
                float resources = resourceObj.floatValue();
                if (resources < 0.2f && actionType.equals("collect")) {
                    // Prioritize resource collection when low
                    priority *= 1.4f;
                }
            }
        }
        
        // Ensure priority stays within bounds
        return Math.max(MIN_PRIORITY, Math.min(MAX_PRIORITY, priority));
    }
    
    /**
     * Calculate recency bias
     */
    private float calculateRecencyBias(String actionId) {
        // Check if action is in recent actions
        synchronized (recentActions) {
            int position = 0;
            for (String recentId : recentActions.keySet()) {
                if (recentId.equals(actionId)) {
                    // Calculate bias based on position (more recent = lower bias)
                    // This slightly favors actions that haven't been used recently
                    return 1.0f + (0.1f * position / recentActions.size());
                }
                position++;
            }
        }
        
        // If not found in recent actions, give neutral bias
        return 1.0f;
    }
    
    /**
     * Get feedback statistics for an action
     */
    public Map<String, Object> getActionFeedbackStats(String actionId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Action data
        ActionData actionData = actionHistory.get(actionId);
        if (actionData != null) {
            stats.put("useCount", actionData.useCount);
            stats.put("successCount", actionData.successCount);
            stats.put("successRate", actionData.getSuccessRate());
            stats.put("baseScore", actionData.baseScore);
        } else {
            stats.put("exists", false);
            return stats;
        }
        
        // Feedback data
        List<Map<String, Object>> feedbackList = new ArrayList<>();
        for (UserFeedbackRecord record : userFeedback.values()) {
            if (record.actionId.equals(actionId)) {
                Map<String, Object> feedbackItem = new HashMap<>();
                feedbackItem.put("type", record.feedbackType);
                feedbackItem.put("score", record.score);
                feedbackItem.put("timestamp", record.timestamp);
                feedbackItem.put("context", record.context);
                feedbackList.add(feedbackItem);
            }
        }
        stats.put("feedback", feedbackList);
        
        return stats;
    }
    
    /**
     * Get overall statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalActionsEvaluated", totalActionsEvaluated.get());
        stats.put("registeredActionCount", actionHistory.size());
        stats.put("feedbackCount", feedbackCount.get());
        stats.put("contextCount", contextualPriorities.size());
        
        // Action type distribution
        Map<String, Integer> actionTypeDistribution = new HashMap<>();
        for (ActionData actionData : actionHistory.values()) {
            String actionType = actionData.actionType;
            actionTypeDistribution.put(actionType, 
                    actionTypeDistribution.getOrDefault(actionType, 0) + 1);
        }
        stats.put("actionTypeDistribution", actionTypeDistribution);
        
        // Success rates by action type
        Map<String, Float> successRatesByType = new HashMap<>();
        Map<String, Integer> countsByType = new HashMap<>();
        
        for (ActionData actionData : actionHistory.values()) {
            if (actionData.useCount > 0) {
                String actionType = actionData.actionType;
                float successRate = actionData.getSuccessRate();
                
                // Update average
                int count = countsByType.getOrDefault(actionType, 0);
                float currentAvg = successRatesByType.getOrDefault(actionType, 0.0f);
                
                // Calculate new average
                float newAvg = (currentAvg * count + successRate) / (count + 1);
                
                successRatesByType.put(actionType, newAvg);
                countsByType.put(actionType, count + 1);
            }
        }
        stats.put("successRatesByType", successRatesByType);
        
        return stats;
    }
    
    /**
     * Reset statistics
     */
    public void resetStats() {
        totalActionsEvaluated.set(0);
    }
    
    /**
     * Release resources
     */
    public void release() {
        actionHistory.clear();
        contextualPriorities.clear();
        userFeedback.clear();
        recentActions.clear();
        
        Log.i(TAG, "Action prioritizer released");
    }
}
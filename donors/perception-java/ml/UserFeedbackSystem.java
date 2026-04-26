package com.aiassistant.ml;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User Feedback System
 * 
 * Collects, processes and integrates user feedback to improve AI decision-making
 * across different operation modes.
 */
public class UserFeedbackSystem {
    private static final String TAG = "UserFeedbackSystem";
    
    // Singleton instance
    private static UserFeedbackSystem instance;
    
    // Context
    private final Context context;
    
    // Feedback storage
    private final Map<String, FeedbackRecord> feedbackRecords = new ConcurrentHashMap<>();
    private final Map<String, ModeFeedbackProfile> modeProfiles = new ConcurrentHashMap<>();
    private final Map<String, List<DecisionFeedback>> decisionFeedback = new ConcurrentHashMap<>();
    
    // Scheduled executor for periodic tasks
    private final ScheduledExecutorService scheduler;
    
    // System state
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    // Statistics
    private final AtomicInteger totalFeedbackCount = new AtomicInteger(0);
    private final AtomicInteger positiveCount = new AtomicInteger(0);
    private final AtomicInteger negativeCount = new AtomicInteger(0);
    private final AtomicInteger neutralCount = new AtomicInteger(0);
    
    // Callbacks
    private final List<FeedbackCallback> callbacks = new CopyOnWriteArrayList<>();
    
    /**
     * Feedback record
     */
    public static class FeedbackRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        
        final String id;
        final FeedbackType type;
        final String source;
        final String target;
        final float rating;
        final String comment;
        final long timestamp;
        final Map<String, Object> metadata;
        
        public FeedbackRecord(
                String id,
                FeedbackType type,
                String source,
                String target,
                float rating,
                String comment,
                Map<String, Object> metadata) {
            this.id = id;
            this.type = type;
            this.source = source;
            this.target = target;
            this.rating = rating;
            this.comment = comment;
            this.timestamp = System.currentTimeMillis();
            this.metadata = new HashMap<>(metadata);
        }
        
        public String getId() {
            return id;
        }
        
        public FeedbackType getType() {
            return type;
        }
        
        public String getSource() {
            return source;
        }
        
        public String getTarget() {
            return target;
        }
        
        public float getRating() {
            return rating;
        }
        
        public String getComment() {
            return comment;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public boolean isPositive() {
            return rating > 0.0f;
        }
        
        public boolean isNegative() {
            return rating < 0.0f;
        }
        
        public boolean isNeutral() {
            return Math.abs(rating) < 0.01f;
        }
    }
    
    /**
     * Feedback type
     */
    public enum FeedbackType {
        EXPLICIT,       // Direct user rating
        IMPLICIT,       // Derived from user behavior
        CORRECTIVE,     // Corrects an action
        REINFORCEMENT,  // Reinforces an action
        PREFERENCE,     // Sets a preference
        SYSTEM          // System-generated feedback
    }
    
    /**
     * Mode feedback profile
     */
    public static class ModeFeedbackProfile implements Serializable {
        private static final long serialVersionUID = 1L;
        
        final String mode;
        float userSatisfaction;
        float successRate;
        float interactionEfficiency;
        int totalFeedbackCount;
        int positiveCount;
        int negativeCount;
        float averageRating;
        Map<String, Float> featureRatings;
        Map<String, Integer> featureCounts;
        long lastUpdated;
        
        public ModeFeedbackProfile(String mode) {
            this.mode = mode;
            this.userSatisfaction = 0.5f;
            this.successRate = 0.0f;
            this.interactionEfficiency = 0.5f;
            this.totalFeedbackCount = 0;
            this.positiveCount = 0;
            this.negativeCount = 0;
            this.averageRating = 0.0f;
            this.featureRatings = new HashMap<>();
            this.featureCounts = new HashMap<>();
            this.lastUpdated = System.currentTimeMillis();
        }
        
        public String getMode() {
            return mode;
        }
        
        public float getUserSatisfaction() {
            return userSatisfaction;
        }
        
        public float getSuccessRate() {
            return successRate;
        }
        
        public float getInteractionEfficiency() {
            return interactionEfficiency;
        }
        
        public int getTotalFeedbackCount() {
            return totalFeedbackCount;
        }
        
        public int getPositiveCount() {
            return positiveCount;
        }
        
        public int getNegativeCount() {
            return negativeCount;
        }
        
        public float getAverageRating() {
            return averageRating;
        }
        
        public Map<String, Float> getFeatureRatings() {
            return featureRatings;
        }
        
        public Map<String, Integer> getFeatureCounts() {
            return featureCounts;
        }
        
        public long getLastUpdated() {
            return lastUpdated;
        }
        
        public void updateWithFeedback(FeedbackRecord feedback) {
            totalFeedbackCount++;
            if (feedback.isPositive()) {
                positiveCount++;
            } else if (feedback.isNegative()) {
                negativeCount++;
            }
            
            // Update average rating
            float oldTotal = averageRating * (totalFeedbackCount - 1);
            averageRating = (oldTotal + feedback.getRating()) / totalFeedbackCount;
            
            // Update user satisfaction
            userSatisfaction = 0.7f * userSatisfaction + 0.3f * (averageRating + 1.0f) / 2.0f;
            
            // Update feature ratings if available
            if (feedback.getMetadata().containsKey("feature")) {
                String feature = (String) feedback.getMetadata().get("feature");
                float featureRating = feedback.getRating();
                
                // Get current count and rating
                int count = featureCounts.getOrDefault(feature, 0);
                float currentRating = featureRatings.getOrDefault(feature, 0.0f);
                
                // Calculate new average
                float newRating;
                if (count == 0) {
                    newRating = featureRating;
                } else {
                    newRating = (currentRating * count + featureRating) / (count + 1);
                }
                
                // Update maps
                featureRatings.put(feature, newRating);
                featureCounts.put(feature, count + 1);
            }
            
            // Update success rate if available
            if (feedback.getMetadata().containsKey("success")) {
                boolean success = (boolean) feedback.getMetadata().get("success");
                // Adjust success rate
                successRate = 0.9f * successRate + 0.1f * (success ? 1.0f : 0.0f);
            }
            
            // Update interaction efficiency if available
            if (feedback.getMetadata().containsKey("efficiency")) {
                float efficiency = ((Number) feedback.getMetadata().get("efficiency")).floatValue();
                // Adjust efficiency (0-1 range)
                interactionEfficiency = 0.8f * interactionEfficiency + 0.2f * efficiency;
            }
            
            lastUpdated = System.currentTimeMillis();
        }
    }
    
    /**
     * Decision feedback
     */
    public static class DecisionFeedback implements Serializable {
        private static final long serialVersionUID = 1L;
        
        final String decisionId;
        final String decisionType;
        final Map<String, Object> decisionParams;
        final float rating;
        final long timestamp;
        final Map<String, Object> context;
        
        public DecisionFeedback(
                String decisionId,
                String decisionType,
                Map<String, Object> decisionParams,
                float rating,
                Map<String, Object> context) {
            this.decisionId = decisionId;
            this.decisionType = decisionType;
            this.decisionParams = new HashMap<>(decisionParams);
            this.rating = rating;
            this.timestamp = System.currentTimeMillis();
            this.context = new HashMap<>(context);
        }
        
        public String getDecisionId() {
            return decisionId;
        }
        
        public String getDecisionType() {
            return decisionType;
        }
        
        public Map<String, Object> getDecisionParams() {
            return decisionParams;
        }
        
        public float getRating() {
            return rating;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public Map<String, Object> getContext() {
            return context;
        }
    }
    
    /**
     * Learning parameters
     */
    public static class LearningParameters {
        float learningRate;
        float explorationRate;
        float discountFactor;
        int batchSize;
        int updateFrequency;
        
        public LearningParameters() {
            this.learningRate = 0.1f;
            this.explorationRate = 0.2f;
            this.discountFactor = 0.9f;
            this.batchSize = 16;
            this.updateFrequency = 50;
        }
        
        public float getLearningRate() {
            return learningRate;
        }
        
        public void setLearningRate(float learningRate) {
            this.learningRate = learningRate;
        }
        
        public float getExplorationRate() {
            return explorationRate;
        }
        
        public void setExplorationRate(float explorationRate) {
            this.explorationRate = explorationRate;
        }
        
        public float getDiscountFactor() {
            return discountFactor;
        }
        
        public void setDiscountFactor(float discountFactor) {
            this.discountFactor = discountFactor;
        }
        
        public int getBatchSize() {
            return batchSize;
        }
        
        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
        
        public int getUpdateFrequency() {
            return updateFrequency;
        }
        
        public void setUpdateFrequency(int updateFrequency) {
            this.updateFrequency = updateFrequency;
        }
    }
    
    /**
     * Decision improvement
     */
    public static class DecisionImprovement {
        final String decisionType;
        final List<ActionAdjustment> adjustments;
        final float confidenceLevel;
        final String explanation;
        
        public DecisionImprovement(
                String decisionType,
                List<ActionAdjustment> adjustments,
                float confidenceLevel,
                String explanation) {
            this.decisionType = decisionType;
            this.adjustments = new ArrayList<>(adjustments);
            this.confidenceLevel = confidenceLevel;
            this.explanation = explanation;
        }
        
        public String getDecisionType() {
            return decisionType;
        }
        
        public List<ActionAdjustment> getAdjustments() {
            return adjustments;
        }
        
        public float getConfidenceLevel() {
            return confidenceLevel;
        }
        
        public String getExplanation() {
            return explanation;
        }
    }
    
    /**
     * Action adjustment
     */
    public static class ActionAdjustment {
        final String paramName;
        final Object originalValue;
        final Object recommendedValue;
        final float adjustmentStrength;
        
        public ActionAdjustment(
                String paramName,
                Object originalValue,
                Object recommendedValue,
                float adjustmentStrength) {
            this.paramName = paramName;
            this.originalValue = originalValue;
            this.recommendedValue = recommendedValue;
            this.adjustmentStrength = adjustmentStrength;
        }
        
        public String getParamName() {
            return paramName;
        }
        
        public Object getOriginalValue() {
            return originalValue;
        }
        
        public Object getRecommendedValue() {
            return recommendedValue;
        }
        
        public float getAdjustmentStrength() {
            return adjustmentStrength;
        }
    }
    
    /**
     * Feedback callback
     */
    public interface FeedbackCallback {
        void onFeedbackProcessed(FeedbackRecord feedback);
        void onDecisionImprovementGenerated(DecisionImprovement improvement);
        void onModeFeedbackUpdated(ModeFeedbackProfile profile);
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized UserFeedbackSystem getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new UserFeedbackSystem(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private UserFeedbackSystem(Context context) {
        this.context = context;
        this.scheduler = new ScheduledThreadPoolExecutor(1);
        
        Log.i(TAG, "User feedback system created");
    }
    
    /**
     * Initialize the system
     */
    public void initialize() {
        if (initialized.get()) {
            return;
        }
        
        // Create operation mode profiles
        initializeModeProfiles();
        
        // Schedule periodic saving of feedback data
        scheduler.scheduleAtFixedRate(
                this::saveData,
                30, // Initial delay
                300, // Save every 5 minutes
                TimeUnit.SECONDS);
        
        // Load existing data
        loadData();
        
        initialized.set(true);
        Log.i(TAG, "User feedback system initialized");
    }
    
    /**
     * Initialize mode profiles
     */
    private void initializeModeProfiles() {
        // Create profiles for each mode
        modeProfiles.put("DISABLED", new ModeFeedbackProfile("DISABLED"));
        modeProfiles.put("OBSERVATION", new ModeFeedbackProfile("OBSERVATION"));
        modeProfiles.put("SUGGESTION", new ModeFeedbackProfile("SUGGESTION"));
        modeProfiles.put("ASSISTED", new ModeFeedbackProfile("ASSISTED"));
        modeProfiles.put("AUTONOMOUS", new ModeFeedbackProfile("AUTONOMOUS"));
    }
    
    /**
     * Register feedback callback
     */
    public void registerCallback(FeedbackCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }
    
    /**
     * Unregister feedback callback
     */
    public void unregisterCallback(FeedbackCallback callback) {
        callbacks.remove(callback);
    }
    
    /**
     * Start the feedback system
     */
    public void start() {
        if (!initialized.get()) {
            initialize();
        }
        
        if (running.get()) {
            return;
        }
        
        running.set(true);
        Log.i(TAG, "User feedback system started");
    }
    
    /**
     * Stop the feedback system
     */
    public void stop() {
        if (!running.get()) {
            return;
        }
        
        running.set(false);
        
        // Save data
        saveData();
        
        Log.i(TAG, "User feedback system stopped");
    }
    
    /**
     * Record user feedback
     */
    public void recordFeedback(
            FeedbackType type,
            String source,
            String target,
            float rating,
            String comment,
            Map<String, Object> metadata) {
        
        if (!running.get()) {
            Log.w(TAG, "Cannot record feedback: system not running");
            return;
        }
        
        // Create unique ID
        String id = "feedback_" + System.currentTimeMillis() + "_" + totalFeedbackCount.get();
        
        // Create feedback record
        FeedbackRecord record = new FeedbackRecord(
                id, type, source, target, rating, comment, metadata);
        
        // Store feedback
        feedbackRecords.put(id, record);
        
        // Update statistics
        totalFeedbackCount.incrementAndGet();
        if (record.isPositive()) {
            positiveCount.incrementAndGet();
        } else if (record.isNegative()) {
            negativeCount.incrementAndGet();
        } else {
            neutralCount.incrementAndGet();
        }
        
        // Update mode profile if applicable
        if (metadata.containsKey("mode")) {
            String mode = metadata.get("mode").toString();
            ModeFeedbackProfile profile = modeProfiles.get(mode);
            if (profile != null) {
                profile.updateWithFeedback(record);
                
                // Notify callbacks
                for (FeedbackCallback callback : callbacks) {
                    try {
                        callback.onModeFeedbackUpdated(profile);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in mode feedback callback: " + e.getMessage());
                    }
                }
            }
        }
        
        // Process feedback
        processFeedback(record);
        
        Log.d(TAG, "Recorded feedback: " + id + " with rating " + rating);
    }
    
    /**
     * Record decision feedback
     */
    public void recordDecisionFeedback(
            String decisionId,
            String decisionType,
            Map<String, Object> decisionParams,
            float rating,
            Map<String, Object> context) {
        
        if (!running.get()) {
            Log.w(TAG, "Cannot record decision feedback: system not running");
            return;
        }
        
        // Create decision feedback record
        DecisionFeedback feedback = new DecisionFeedback(
                decisionId, decisionType, decisionParams, rating, context);
        
        // Store feedback
        if (!decisionFeedback.containsKey(decisionType)) {
            decisionFeedback.put(decisionType, new ArrayList<>());
        }
        decisionFeedback.get(decisionType).add(feedback);
        
        // Process for decision improvements
        generateDecisionImprovements(decisionType);
        
        Log.d(TAG, "Recorded decision feedback for: " + decisionId + 
                " of type " + decisionType + " with rating " + rating);
    }
    
    /**
     * Process feedback
     */
    private void processFeedback(FeedbackRecord record) {
        // Process feedback and generate insights
        
        // Notify callbacks
        for (FeedbackCallback callback : callbacks) {
            try {
                callback.onFeedbackProcessed(record);
            } catch (Exception e) {
                Log.e(TAG, "Error in feedback processed callback: " + e.getMessage());
            }
        }
    }
    
    /**
     * Generate decision improvements
     */
    private void generateDecisionImprovements(String decisionType) {
        List<DecisionFeedback> feedbackList = decisionFeedback.get(decisionType);
        if (feedbackList == null || feedbackList.size() < 5) {
            // Need more data for meaningful improvements
            return;
        }
        
        // Get recent feedback
        List<DecisionFeedback> recentFeedback = new ArrayList<>();
        for (int i = Math.max(0, feedbackList.size() - 10); i < feedbackList.size(); i++) {
            recentFeedback.add(feedbackList.get(i));
        }
        
        // Analyze feedback to identify patterns
        Map<String, List<Float>> paramRatings = new HashMap<>();
        
        for (DecisionFeedback feedback : recentFeedback) {
            for (Map.Entry<String, Object> entry : feedback.getDecisionParams().entrySet()) {
                String paramName = entry.getKey();
                Object paramValue = entry.getValue();
                
                // Skip non-numeric parameters for now
                if (!(paramValue instanceof Number)) {
                    continue;
                }
                
                // Get or create ratings list
                if (!paramRatings.containsKey(paramName)) {
                    paramRatings.put(paramName, new ArrayList<>());
                }
                
                // Add rating
                paramRatings.get(paramName).add(feedback.getRating());
            }
        }
        
        // Identify parameters that most impact ratings
        List<ActionAdjustment> adjustments = new ArrayList<>();
        
        for (Map.Entry<String, List<Float>> entry : paramRatings.entrySet()) {
            String paramName = entry.getKey();
            List<Float> ratings = entry.getValue();
            
            if (ratings.size() < 3) {
                continue;
            }
            
            // Calculate average rating
            float sum = 0;
            for (float rating : ratings) {
                sum += rating;
            }
            float avgRating = sum / ratings.size();
            
            // If average rating is low, suggest adjustment
            if (avgRating < 0.0f) {
                // Get the most recent positive feedback for this parameter
                DecisionFeedback positiveFeedback = null;
                for (int i = recentFeedback.size() - 1; i >= 0; i--) {
                    DecisionFeedback feedback = recentFeedback.get(i);
                    if (feedback.getRating() > 0.5f && 
                            feedback.getDecisionParams().containsKey(paramName)) {
                        positiveFeedback = feedback;
                        break;
                    }
                }
                
                // If found positive example, create adjustment
                if (positiveFeedback != null) {
                    // Get values
                    Object recommendedValue = positiveFeedback.getDecisionParams().get(paramName);
                    
                    // Get original value from most recent feedback
                    DecisionFeedback mostRecent = recentFeedback.get(recentFeedback.size() - 1);
                    Object originalValue = mostRecent.getDecisionParams().get(paramName);
                    
                    // Create adjustment
                    adjustments.add(new ActionAdjustment(
                            paramName,
                            originalValue,
                            recommendedValue,
                            Math.abs(avgRating)));
                }
            }
        }
        
        // If we have adjustments, create improvement
        if (!adjustments.isEmpty()) {
            DecisionImprovement improvement = new DecisionImprovement(
                    decisionType,
                    adjustments,
                    0.7f, // Confidence level
                    "Based on user feedback, adjusting parameters for better performance");
            
            // Notify callbacks
            for (FeedbackCallback callback : callbacks) {
                try {
                    callback.onDecisionImprovementGenerated(improvement);
                } catch (Exception e) {
                    Log.e(TAG, "Error in decision improvement callback: " + e.getMessage());
                }
            }
            
            Log.d(TAG, "Generated decision improvement for: " + decisionType + 
                    " with " + adjustments.size() + " adjustments");
        }
    }
    
    /**
     * Get feedback by ID
     */
    public FeedbackRecord getFeedback(String feedbackId) {
        return feedbackRecords.get(feedbackId);
    }
    
    /**
     * Get all feedback records
     */
    public List<FeedbackRecord> getAllFeedback() {
        return new ArrayList<>(feedbackRecords.values());
    }
    
    /**
     * Get recent feedback
     */
    public List<FeedbackRecord> getRecentFeedback(int count) {
        List<FeedbackRecord> allFeedback = new ArrayList<>(feedbackRecords.values());
        allFeedback.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        
        return allFeedback.subList(0, Math.min(count, allFeedback.size()));
    }
    
    /**
     * Get feedback for mode
     */
    public List<FeedbackRecord> getFeedbackForMode(String mode) {
        List<FeedbackRecord> result = new ArrayList<>();
        
        for (FeedbackRecord record : feedbackRecords.values()) {
            if (record.getMetadata().containsKey("mode") && 
                    record.getMetadata().get("mode").equals(mode)) {
                result.add(record);
            }
        }
        
        return result;
    }
    
    /**
     * Get mode profile
     */
    public ModeFeedbackProfile getModeProfile(String mode) {
        return modeProfiles.get(mode);
    }
    
    /**
     * Get all mode profiles
     */
    public List<ModeFeedbackProfile> getAllModeProfiles() {
        return new ArrayList<>(modeProfiles.values());
    }
    
    /**
     * Get mode preferences
     */
    public Map<String, Object> getModePreferences(String mode) {
        Map<String, Object> preferences = new HashMap<>();
        ModeFeedbackProfile profile = modeProfiles.get(mode);
        
        if (profile == null) {
            return preferences;
        }
        
        // Add general preferences
        preferences.put("userSatisfaction", profile.getUserSatisfaction());
        preferences.put("successRate", profile.getSuccessRate());
        preferences.put("interactionEfficiency", profile.getInteractionEfficiency());
        
        // Add feature-specific preferences
        Map<String, Float> featureRatings = profile.getFeatureRatings();
        for (Map.Entry<String, Float> entry : featureRatings.entrySet()) {
            preferences.put("feature_" + entry.getKey(), entry.getValue());
        }
        
        return preferences;
    }
    
    /**
     * Get learning parameters for a mode
     */
    public LearningParameters getLearningParameters(String mode) {
        LearningParameters params = new LearningParameters();
        ModeFeedbackProfile profile = modeProfiles.get(mode);
        
        if (profile == null) {
            return params;
        }
        
        // Adjust learning rate based on feedback volume
        int feedbackCount = profile.getTotalFeedbackCount();
        if (feedbackCount > 100) {
            // Slow down learning when we have lots of data
            params.setLearningRate(0.05f);
        } else if (feedbackCount < 10) {
            // Fast learning with limited data
            params.setLearningRate(0.2f);
        }
        
        // Adjust exploration rate based on user satisfaction
        float satisfaction = profile.getUserSatisfaction();
        if (satisfaction > 0.8f) {
            // Less exploration when user is satisfied
            params.setExplorationRate(0.1f);
        } else if (satisfaction < 0.4f) {
            // More exploration when user is not satisfied
            params.setExplorationRate(0.3f);
        }
        
        return params;
    }
    
    /**
     * Save feedback data
     */
    public void saveData() {
        if (!initialized.get()) {
            return;
        }
        
        try {
            File dataDir = new File(context.getFilesDir(), "feedback");
            if (!dataDir.exists() && !dataDir.mkdirs()) {
                Log.e(TAG, "Failed to create feedback directory");
                return;
            }
            
            // Save feedback records
            File feedbackFile = new File(dataDir, "feedback_records.dat");
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(feedbackFile))) {
                // Convert to HashMap for serialization
                oos.writeObject(new HashMap<>(feedbackRecords));
            }
            
            // Save mode profiles
            File profilesFile = new File(dataDir, "mode_profiles.dat");
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(profilesFile))) {
                oos.writeObject(new HashMap<>(modeProfiles));
            }
            
            // Save decision feedback
            File decisionFile = new File(dataDir, "decision_feedback.dat");
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(decisionFile))) {
                oos.writeObject(new HashMap<>(decisionFeedback));
            }
            
            Log.i(TAG, "Saved feedback data: " + 
                    feedbackRecords.size() + " records, " + 
                    modeProfiles.size() + " profiles");
                    
        } catch (IOException e) {
            Log.e(TAG, "Error saving feedback data: " + e.getMessage());
        }
    }
    
    /**
     * Load feedback data
     */
    @SuppressWarnings("unchecked")
    private void loadData() {
        try {
            File dataDir = new File(context.getFilesDir(), "feedback");
            if (!dataDir.exists()) {
                Log.i(TAG, "No feedback data to load");
                return;
            }
            
            // Load feedback records
            File feedbackFile = new File(dataDir, "feedback_records.dat");
            if (feedbackFile.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(
                        new FileInputStream(feedbackFile))) {
                    Map<String, FeedbackRecord> loadedRecords = 
                            (Map<String, FeedbackRecord>) ois.readObject();
                    feedbackRecords.putAll(loadedRecords);
                    
                    // Update statistics
                    totalFeedbackCount.set(feedbackRecords.size());
                    
                    // Count positive/negative/neutral
                    int positive = 0;
                    int negative = 0;
                    int neutral = 0;
                    
                    for (FeedbackRecord record : feedbackRecords.values()) {
                        if (record.isPositive()) {
                            positive++;
                        } else if (record.isNegative()) {
                            negative++;
                        } else {
                            neutral++;
                        }
                    }
                    
                    positiveCount.set(positive);
                    negativeCount.set(negative);
                    neutralCount.set(neutral);
                }
            }
            
            // Load mode profiles
            File profilesFile = new File(dataDir, "mode_profiles.dat");
            if (profilesFile.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(
                        new FileInputStream(profilesFile))) {
                    Map<String, ModeFeedbackProfile> loadedProfiles = 
                            (Map<String, ModeFeedbackProfile>) ois.readObject();
                    modeProfiles.putAll(loadedProfiles);
                }
            }
            
            // Load decision feedback
            File decisionFile = new File(dataDir, "decision_feedback.dat");
            if (decisionFile.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(
                        new FileInputStream(decisionFile))) {
                    Map<String, List<DecisionFeedback>> loadedFeedback = 
                            (Map<String, List<DecisionFeedback>>) ois.readObject();
                    decisionFeedback.putAll(loadedFeedback);
                }
            }
            
            Log.i(TAG, "Loaded feedback data: " + 
                    feedbackRecords.size() + " records, " + 
                    modeProfiles.size() + " profiles");
                    
        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, "Error loading feedback data: " + e.getMessage());
        }
    }
    
    /**
     * Get statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("initialized", initialized.get());
        stats.put("running", running.get());
        stats.put("totalFeedbackCount", totalFeedbackCount.get());
        stats.put("positiveCount", positiveCount.get());
        stats.put("negativeCount", negativeCount.get());
        stats.put("neutralCount", neutralCount.get());
        
        // Add mode stats
        Map<String, Object> modeStats = new HashMap<>();
        for (ModeFeedbackProfile profile : modeProfiles.values()) {
            Map<String, Object> profileStats = new HashMap<>();
            profileStats.put("userSatisfaction", profile.getUserSatisfaction());
            profileStats.put("successRate", profile.getSuccessRate());
            profileStats.put("feedbackCount", profile.getTotalFeedbackCount());
            
            modeStats.put(profile.getMode(), profileStats);
        }
        stats.put("modeStats", modeStats);
        
        return stats;
    }
    
    /**
     * Reset statistics
     */
    public void resetStats() {
        totalFeedbackCount.set(0);
        positiveCount.set(0);
        negativeCount.set(0);
        neutralCount.set(0);
    }
    
    /**
     * Clear all feedback data
     */
    public void clearAllData() {
        feedbackRecords.clear();
        decisionFeedback.clear();
        
        // Reset mode profiles
        initializeModeProfiles();
        
        // Reset statistics
        resetStats();
        
        // Delete stored data
        try {
            File dataDir = new File(context.getFilesDir(), "feedback");
            if (dataDir.exists()) {
                File[] files = dataDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.delete()) {
                            Log.w(TAG, "Failed to delete " + file.getName());
                        }
                    }
                }
                if (!dataDir.delete()) {
                    Log.w(TAG, "Failed to delete feedback directory");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing data: " + e.getMessage());
        }
        
        Log.i(TAG, "All feedback data cleared");
    }
    
    /**
     * Release resources
     */
    public void release() {
        // Stop the system
        stop();
        
        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Save data
        saveData();
        
        // Clear in-memory data
        feedbackRecords.clear();
        modeProfiles.clear();
        decisionFeedback.clear();
        callbacks.clear();
        
        initialized.set(false);
        
        Log.i(TAG, "User feedback system released");
    }
}
package com.aiassistant.ml;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Advanced Action Sequencer for complex gaming strategies
 * 
 * This class provides sophisticated action prioritization and sequencing to
 * execute optimized action chains for fast-paced gaming.
 */
public class AdvancedActionSequencer {
    private static final String TAG = "AdvancedActionSequencer";
    
    // Singleton instance
    private static AdvancedActionSequencer instance;
    
    // Context
    private final Context context;
    
    // Action queue
    private final PriorityQueue<ActionItem> actionQueue;
    
    // Action sequences
    private final Map<String, ActionSequence> sequences;
    
    // Active sequence
    private ActionSequence activeSequence;
    
    // Executors
    private final ScheduledExecutorService scheduler;
    private final Executor actionExecutor;
    
    // State
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger sequenceCounter = new AtomicInteger(0);
    private final AtomicInteger actionCounter = new AtomicInteger(0);
    
    // Action callback
    public interface ActionCallback {
        void onAction(PredictiveActionSystem.GameAction action);
        void onSequenceComplete(String sequenceId);
        void onError(String error);
    }
    
    // Callback list
    private final List<ActionCallback> callbacks = new CopyOnWriteArrayList<>();
    
    /**
     * Action item in the queue
     */
    private static class ActionItem implements Comparable<ActionItem> {
        final long executionTime; // Absolute time in milliseconds
        final PredictiveActionSystem.GameAction action;
        final float priority;
        final String sequenceId;
        final int sequencePosition;
        
        ActionItem(PredictiveActionSystem.GameAction action, long executionTime, 
                   float priority, String sequenceId, int sequencePosition) {
            this.action = action;
            this.executionTime = executionTime;
            this.priority = priority;
            this.sequenceId = sequenceId;
            this.sequencePosition = sequencePosition;
        }
        
        @Override
        public int compareTo(ActionItem other) {
            // First order by execution time
            int timeCompare = Long.compare(executionTime, other.executionTime);
            if (timeCompare != 0) {
                return timeCompare;
            }
            
            // Then by priority (higher priority first)
            return Float.compare(other.priority, priority);
        }
    }
    
    /**
     * Action sequence definition
     */
    public static class ActionSequence {
        private final String id;
        private final String name;
        private final List<PredictiveActionSystem.GameAction> actions;
        private final Map<String, Object> parameters;
        private long[] timingOffsets; // Milliseconds from sequence start
        private float priority;
        private boolean interruptible;
        private String gameContext; // When this sequence applies
        private String condition; // Condition for execution
        
        // Execution state
        private int currentPosition;
        private long startTime;
        private boolean completed;
        private boolean aborted;
        
        /**
         * Create a new sequence
         */
        public ActionSequence(String id, String name) {
            this.id = id;
            this.name = name;
            this.actions = new ArrayList<>();
            this.parameters = new HashMap<>();
            this.timingOffsets = new long[0];
            this.priority = 1.0f;
            this.interruptible = true;
            this.gameContext = "";
            this.condition = "";
            this.currentPosition = -1;
            this.completed = false;
            this.aborted = false;
        }
        
        /**
         * Add an action to the sequence
         */
        public void addAction(PredictiveActionSystem.GameAction action) {
            actions.add(action);
            
            // Update timing offsets
            long[] newOffsets = new long[actions.size()];
            if (timingOffsets.length > 0) {
                System.arraycopy(timingOffsets, 0, newOffsets, 0, timingOffsets.length);
            }
            
            // Default timing: 200ms intervals
            if (actions.size() > 1) {
                newOffsets[actions.size() - 1] = newOffsets[actions.size() - 2] + 200;
            }
            
            this.timingOffsets = newOffsets;
        }
        
        /**
         * Set timing offsets for the sequence
         */
        public void setTimingOffsets(long[] offsets) {
            if (offsets.length != actions.size()) {
                throw new IllegalArgumentException("Timing offsets length must match actions length");
            }
            this.timingOffsets = offsets;
        }
        
        /**
         * Set priority for the sequence
         */
        public void setPriority(float priority) {
            this.priority = priority;
        }
        
        /**
         * Set whether the sequence can be interrupted
         */
        public void setInterruptible(boolean interruptible) {
            this.interruptible = interruptible;
        }
        
        /**
         * Set game context in which this sequence applies
         */
        public void setGameContext(String gameContext) {
            this.gameContext = gameContext;
        }
        
        /**
         * Set condition for execution
         */
        public void setCondition(String condition) {
            this.condition = condition;
        }
        
        /**
         * Add a parameter to the sequence
         */
        public void addParameter(String key, Object value) {
            parameters.put(key, value);
        }
        
        /**
         * Start the sequence
         */
        public void start() {
            startTime = System.currentTimeMillis();
            currentPosition = 0;
            completed = false;
            aborted = false;
        }
        
        /**
         * Mark sequence as completed
         */
        public void complete() {
            currentPosition = actions.size();
            completed = true;
        }
        
        /**
         * Abort the sequence
         */
        public void abort() {
            aborted = true;
        }
        
        /**
         * Check if sequence is active
         */
        public boolean isActive() {
            return currentPosition >= 0 && currentPosition < actions.size() && !completed && !aborted;
        }
        
        /**
         * Get ID
         */
        public String getId() {
            return id;
        }
        
        /**
         * Get name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Get priority
         */
        public float getPriority() {
            return priority;
        }
        
        /**
         * Get interruptible flag
         */
        public boolean isInterruptible() {
            return interruptible;
        }
        
        /**
         * Get game context
         */
        public String getGameContext() {
            return gameContext;
        }
        
        /**
         * Get condition
         */
        public String getCondition() {
            return condition;
        }
        
        /**
         * Get actions
         */
        public List<PredictiveActionSystem.GameAction> getActions() {
            return actions;
        }
        
        /**
         * Get parameters
         */
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        /**
         * Get timing offsets
         */
        public long[] getTimingOffsets() {
            return timingOffsets;
        }
        
        /**
         * Get current position
         */
        public int getCurrentPosition() {
            return currentPosition;
        }
        
        /**
         * Advance to next position
         */
        public void advancePosition() {
            currentPosition++;
            if (currentPosition >= actions.size()) {
                completed = true;
            }
        }
        
        /**
         * Check if completed
         */
        public boolean isCompleted() {
            return completed;
        }
        
        /**
         * Check if aborted
         */
        public boolean isAborted() {
            return aborted;
        }
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized AdvancedActionSequencer getInstance(Context context) {
        if (instance == null) {
            instance = new AdvancedActionSequencer(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private AdvancedActionSequencer(Context context) {
        this.context = context;
        
        // Create priority queue for actions
        actionQueue = new PriorityQueue<>();
        
        // Create map for sequences
        sequences = new HashMap<>();
        
        // Create executors
        scheduler = Executors.newSingleThreadScheduledExecutor();
        actionExecutor = Executors.newSingleThreadExecutor();
        
        Log.i(TAG, "Advanced action sequencer created");
    }
    
    /**
     * Start the sequencer
     */
    public void start() {
        if (running.get()) {
            return;
        }
        
        running.set(true);
        
        // Start action processor
        scheduler.scheduleAtFixedRate(
                this::processActions,
                0,
                10, // 10ms intervals for high precision
                TimeUnit.MILLISECONDS);
        
        Log.i(TAG, "Action sequencer started");
    }
    
    /**
     * Stop the sequencer
     */
    public void stop() {
        running.set(false);
        
        // Clear queue
        synchronized (actionQueue) {
            actionQueue.clear();
        }
        
        // Reset active sequence
        activeSequence = null;
        
        Log.i(TAG, "Action sequencer stopped");
    }
    
    /**
     * Register action callback
     */
    public void registerCallback(ActionCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }
    
    /**
     * Unregister action callback
     */
    public void unregisterCallback(ActionCallback callback) {
        callbacks.remove(callback);
    }
    
    /**
     * Process due actions
     */
    private void processActions() {
        if (!running.get()) {
            return;
        }
        
        try {
            long currentTime = System.currentTimeMillis();
            
            // Process all due actions
            while (true) {
                ActionItem actionItem;
                
                synchronized (actionQueue) {
                    // Get the next due action
                    if (actionQueue.isEmpty() || actionQueue.peek().executionTime > currentTime) {
                        break;
                    }
                    
                    actionItem = actionQueue.poll();
                }
                
                // Execute the action
                executeAction(actionItem);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing actions: " + e.getMessage());
        }
    }
    
    /**
     * Execute an action
     */
    private void executeAction(ActionItem actionItem) {
        actionExecutor.execute(() -> {
            try {
                // Notify callbacks
                for (ActionCallback callback : callbacks) {
                    try {
                        callback.onAction(actionItem.action);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in action callback: " + e.getMessage());
                    }
                }
                
                // Check if part of a sequence
                if (actionItem.sequenceId != null && !actionItem.sequenceId.isEmpty()) {
                    // Get the sequence
                    ActionSequence sequence = sequences.get(actionItem.sequenceId);
                    if (sequence != null && sequence.isActive() && 
                            sequence.getCurrentPosition() == actionItem.sequencePosition) {
                        // Advance sequence position
                        sequence.advancePosition();
                        
                        // If sequence completed, notify callbacks
                        if (sequence.isCompleted()) {
                            activeSequence = null;
                            
                            for (ActionCallback callback : callbacks) {
                                try {
                                    callback.onSequenceComplete(sequence.getId());
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in sequence completion callback: " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error executing action: " + e.getMessage());
            }
        });
    }
    
    /**
     * Add a single action to the queue
     */
    public void queueAction(PredictiveActionSystem.GameAction action, long delayMs, float priority) {
        if (!running.get()) {
            return;
        }
        
        // Don't queue if delay is negative
        if (delayMs < 0) {
            Log.w(TAG, "Ignoring action with negative delay");
            return;
        }
        
        // Create action item
        long executionTime = System.currentTimeMillis() + delayMs;
        ActionItem item = new ActionItem(action, executionTime, priority, "", -1);
        
        // Add to queue
        synchronized (actionQueue) {
            actionQueue.offer(item);
        }
    }
    
    /**
     * Create a new action sequence
     */
    public ActionSequence createSequence(String name) {
        String id = "sequence_" + sequenceCounter.incrementAndGet();
        ActionSequence sequence = new ActionSequence(id, name);
        sequences.put(id, sequence);
        return sequence;
    }
    
    /**
     * Execute an action sequence
     */
    public boolean executeSequence(String sequenceId) {
        ActionSequence sequence = sequences.get(sequenceId);
        if (sequence == null) {
            Log.e(TAG, "Sequence not found: " + sequenceId);
            return false;
        }
        
        return executeSequence(sequence);
    }
    
    /**
     * Execute an action sequence
     */
    public boolean executeSequence(ActionSequence sequence) {
        if (!running.get()) {
            return false;
        }
        
        // Check if a non-interruptible sequence is active
        if (activeSequence != null && !activeSequence.isInterruptible()) {
            Log.w(TAG, "Cannot execute sequence: non-interruptible sequence active");
            return false;
        }
        
        // Cancel any active sequence
        if (activeSequence != null) {
            activeSequence.abort();
        }
        
        // Set as active sequence
        activeSequence = sequence;
        
        // Start the sequence
        sequence.start();
        
        // Queue all actions in the sequence
        long baseTime = System.currentTimeMillis();
        List<PredictiveActionSystem.GameAction> actions = sequence.getActions();
        long[] timingOffsets = sequence.getTimingOffsets();
        
        for (int i = 0; i < actions.size(); i++) {
            PredictiveActionSystem.GameAction action = actions.get(i);
            long executionTime = baseTime + timingOffsets[i];
            
            // Create action item with sequence information
            ActionItem item = new ActionItem(
                    action, 
                    executionTime, 
                    sequence.getPriority(), 
                    sequence.getId(), 
                    i);
            
            // Add to queue
            synchronized (actionQueue) {
                actionQueue.offer(item);
            }
        }
        
        Log.i(TAG, "Executing sequence: " + sequence.getId() + " with " + actions.size() + " actions");
        return true;
    }
    
    /**
     * Cancel an active sequence
     */
    public boolean cancelActiveSequence() {
        if (activeSequence == null) {
            return false;
        }
        
        // Abort sequence
        activeSequence.abort();
        
        // Remove actions from queue
        synchronized (actionQueue) {
            actionQueue.removeIf(item -> 
                    item.sequenceId.equals(activeSequence.getId()));
        }
        
        // Clear active sequence
        activeSequence = null;
        
        return true;
    }
    
    /**
     * Create a combo sequence
     * 
     * A combo is a predefined, optimized sequence for fast execution
     */
    public ActionSequence createComboSequence(String name, String comboType) {
        ActionSequence sequence = createSequence(name);
        
        // Configure based on combo type
        switch (comboType) {
            case "attack_combo":
                createAttackCombo(sequence);
                break;
                
            case "defense_combo":
                createDefenseCombo(sequence);
                break;
                
            case "movement_combo":
                createMovementCombo(sequence);
                break;
                
            case "special_combo":
                createSpecialCombo(sequence);
                break;
                
            default:
                Log.w(TAG, "Unknown combo type: " + comboType);
                break;
        }
        
        return sequence;
    }
    
    /**
     * Create attack combo
     */
    private void createAttackCombo(ActionSequence sequence) {
        // Create a typical attack combo sequence
        // This would be game-specific in a real implementation
        
        // Example: Three-hit combo with specific timing
        
        // Attack 1
        Map<String, Object> params1 = new HashMap<>();
        params1.put("x", 0.8f);
        params1.put("y", 0.7f);
        PredictiveActionSystem.GameAction action1 = 
                new PredictiveActionSystem.GameAction(
                        PredictiveActionSystem.ActionType.TAP, 
                        params1, 1.0f, 0.9f);
        sequence.addAction(action1);
        
        // Attack 2
        Map<String, Object> params2 = new HashMap<>();
        params2.put("x", 0.85f);
        params2.put("y", 0.7f);
        PredictiveActionSystem.GameAction action2 = 
                new PredictiveActionSystem.GameAction(
                        PredictiveActionSystem.ActionType.TAP, 
                        params2, 1.0f, 0.9f);
        sequence.addAction(action2);
        
        // Attack 3
        Map<String, Object> params3 = new HashMap<>();
        params3.put("x", 0.9f);
        params3.put("y", 0.7f);
        PredictiveActionSystem.GameAction action3 = 
                new PredictiveActionSystem.GameAction(
                        PredictiveActionSystem.ActionType.TAP, 
                        params3, 1.0f, 0.9f);
        sequence.addAction(action3);
        
        // Set timing for precise execution
        sequence.setTimingOffsets(new long[] {0, 300, 600});
        
        // Set high priority
        sequence.setPriority(0.9f);
        
        // Make non-interruptible
        sequence.setInterruptible(false);
        
        // Set game context
        sequence.setGameContext("combat");
    }
    
    /**
     * Create defense combo
     */
    private void createDefenseCombo(ActionSequence sequence) {
        // Block
        Map<String, Object> params1 = new HashMap<>();
        params1.put("x", 0.2f);
        params1.put("y", 0.8f);
        PredictiveActionSystem.GameAction action1 = 
                new PredictiveActionSystem.GameAction(
                        PredictiveActionSystem.ActionType.TAP, 
                        params1, 1.0f, 0.9f);
        sequence.addAction(action1);
        
        // Dodge
        Map<String, Object> params2 = new HashMap<>();
        params2.put("startX", 0.5f);
        params2.put("startY", 0.5f);
        params2.put("endX", 0.7f);
        params2.put("endY", 0.5f);
        params2.put("duration", 100L);
        PredictiveActionSystem.GameAction action2 = 
                new PredictiveActionSystem.GameAction(
                        PredictiveActionSystem.ActionType.SWIPE, 
                        params2, 1.0f, 0.9f);
        sequence.addAction(action2);
        
        // Counter-attack
        Map<String, Object> params3 = new HashMap<>();
        params3.put("x", 0.8f);
        params3.put("y", 0.7f);
        PredictiveActionSystem.GameAction action3 = 
                new PredictiveActionSystem.GameAction(
                        PredictiveActionSystem.ActionType.TAP, 
                        params3, 1.0f, 0.9f);
        sequence.addAction(action3);
        
        // Set timing for precise execution
        sequence.setTimingOffsets(new long[] {0, 200, 500});
        
        // Set high priority
        sequence.setPriority(1.0f);
        
        // Set game context
        sequence.setGameContext("combat");
    }
    
    /**
     * Create movement combo
     */
    private void createMovementCombo(ActionSequence sequence) {
        // Dodge left
        Map<String, Object> params1 = new HashMap<>();
        params1.put("startX", 0.5f);
        params1.put("startY", 0.5f);
        params1.put("endX", 0.3f);
        params1.put("endY", 0.5f);
        params1.put("duration", 100L);
        PredictiveActionSystem.GameAction action1 = 
                new PredictiveActionSystem.GameAction(
                        PredictiveActionSystem.ActionType.SWIPE, 
                        params1, 1.0f, 0.9f);
        sequence.addAction(action1);
        
        // Jump
        Map<String, Object> params2 = new HashMap<>();
        params2.put("startX", 0.5f);
        params2.put("startY", 0.6f);
        params2.put("endX", 0.5f);
        params2.put("endY", 0.4f);
        params2.put("duration", 100L);
        PredictiveActionSystem.GameAction action2 = 
                new PredictiveActionSystem.GameAction(
                        PredictiveActionSystem.ActionType.SWIPE, 
                        params2, 1.0f, 0.9f);
        sequence.addAction(action2);
        
        // Dodge right
        Map<String, Object> params3 = new HashMap<>();
        params3.put("startX", 0.5f);
        params3.put("startY", 0.5f);
        params3.put("endX", 0.7f);
        params3.put("endY", 0.5f);
        params3.put("duration", 100L);
        PredictiveActionSystem.GameAction action3 = 
                new PredictiveActionSystem.GameAction(
                        PredictiveActionSystem.ActionType.SWIPE, 
                        params3, 1.0f, 0.9f);
        sequence.addAction(action3);
        
        // Set timing for precise execution
        sequence.setTimingOffsets(new long[] {0, 250, 500});
        
        // Set medium priority
        sequence.setPriority(0.7f);
        
        // Set game context
        sequence.setGameContext("movement");
    }
    
    /**
     * Create special combo
     */
    private void createSpecialCombo(ActionSequence sequence) {
        // Long press special button
        Map<String, Object> params1 = new HashMap<>();
        params1.put("x", 0.9f);
        params1.put("y", 0.9f);
        params1.put("duration", 500L);
        PredictiveActionSystem.GameAction action1 = 
                new PredictiveActionSystem.GameAction(
                        PredictiveActionSystem.ActionType.LONG_PRESS, 
                        params1, 1.0f, 0.9f);
        sequence.addAction(action1);
        
        // Swipe pattern
        Map<String, Object> params2 = new HashMap<>();
        params2.put("startX", 0.3f);
        params2.put("startY", 0.5f);
        params2.put("endX", 0.7f);
        params2.put("endY", 0.5f);
        params2.put("duration", 150L);
        PredictiveActionSystem.GameAction action2 = 
                new PredictiveActionSystem.GameAction(
                        PredictiveActionSystem.ActionType.SWIPE, 
                        params2, 1.0f, 0.9f);
        sequence.addAction(action2);
        
        // Tap to confirm
        Map<String, Object> params3 = new HashMap<>();
        params3.put("x", 0.5f);
        params3.put("y", 0.5f);
        PredictiveActionSystem.GameAction action3 = 
                new PredictiveActionSystem.GameAction(
                        PredictiveActionSystem.ActionType.TAP, 
                        params3, 1.0f, 0.9f);
        sequence.addAction(action3);
        
        // Set timing for precise execution
        sequence.setTimingOffsets(new long[] {0, 600, 800});
        
        // Set very high priority
        sequence.setPriority(1.0f);
        
        // Make non-interruptible
        sequence.setInterruptible(false);
        
        // Set game context
        sequence.setGameContext("special");
    }
    
    /**
     * Get active sequence
     */
    public ActionSequence getActiveSequence() {
        return activeSequence;
    }
    
    /**
     * Get sequence by ID
     */
    public ActionSequence getSequence(String sequenceId) {
        return sequences.get(sequenceId);
    }
    
    /**
     * Get all sequences
     */
    public List<ActionSequence> getAllSequences() {
        return new ArrayList<>(sequences.values());
    }
    
    /**
     * Get sequences for a specific game context
     */
    public List<ActionSequence> getSequencesForContext(String gameContext) {
        List<ActionSequence> result = new ArrayList<>();
        for (ActionSequence sequence : sequences.values()) {
            if (sequence.getGameContext().equals(gameContext)) {
                result.add(sequence);
            }
        }
        return result;
    }
    
    /**
     * Get queue size
     */
    public int getQueueSize() {
        synchronized (actionQueue) {
            return actionQueue.size();
        }
    }
    
    /**
     * Get statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("running", running.get());
        stats.put("queueSize", getQueueSize());
        stats.put("sequenceCount", sequences.size());
        stats.put("hasActiveSequence", activeSequence != null);
        
        if (activeSequence != null) {
            Map<String, Object> activeSequenceInfo = new HashMap<>();
            activeSequenceInfo.put("id", activeSequence.getId());
            activeSequenceInfo.put("name", activeSequence.getName());
            activeSequenceInfo.put("position", activeSequence.getCurrentPosition());
            activeSequenceInfo.put("totalActions", activeSequence.getActions().size());
            activeSequenceInfo.put("completed", activeSequence.isCompleted());
            activeSequenceInfo.put("aborted", activeSequence.isAborted());
            
            stats.put("activeSequence", activeSequenceInfo);
        }
        
        return stats;
    }
    
    /**
     * Release resources
     */
    public void release() {
        // Stop sequencer
        stop();
        
        // Shut down executors
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Clear data
        sequences.clear();
        callbacks.clear();
        
        Log.i(TAG, "Advanced action sequencer released");
    }
}
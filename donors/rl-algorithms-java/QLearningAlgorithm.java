package com.aiassistant.core.ai.algorithms;

import android.content.Context;
import android.util.Log;

import com.aiassistant.utils.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Q-Learning algorithm for reinforcement learning
 */
public class QLearningAlgorithm {
    private static final String TAG = "QLearningAlgorithm";
    
    // Context
    private final Context context;
    
    // Algorithm state
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Random random = new Random();
    
    // Q-Learning parameters
    private float learningRate = 0.1f;
    private float discountFactor = 0.9f;
    private float explorationRate = 0.2f;
    
    // Q-Table: game -> state -> action -> q-value
    private final Map<String, Map<String, float[]>> qTables = new ConcurrentHashMap<>();
    
    // State tracking: game -> last state
    private final Map<String, String> lastStates = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastActions = new ConcurrentHashMap<>();
    
    /**
     * Constructor
     * 
     * @param context Application context
     */
    public QLearningAlgorithm(Context context) {
        this.context = context;
        
        Log.d(TAG, "QLearningAlgorithm initialized");
    }
    
    /**
     * Start the algorithm
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            Log.d(TAG, "QLearningAlgorithm started");
        }
    }
    
    /**
     * Stop the algorithm
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            // Save Q-tables
            for (String gameId : qTables.keySet()) {
                saveQTable(gameId);
            }
            
            Log.d(TAG, "QLearningAlgorithm stopped");
        }
    }
    
    /**
     * Calculate the best action for a state
     * 
     * @param gameId Game ID
     * @param features Feature vector as state
     * @return The best action index
     */
    public int getBestAction(String gameId, float[] features) {
        if (!isRunning.get() || gameId == null || features == null || features.length == 0) {
            return -1;
        }
        
        // Convert features to state string
        String state = featuresToState(features);
        
        // Get Q-table for game
        Map<String, float[]> qTable = getQTable(gameId);
        
        // Get Q-values for state
        float[] qValues = qTable.get(state);
        if (qValues == null) {
            // Initialize Q-values for new state
            qValues = new float[10]; // Assume 10 possible actions
            qTable.put(state, qValues);
        }
        
        // Exploration vs exploitation
        if (random.nextFloat() < explorationRate) {
            // Exploration - random action
            int action = random.nextInt(qValues.length);
            Log.d(TAG, "QLearning exploration: chosen random action " + action);
            
            // Save last state and action
            lastStates.put(gameId, state);
            lastActions.put(gameId, action);
            
            return action;
        } else {
            // Exploitation - best action
            int bestAction = 0;
            float bestQValue = qValues[0];
            
            for (int i = 1; i < qValues.length; i++) {
                if (qValues[i] > bestQValue) {
                    bestQValue = qValues[i];
                    bestAction = i;
                }
            }
            
            Log.d(TAG, "QLearning exploitation: chosen best action " + bestAction);
            
            // Save last state and action
            lastStates.put(gameId, state);
            lastActions.put(gameId, bestAction);
            
            return bestAction;
        }
    }
    
    /**
     * Calculate confidence for an action
     * 
     * @param gameId Game ID
     * @param features Feature vector as state
     * @param actionIndex Action index
     * @return Confidence value (0.0-1.0)
     */
    public float calculateConfidence(String gameId, float[] features, int actionIndex) {
        if (gameId == null || features == null || features.length == 0) {
            return 0.0f;
        }
        
        // Convert features to state string
        String state = featuresToState(features);
        
        // Get Q-table for game
        Map<String, float[]> qTable = getQTable(gameId);
        
        // Get Q-values for state
        float[] qValues = qTable.get(state);
        if (qValues == null || actionIndex < 0 || actionIndex >= qValues.length) {
            return 0.0f;
        }
        
        // Find max Q-value
        float maxQValue = Float.NEGATIVE_INFINITY;
        for (float qValue : qValues) {
            if (qValue > maxQValue) {
                maxQValue = qValue;
            }
        }
        
        // Find min Q-value
        float minQValue = Float.POSITIVE_INFINITY;
        for (float qValue : qValues) {
            if (qValue < minQValue) {
                minQValue = qValue;
            }
        }
        
        // Normalize Q-value to [0, 1]
        float range = maxQValue - minQValue;
        if (range <= 0.0001f) {
            // All Q-values are approximately equal
            return 0.5f;
        }
        
        return (qValues[actionIndex] - minQValue) / range;
    }
    
    /**
     * Provide feedback for the last action
     * 
     * @param gameId Game ID
     * @param actionIndex Action index
     * @param feedback Feedback (-1.0 to 1.0)
     */
    public void provideFeedback(String gameId, int actionIndex, float feedback) {
        if (!isRunning.get() || gameId == null) {
            return;
        }
        
        // Convert feedback to reward
        float reward = feedback;
        
        // Get last state and action
        String lastState = lastStates.get(gameId);
        Integer lastAction = lastActions.get(gameId);
        
        // Check if we have a last state and action
        if (lastState == null || lastAction == null) {
            return;
        }
        
        // Get Q-table for game
        Map<String, float[]> qTable = getQTable(gameId);
        
        // Get Q-values for last state
        float[] qValues = qTable.get(lastState);
        if (qValues == null) {
            return;
        }
        
        // Update Q-value for last action
        // Q(s,a) = Q(s,a) + learningRate * (reward + discountFactor * max(Q(s')) - Q(s,a))
        
        // Since we don't have s' (next state), we use the Q-value update formula without the next state:
        // Q(s,a) = Q(s,a) + learningRate * (reward - Q(s,a))
        qValues[lastAction] = qValues[lastAction] + learningRate * (reward - qValues[lastAction]);
        
        Log.d(TAG, "Updated Q-value for game " + gameId + ", state " + lastState + 
              ", action " + lastAction + ": " + qValues[lastAction]);
        
        // Save Q-table
        saveQTable(gameId);
    }
    
    /**
     * Reset game data
     * 
     * @param gameId Game ID
     */
    public void resetGameData(String gameId) {
        if (gameId == null) {
            return;
        }
        
        // Remove from in-memory maps
        qTables.remove(gameId);
        lastStates.remove(gameId);
        lastActions.remove(gameId);
        
        // Delete saved file
        File file = new File(context.getFilesDir(), "q_table_" + gameId + ".dat");
        if (file.exists()) {
            boolean deleted = file.delete();
            Log.d(TAG, "Reset Q-table for game " + gameId + ": " + (deleted ? "success" : "failed"));
        }
    }
    
    /**
     * Save Q-table for a game
     * 
     * @param gameId Game ID
     */
    private void saveQTable(String gameId) {
        if (gameId == null) {
            return;
        }
        
        Map<String, float[]> qTable = qTables.get(gameId);
        if (qTable == null) {
            return;
        }
        
        File file = new File(context.getFilesDir(), "q_table_" + gameId + ".dat");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(qTable);
            Log.d(TAG, "Saved Q-table for game " + gameId);
        } catch (IOException e) {
            Log.e(TAG, "Error saving Q-table for " + gameId, e);
        }
    }
    
    /**
     * Load Q-table for a game
     * 
     * @param gameId Game ID
     */
    @SuppressWarnings("unchecked")
    private void loadQTable(String gameId) {
        if (gameId == null) {
            return;
        }
        
        File file = new File(context.getFilesDir(), "q_table_" + gameId + ".dat");
        if (!file.exists()) {
            Log.d(TAG, "No saved Q-table found for game: " + gameId);
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Map<String, float[]> qTable = (Map<String, float[]>) ois.readObject();
            qTables.put(gameId, qTable);
            Log.d(TAG, "Loaded Q-table for game: " + gameId);
        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, "Error loading Q-table for " + gameId, e);
        }
    }
    
    /**
     * Get Q-table for a game
     * 
     * @param gameId Game ID
     * @return Q-table
     */
    private Map<String, float[]> getQTable(String gameId) {
        Map<String, float[]> qTable = qTables.get(gameId);
        if (qTable == null) {
            // Try to load from storage
            loadQTable(gameId);
            
            // Check again
            qTable = qTables.get(gameId);
            if (qTable == null) {
                // Create new Q-table
                qTable = new HashMap<>();
                qTables.put(gameId, qTable);
            }
        }
        
        return qTable;
    }
    
    /**
     * Convert feature vector to state string
     * 
     * @param features Feature vector
     * @return State string
     */
    private String featuresToState(float[] features) {
        // For simplicity, we just quantize the features to a limited number of buckets
        StringBuilder sb = new StringBuilder();
        
        for (float feature : features) {
            // Quantize to 4 levels (0, 1, 2, 3)
            int quantized = Math.min(3, Math.max(0, (int) (feature * 4)));
            sb.append(quantized);
        }
        
        return sb.toString();
    }
    
    /**
     * Set learning parameters
     * 
     * @param learningRate Learning rate
     * @param discountFactor Discount factor
     * @param explorationRate Exploration rate
     */
    public void setLearningParameters(float learningRate, float discountFactor, float explorationRate) {
        this.learningRate = learningRate;
        this.discountFactor = discountFactor;
        this.explorationRate = explorationRate;
        
        Log.d(TAG, "Updated learning parameters: learningRate=" + learningRate + 
              ", discountFactor=" + discountFactor + ", explorationRate=" + explorationRate);
    }
}

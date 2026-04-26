package com.aiassistant.core.ai.algorithms;

import android.util.Log;

import com.aiassistant.data.models.AIAction;
import com.aiassistant.data.models.GameState;
import com.aiassistant.utils.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * SARSA (State-Action-Reward-State-Action) algorithm implementation
 */
public class SARSAAlgorithm {
    private static final String TAG = "SARSAAlgorithm";
    
    private Map<String, Map<Integer, Float>> qTable;
    private float learningRate;
    private float discountFactor;
    private float epsilon;
    private int stateSize;
    private int actionSize;
    private Random random;
    
    /**
     * Constructor
     */
    public SARSAAlgorithm() {
        this.qTable = new HashMap<>();
        this.learningRate = 0.1f;
        this.discountFactor = 0.9f;
        this.epsilon = 0.1f;
        this.stateSize = Constants.FEATURE_VECTOR_SIZE;
        this.actionSize = 9;  // Default 3x3 grid
        this.random = new Random();
        
        Log.d(TAG, "SARSAAlgorithm created with state size: " + stateSize + ", action size: " + actionSize);
    }
    
    /**
     * Constructor with parameters
     */
    public SARSAAlgorithm(float learningRate, float discountFactor, float epsilon) {
        this();
        this.learningRate = learningRate;
        this.discountFactor = discountFactor;
        this.epsilon = epsilon;
        
        Log.d(TAG, "SARSAAlgorithm created with parameters: learningRate=" + learningRate + 
                  ", discountFactor=" + discountFactor + ", epsilon=" + epsilon);
    }
    
    /**
     * Initialize the algorithm
     * 
     * @return True if initialization successful
     */
    public boolean initialize() {
        Log.d(TAG, "Initializing SARSAAlgorithm");
        
        // Nothing to initialize for tabular SARSA
        return true;
    }
    
    /**
     * Get the best action for the current state
     * 
     * @param state Current state
     * @return Best action
     */
    public AIAction getBestAction(GameState state) {
        if (state == null) {
            Log.e(TAG, "Invalid state provided to getBestAction");
            return createRandomAction();
        }
        
        // Get state key
        float[] stateFeatures = state.toFeatureVector();
        String stateKey = getStateKey(stateFeatures);
        
        // Epsilon-greedy policy
        if (random.nextFloat() < epsilon) {
            // Exploration: Random action
            AIAction action = createRandomAction();
            action.setConfidence(epsilon);
            return action;
        } else {
            // Exploitation: Best action from Q-table
            int actionIndex = getBestActionIndex(stateKey);
            AIAction action = actionIndexToAction(actionIndex);
            action.setConfidence(1.0f - epsilon);
            return action;
        }
    }
    
    /**
     * Update the Q-table with a new experience
     * 
     * @param state Current state
     * @param action Action taken
     * @param reward Reward received
     * @param nextState Next state
     * @param nextAction Next action
     */
    public void update(GameState state, AIAction action, float reward, GameState nextState, AIAction nextAction) {
        if (state == null || action == null) {
            Log.e(TAG, "Invalid state or action provided to update");
            return;
        }
        
        try {
            // Get state keys and action indices
            String stateKey = getStateKey(state.toFeatureVector());
            int actionIndex = actionToActionIndex(action);
            
            // Get current Q-value
            float currentQ = getQValue(stateKey, actionIndex);
            
            // Calculate next Q-value
            float nextQ = 0.0f;
            if (nextState != null && nextAction != null) {
                String nextStateKey = getStateKey(nextState.toFeatureVector());
                int nextActionIndex = actionToActionIndex(nextAction);
                nextQ = getQValue(nextStateKey, nextActionIndex);
            }
            
            // SARSA update rule
            float updatedQ = currentQ + learningRate * (reward + discountFactor * nextQ - currentQ);
            
            // Update Q-table
            setQValue(stateKey, actionIndex, updatedQ);
            
            Log.d(TAG, "Updated Q-value: state=" + stateKey + ", action=" + actionIndex + 
                      ", reward=" + reward + ", currentQ=" + currentQ + ", updatedQ=" + updatedQ);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating Q-value: " + e.getMessage());
        }
    }
    
    /**
     * Update the Q-table with a new experience (simplified version)
     * 
     * @param state Current state
     * @param action Action taken
     * @param reward Reward received
     * @param nextState Next state
     * @param done Whether the episode is done
     */
    public void update(GameState state, AIAction action, float reward, GameState nextState, boolean done) {
        if (state == null || action == null) {
            Log.e(TAG, "Invalid state or action provided to update");
            return;
        }
        
        try {
            // Get state keys and action indices
            String stateKey = getStateKey(state.toFeatureVector());
            int actionIndex = actionToActionIndex(action);
            
            // Get current Q-value
            float currentQ = getQValue(stateKey, actionIndex);
            
            // Calculate next Q-value
            float nextQ = 0.0f;
            if (!done && nextState != null) {
                String nextStateKey = getStateKey(nextState.toFeatureVector());
                
                // For this simplified update, we use the best next action
                int nextActionIndex = getBestActionIndex(nextStateKey);
                nextQ = getQValue(nextStateKey, nextActionIndex);
            }
            
            // SARSA update rule
            float updatedQ = currentQ + learningRate * (reward + discountFactor * nextQ - currentQ);
            
            // Update Q-table
            setQValue(stateKey, actionIndex, updatedQ);
            
            Log.d(TAG, "Updated Q-value: state=" + stateKey + ", action=" + actionIndex + 
                      ", reward=" + reward + ", currentQ=" + currentQ + ", updatedQ=" + updatedQ);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating Q-value: " + e.getMessage());
        }
    }
    
    /**
     * Train the algorithm for a number of steps
     * 
     * @param steps Number of training steps
     * @return Whether training was successful
     */
    public boolean train(int steps) {
        Log.d(TAG, "Training SARSA for " + steps + " steps");
        
        // Tabular SARSA doesn't need explicit training steps beyond updates
        return true;
    }
    
    /**
     * Create a random action
     * 
     * @return Random action
     */
    private AIAction createRandomAction() {
        int screenWidth = 1080;  // Default width
        int screenHeight = 1920; // Default height
        
        int x = random.nextInt(screenWidth);
        int y = random.nextInt(screenHeight);
        
        AIAction action = AIAction.createTapAction(x, y);
        return action;
    }
    
    /**
     * Get a state key from features
     * 
     * @param features State features
     * @return State key
     */
    private String getStateKey(float[] features) {
        if (features == null || features.length == 0) {
            return "default";
        }
        
        // For simplicity, use first few features to create a key
        // In a real implementation, you would use a better state representation
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < Math.min(features.length, 5); i++) {
            key.append(Math.round(features[i] * 10) / 10.0f).append("_");
        }
        
        return key.toString();
    }
    
    /**
     * Get the Q-value for a state-action pair
     * 
     * @param stateKey State key
     * @param actionIndex Action index
     * @return Q-value
     */
    private float getQValue(String stateKey, int actionIndex) {
        // Ensure state exists in Q-table
        if (!qTable.containsKey(stateKey)) {
            qTable.put(stateKey, new HashMap<>());
        }
        
        // Get action map for state
        Map<Integer, Float> actionMap = qTable.get(stateKey);
        
        // Return Q-value or default value if not present
        return actionMap.getOrDefault(actionIndex, 0.0f);
    }
    
    /**
     * Set the Q-value for a state-action pair
     * 
     * @param stateKey State key
     * @param actionIndex Action index
     * @param value Q-value
     */
    private void setQValue(String stateKey, int actionIndex, float value) {
        // Ensure state exists in Q-table
        if (!qTable.containsKey(stateKey)) {
            qTable.put(stateKey, new HashMap<>());
        }
        
        // Get action map for state
        Map<Integer, Float> actionMap = qTable.get(stateKey);
        
        // Set Q-value
        actionMap.put(actionIndex, value);
    }
    
    /**
     * Get the best action index for a state
     * 
     * @param stateKey State key
     * @return Best action index
     */
    private int getBestActionIndex(String stateKey) {
        // Ensure state exists in Q-table
        if (!qTable.containsKey(stateKey)) {
            qTable.put(stateKey, new HashMap<>());
        }
        
        // Get action map for state
        Map<Integer, Float> actionMap = qTable.get(stateKey);
        
        // If no actions have been tried, return random action
        if (actionMap.isEmpty()) {
            return random.nextInt(actionSize);
        }
        
        // Find action with highest Q-value
        int bestAction = -1;
        float bestValue = Float.NEGATIVE_INFINITY;
        
        for (Map.Entry<Integer, Float> entry : actionMap.entrySet()) {
            if (entry.getValue() > bestValue) {
                bestValue = entry.getValue();
                bestAction = entry.getKey();
            }
        }
        
        // If no best action found, return random action
        if (bestAction == -1) {
            return random.nextInt(actionSize);
        }
        
        return bestAction;
    }
    
    /**
     * Convert action to action index
     * 
     * @param action Action
     * @return Action index
     */
    private int actionToActionIndex(AIAction action) {
        if (ACTION_TAP.equals(action.getActionType())) {
            try {
                int x = Integer.parseInt(action.getParameter("x"));
                int y = Integer.parseInt(action.getParameter("y"));
                
                int screenWidth = 1080;  // Default width
                int screenHeight = 1920; // Default height
                
                // Map x,y coordinates to an action index
                // For example, divide the screen into a grid
                int gridSize = (int) Math.sqrt(actionSize);
                int gridWidth = screenWidth / gridSize;
                int gridHeight = screenHeight / gridSize;
                
                int gridX = Math.min(x / gridWidth, gridSize - 1);
                int gridY = Math.min(y / gridHeight, gridSize - 1);
                
                return gridY * gridSize + gridX;
                
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing action coordinates: " + e.getMessage());
                return 0;
            }
        }
        
        return 0;  // Default action index
    }
    
    /**
     * Action types
     */
    private static final String ACTION_TAP = "TAP";
    
    /**
     * Convert action index to action
     * 
     * @param actionIndex Action index
     * @return Action
     */
    private AIAction actionIndexToAction(int actionIndex) {
        int screenWidth = 1080;  // Default width
        int screenHeight = 1920; // Default height
        
        // Map action index to x,y coordinates
        // For example, divide the screen into a grid
        int gridSize = (int) Math.sqrt(actionSize);
        int gridWidth = screenWidth / gridSize;
        int gridHeight = screenHeight / gridSize;
        
        int gridX = actionIndex % gridSize;
        int gridY = actionIndex / gridSize;
        
        int x = gridX * gridWidth + gridWidth / 2;
        int y = gridY * gridHeight + gridHeight / 2;
        
        return AIAction.createTapAction(x, y);
    }
    
    /**
     * Get the size of the Q-table
     * 
     * @return Number of state-action pairs in the Q-table
     */
    public int getQTableSize() {
        int size = 0;
        for (Map<Integer, Float> actionMap : qTable.values()) {
            size += actionMap.size();
        }
        return size;
    }
    
    /**
     * Get the number of unique states in the Q-table
     * 
     * @return Number of states
     */
    public int getNumStates() {
        return qTable.size();
    }
    
    /**
     * Get the learning rate
     * 
     * @return Learning rate
     */
    public float getLearningRate() {
        return learningRate;
    }
    
    /**
     * Set the learning rate
     * 
     * @param learningRate Learning rate
     */
    public void setLearningRate(float learningRate) {
        this.learningRate = learningRate;
    }
    
    /**
     * Get the discount factor
     * 
     * @return Discount factor
     */
    public float getDiscountFactor() {
        return discountFactor;
    }
    
    /**
     * Set the discount factor
     * 
     * @param discountFactor Discount factor
     */
    public void setDiscountFactor(float discountFactor) {
        this.discountFactor = discountFactor;
    }
    
    /**
     * Get the exploration rate (epsilon)
     * 
     * @return Epsilon
     */
    public float getEpsilon() {
        return epsilon;
    }
    
    /**
     * Set the exploration rate (epsilon)
     * 
     * @param epsilon Epsilon
     */
    public void setEpsilon(float epsilon) {
        this.epsilon = epsilon;
    }
    
    /**
     * Save the model
     * 
     * @param path Path to save the model to
     * @return Whether saving was successful
     */
    public boolean save(String path) {
        Log.d(TAG, "Saving model to " + path);
        
        // In a full implementation, this would save the Q-table to a file
        
        return true;
    }
    
    /**
     * Load the model
     * 
     * @param path Path to load the model from
     * @return Whether loading was successful
     */
    public boolean load(String path) {
        Log.d(TAG, "Loading model from " + path);
        
        // In a full implementation, this would load the Q-table from a file
        
        return true;
    }
    
    /**
     * Clear the Q-table
     */
    public void reset() {
        qTable.clear();
        Log.d(TAG, "Q-table reset");
    }
}

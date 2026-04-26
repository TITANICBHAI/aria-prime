package com.aiassistant.core.ai.algorithms;

import android.content.Context;
import android.util.Log;

import com.aiassistant.data.models.AIAction;
import com.aiassistant.data.models.AIActionReward;
import com.aiassistant.data.models.GameState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Meta-learning algorithm that selects the best algorithm based on performance
 */
public class MetaLearningAlgorithm extends ReinforcementLearningAlgorithm {
    private static final String TAG = "MetaLearning";
    
    // List of available algorithms
    private List<RLAlgorithm> algorithms;
    
    // Performance tracking for each algorithm
    private Map<RLAlgorithm, Float> algorithmPerformance;
    
    // Number of times each algorithm has been used
    private Map<RLAlgorithm, Integer> algorithmUsage;
    
    // Currently selected algorithm
    private RLAlgorithm currentAlgorithm;
    
    // History of algorithm selections
    private List<AlgorithmSelection> selectionHistory;
    
    // Exploration parameter for algorithm selection
    private float metaExplorationRate;
    
    /**
     * Algorithm selection history entry
     */
    private static class AlgorithmSelection {
        RLAlgorithm algorithm;
        GameState state;
        long timestamp;
        float reward;
        
        public AlgorithmSelection(RLAlgorithm algorithm, GameState state) {
            this.algorithm = algorithm;
            this.state = state;
            this.timestamp = System.currentTimeMillis();
            this.reward = 0.0f;
        }
    }
    
    /**
     * Constructor
     */
    public MetaLearningAlgorithm(Context context) {
        super(context);
        this.algorithms = new ArrayList<>();
        this.algorithmPerformance = new HashMap<>();
        this.algorithmUsage = new HashMap<>();
        this.selectionHistory = new ArrayList<>();
        this.metaExplorationRate = 0.2f;
    }
    
    /**
     * Add an algorithm to the meta-learner
     */
    public void addAlgorithm(RLAlgorithm algorithm) {
        if (algorithm != null && !algorithms.contains(algorithm)) {
            algorithms.add(algorithm);
            algorithmPerformance.put(algorithm, 0.0f);
            algorithmUsage.put(algorithm, 0);
            Log.d(TAG, "Added algorithm: " + algorithm.getName());
        }
    }
    
    /**
     * Remove an algorithm from the meta-learner
     */
    public void removeAlgorithm(RLAlgorithm algorithm) {
        if (algorithm != null && algorithms.contains(algorithm)) {
            algorithms.remove(algorithm);
            algorithmPerformance.remove(algorithm);
            algorithmUsage.remove(algorithm);
            
            // If this was the current algorithm, select a new one
            if (algorithm == currentAlgorithm) {
                currentAlgorithm = null;
            }
            
            Log.d(TAG, "Removed algorithm: " + algorithm.getName());
        }
    }
    
    /**
     * Select the best algorithm for the current state
     */
    private RLAlgorithm selectAlgorithm(GameState state) {
        if (algorithms.isEmpty()) {
            return null;
        }
        
        // If exploration, choose a random algorithm
        if (random.nextFloat() < metaExplorationRate) {
            int randomIndex = random.nextInt(algorithms.size());
            RLAlgorithm selected = algorithms.get(randomIndex);
            Log.d(TAG, "Exploring with random algorithm: " + selected.getName());
            return selected;
        }
        
        // Find the best performing algorithm
        float bestPerformance = Float.NEGATIVE_INFINITY;
        int bestAlgorithm = 0;
        
        for (int i = 0; i < algorithms.size(); i++) {
            RLAlgorithm algorithm = algorithms.get(i);
            float performance = algorithmPerformance.get(algorithm);
            
            // Add bonus for less used algorithms to encourage exploration
            int usageCount = algorithmUsage.get(algorithm);
            float explorationBonus = 1.0f / (usageCount + 1);
            
            float adjustedPerformance = performance + (explorationBonus * metaExplorationRate);
            
            if (adjustedPerformance > bestPerformance) {
                bestPerformance = adjustedPerformance;
                bestAlgorithm = i;
            }
        }
        
        Log.d(TAG, "Selected algorithm: " + algorithms.get(bestAlgorithm).getName() + 
                " with performance: " + algorithmPerformance.get(algorithms.get(bestAlgorithm)));
        
        return algorithms.get(bestAlgorithm);
    }
    
    @Override
    public AIAction chooseAction(GameState state) {
        // Select the algorithm if not already selected
        if (currentAlgorithm == null) {
            currentAlgorithm = selectAlgorithm(state);
            
            if (currentAlgorithm != null) {
                // Record the selection
                selectionHistory.add(new AlgorithmSelection(currentAlgorithm, state));
                
                // Update usage count
                algorithmUsage.put(currentAlgorithm, algorithmUsage.get(currentAlgorithm) + 1);
            }
        }
        
        // If no algorithm could be selected, choose a random action
        if (currentAlgorithm == null) {
            Log.e(TAG, "No algorithm available for selection");
            return createRandomAction(state);
        }
        
        // Let the selected algorithm choose the action
        return currentAlgorithm.chooseAction(state);
    }
    
    /**
     * Create a random action
     */
    private AIAction createRandomAction(GameState state) {
        // Find a random algorithm to use for action creation
        int numAlgorithms = algorithms.size();
        if (numAlgorithms == 0) {
            // No algorithms available, create a default tap action
            return AIAction.createTapAction(100, 100);
        }
        
        // Get a random algorithm
        for (int i = 0; i < numAlgorithms; i++) {
            RLAlgorithm algorithm = null;
            for (RLAlgorithm alg : algorithms) {
                if (alg.getName().hashCode() % numAlgorithms == i) {
                    algorithm = alg;
                    break;
                }
            }
            
            if (algorithm != null) {
                return algorithm.chooseAction(state);
            }
        }
        
        // Fallback
        return AIAction.createTapAction(100, 100);
    }
    
    @Override
    public List<AIAction> chooseActions(GameState state, int count) {
        // Select the algorithm if not already selected
        if (currentAlgorithm == null) {
            currentAlgorithm = selectAlgorithm(state);
            
            if (currentAlgorithm != null) {
                // Record the selection
                selectionHistory.add(new AlgorithmSelection(currentAlgorithm, state));
                
                // Update usage count
                algorithmUsage.put(currentAlgorithm, algorithmUsage.get(currentAlgorithm) + 1);
            }
        }
        
        // If no algorithm could be selected, choose random actions
        if (currentAlgorithm == null) {
            Log.e(TAG, "No algorithm available for selection");
            List<AIAction> randomActions = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                randomActions.add(createRandomAction(state));
            }
            return randomActions;
        }
        
        // Let the selected algorithm choose the actions
        return currentAlgorithm.chooseActions(state, count);
    }
    
    @Override
    public void update(GameState state, AIAction action, GameState nextState, AIActionReward reward) {
        if (currentAlgorithm == null || reward == null) {
            return;
        }
        
        // Update the current algorithm
        currentAlgorithm.update(state, action, nextState, reward);
        
        // Update the performance of the algorithm
        float currentPerformance = algorithmPerformance.getOrDefault(currentAlgorithm, 0.0f);
        float rewardValue = reward.getValue();
        
        // Update using exponential moving average
        float updatedPerformance = (currentPerformance * 0.9f) + (rewardValue * 0.1f);
        algorithmPerformance.put(currentAlgorithm, updatedPerformance);
        
        Log.d(TAG, "Updated performance for algorithm: " + currentAlgorithm.getName() + 
                " to " + updatedPerformance);
        
        // Update the most recent selection history entry
        if (!selectionHistory.isEmpty()) {
            AlgorithmSelection lastSelection = selectionHistory.get(selectionHistory.size() - 1);
            if (lastSelection.algorithm == currentAlgorithm) {
                lastSelection.reward += rewardValue;
            }
        }
        
        // Reset current algorithm periodically to force re-selection
        if (random.nextFloat() < 0.1f) {
            currentAlgorithm = null;
        }
    }
    
    @Override
    public boolean saveModel(String path) {
        // Save meta-learning state
        Log.d(TAG, "Saving meta-learning state");
        
        // Save individual algorithms
        for (RLAlgorithm algorithm : algorithms) {
            algorithm.saveModel(path + "/" + algorithm.getName());
        }
        
        return true;
    }
    
    @Override
    public boolean loadModel(String path) {
        // Load meta-learning state
        Log.d(TAG, "Loading meta-learning state");
        
        // Load individual algorithms
        for (RLAlgorithm algorithm : algorithms) {
            algorithm.loadModel(path + "/" + algorithm.getName());
        }
        
        return true;
    }
    
    @Override
    public float train(int numEpisodes, int maxStepsPerEpisode) {
        // Train each algorithm
        float totalReward = 0.0f;
        
        for (RLAlgorithm algorithm : algorithms) {
            float reward = algorithm.train(numEpisodes, maxStepsPerEpisode);
            totalReward += reward;
            
            // Update performance
            algorithmPerformance.put(algorithm, reward);
        }
        
        return totalReward / Math.max(1, algorithms.size());
    }
    
    @Override
    public void reset() {
        // Reset meta-learning state
        currentAlgorithm = null;
        selectionHistory.clear();
        
        // Reset performance tracking
        for (RLAlgorithm algorithm : algorithms) {
            algorithmPerformance.put(algorithm, 0.0f);
            algorithmUsage.put(algorithm, 0);
            
            // Reset each algorithm
            algorithm.reset();
        }
    }
    
    /**
     * Set meta-exploration rate
     */
    public void setMetaExplorationRate(float rate) {
        this.metaExplorationRate = Math.max(0.0f, Math.min(1.0f, rate));
    }
    
    /**
     * Get meta-exploration rate
     */
    public float getMetaExplorationRate() {
        return metaExplorationRate;
    }
    
    /**
     * Get performance of all algorithms
     */
    public Map<RLAlgorithm, Float> getAlgorithmPerformance() {
        return new HashMap<>(algorithmPerformance);
    }
    
    /**
     * Get usage count of all algorithms
     */
    public Map<RLAlgorithm, Integer> getAlgorithmUsage() {
        return new HashMap<>(algorithmUsage);
    }
    
    /**
     * Get current algorithm
     */
    public RLAlgorithm getCurrentAlgorithm() {
        return currentAlgorithm;
    }
}

package com.aiassistant.core.ai.algorithms;

import com.aiassistant.data.models.AIAction;
import com.aiassistant.data.models.AIActionReward;
import com.aiassistant.data.models.GameState;

import java.util.List;

/**
 * Interface for reinforcement learning algorithms
 */
public interface RLAlgorithm {
    
    /**
     * Choose the best action for the current state
     * 
     * @param state The current game state
     * @return The chosen action
     */
    AIAction chooseAction(GameState state);
    
    /**
     * Choose the best actions for the current state
     * 
     * @param state The current game state
     * @param count The number of actions to choose
     * @return The chosen actions with descending preference
     */
    List<AIAction> chooseActions(GameState state, int count);
    
    /**
     * Update the model with reward information
     * 
     * @param state The state before the action
     * @param action The action taken
     * @param nextState The state after the action
     * @param reward The reward received
     */
    void update(GameState state, AIAction action, GameState nextState, AIActionReward reward);
    
    /**
     * Save the model to storage
     * 
     * @param path The path to save to
     * @return Whether the save was successful
     */
    boolean saveModel(String path);
    
    /**
     * Load the model from storage
     * 
     * @param path The path to load from
     * @return Whether the load was successful
     */
    boolean loadModel(String path);
    
    /**
     * Train the model
     * 
     * @param numEpisodes The number of episodes to train for
     * @param maxStepsPerEpisode The maximum number of steps per episode
     * @return The average reward per episode
     */
    float train(int numEpisodes, int maxStepsPerEpisode);
    
    /**
     * Reset the model
     */
    void reset();
    
    /**
     * Get the name of the algorithm
     * 
     * @return The algorithm name
     */
    String getName();
    
    /**
     * Set the learning rate
     * 
     * @param learningRate The learning rate
     */
    void setLearningRate(float learningRate);
    
    /**
     * Get the learning rate
     * 
     * @return The learning rate
     */
    float getLearningRate();
    
    /**
     * Set the discount factor
     * 
     * @param discountFactor The discount factor
     */
    void setDiscountFactor(float discountFactor);
    
    /**
     * Get the discount factor
     * 
     * @return The discount factor
     */
    float getDiscountFactor();
    
    /**
     * Set the exploration rate
     * 
     * @param explorationRate The exploration rate
     */
    void setExplorationRate(float explorationRate);
    
    /**
     * Get the exploration rate
     * 
     * @return The exploration rate
     */
    float getExplorationRate();
}

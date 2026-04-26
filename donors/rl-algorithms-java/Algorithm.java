package com.aiassistant.core.ai.algorithms;

/**
 * Enum for reinforcement learning algorithms
 */
public enum Algorithm {
    Q_LEARNING("Q-Learning"),
    SARSA("State-Action-Reward-State-Action"),
    DQN("Deep Q-Network"),
    PPO("Proximal Policy Optimization"),
    A3C("Asynchronous Advantage Actor-Critic"),
    TD3("Twin Delayed DDPG"),
    SAC("Soft Actor-Critic");
    
    private final String description;
    
    Algorithm(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

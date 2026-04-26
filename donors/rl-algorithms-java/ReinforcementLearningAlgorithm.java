package com.aiassistant.core.ai.algorithms;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;

import com.aiassistant.core.ai.detection.DetectedEnemy;
import com.aiassistant.data.models.AIAction;
import com.aiassistant.data.models.AIActionReward;
import com.aiassistant.data.models.GameState;
import com.aiassistant.data.models.UIElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Base class for reinforcement learning algorithms
 */
public abstract class ReinforcementLearningAlgorithm implements RLAlgorithm {
    
    private static final String TAG = "RLAlgorithm";
    
    protected float learningRate;
    protected float discountFactor;
    protected float explorationRate;
    
    protected final Context context;
    protected final Random random;
    
    /**
     * Constructor
     * 
     * @param context The application context
     */
    public ReinforcementLearningAlgorithm(Context context) {
        this.context = context.getApplicationContext();
        this.learningRate = 0.01f;
        this.discountFactor = 0.95f;
        this.explorationRate = 0.1f;
        this.random = new Random();
    }
    
    /**
     * Generate action mask based on available UI elements
     * 
     * @param gameState The game state
     * @return The action mask (1 for valid actions, 0 for invalid)
     */
    protected float[] generateActionMask(GameState gameState) {
        if (gameState == null) {
            return null;
        }
        
        // Default action space - all actions valid
        int actionSpaceSize = getActionSpaceSize();
        float[] mask = new float[actionSpaceSize];
        for (int i = 0; i < actionSpaceSize; i++) {
            mask[i] = 1.0f;
        }
        
        // If no UI elements, all actions remain valid
        List<UIElement> uiElements = gameState.getUiElements();
        if (uiElements == null || uiElements.isEmpty()) {
            return mask;
        }
        
        // Check each action for validity
        for (int i = 0; i < actionSpaceSize; i++) {
            AIAction action = mapIndexToAction(i, gameState);
            if (action != null) {
                // Check if action targets a valid UI element
                boolean isValid = true;
                
                if (AIAction.ACTION_TAP.equals(action.getActionType())) {
                    // For tap actions, check if tap position overlaps with a clickable UI element
                    boolean hitsClickableElement = false;
                    
                    String xStr = action.getParameter("x");
                    String yStr = action.getParameter("y");
                    
                    if (xStr != null && yStr != null) {
                        try {
                            int x = Integer.parseInt(xStr);
                            int y = Integer.parseInt(yStr);
                            
                            for (UIElement element : uiElements) {
                                if (element.containsPoint(x, y) && element.isInteractive()) {
                                    hitsClickableElement = true;
                                    break;
                                }
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Error parsing coordinates", e);
                        }
                    }
                    
                    // If no clickable element hit and this is a specific tap action, mark as invalid
                    if (!hitsClickableElement && i >= 4) { // Skip basic directional actions
                        isValid = false;
                    }
                }
                
                mask[i] = isValid ? 1.0f : 0.0f;
            }
        }
        
        return mask;
    }
    
    /**
     * Map an action index to an AI action
     * 
     * @param actionIndex The action index
     * @param gameState The game state
     * @return The AI action
     */
    protected AIAction mapIndexToAction(int actionIndex, GameState gameState) {
        if (gameState == null) {
            return null;
        }
        
        int screenWidth = gameState.getScreenWidth();
        int screenHeight = gameState.getScreenHeight();
        
        // Simple directional grid of tap and swipe actions
        switch (actionIndex % 12) {
            case 0: // Center tap
                return AIAction.createTapAction(screenWidth / 2, screenHeight / 2);
                
            case 1: // Top tap
                return AIAction.createTapAction(screenWidth / 2, screenHeight / 4);
                
            case 2: // Right tap
                return AIAction.createTapAction(screenWidth * 3 / 4, screenHeight / 2);
                
            case 3: // Bottom tap
                return AIAction.createTapAction(screenWidth / 2, screenHeight * 3 / 4);
                
            case 4: // Left tap
                return AIAction.createTapAction(screenWidth / 4, screenHeight / 2);
                
            case 5: // Swipe up
                return AIAction.createSwipeAction(screenWidth / 2, screenHeight * 3 / 4, 
                                    screenWidth / 2, screenHeight / 4, 300);
                
            case 6: // Swipe right
                return AIAction.createSwipeAction(screenWidth / 4, screenHeight / 2, 
                                    screenWidth * 3 / 4, screenHeight / 2, 300);
                
            case 7: // Swipe down
                return AIAction.createSwipeAction(screenWidth / 2, screenHeight / 4, 
                                    screenWidth / 2, screenHeight * 3 / 4, 300);
                
            case 8: // Swipe left
                return AIAction.createSwipeAction(screenWidth * 3 / 4, screenHeight / 2, 
                                    screenWidth / 4, screenHeight / 2, 300);
                
            case 9: // Long press center
                return AIAction.createLongPressAction(screenWidth / 2, screenHeight / 2, 500);
                
            case 10: // Back action
                return AIAction.createBackAction();
                
            case 11: // Tap UI element if available
                List<UIElement> elements = gameState.getUiElements();
                if (elements != null && !elements.isEmpty()) {
                    // Find clickable elements
                    List<UIElement> clickableElements = new ArrayList<>();
                    for (UIElement element : elements) {
                        if (element.isInteractive()) {
                            clickableElements.add(element);
                        }
                    }
                    
                    if (!clickableElements.isEmpty()) {
                        // Choose a random clickable element
                        UIElement target = clickableElements.get(random.nextInt(clickableElements.size()));
                        AIAction tapElement = AIAction.createTapAction(target.getCenterX(), target.getCenterY());
                        tapElement.addParameter("targetElementId", String.valueOf(target.getId()));
                        return tapElement;
                    }
                }
                
                // Fallback to center tap
                return AIAction.createTapAction(screenWidth / 2, screenHeight / 2);
                
            default:
                return AIAction.createTapAction(screenWidth / 2, screenHeight / 2);
        }
    }
    
    /**
     * Get the size of the action space
     * 
     * @return The action space size
     */
    protected int getActionSpaceSize() {
        return 12; // Default size based on mapIndexToAction implementation
    }
    
    /**
     * Filter out invalid actions from a list of actions
     * 
     * @param actions The actions to filter
     * @param gameState The game state
     * @return The filtered actions
     */
    protected List<AIAction> filterValidActions(List<AIAction> actions, GameState gameState) {
        if (actions == null || gameState == null) {
            return actions;
        }
        
        List<AIAction> validActions = new ArrayList<>();
        List<UIElement> uiElements = gameState.getUiElements();
        
        for (AIAction action : actions) {
            boolean isValid = true;
            
            // For tap actions, check if tap position overlaps with a clickable UI element
            if (AIAction.ACTION_TAP.equals(action.getActionType()) && uiElements != null) {
                boolean hitsClickableElement = false;
                
                String xStr = action.getParameter("x");
                String yStr = action.getParameter("y");
                String targetIdStr = action.getParameter("targetElementId");
                
                if (xStr != null && yStr != null) {
                    try {
                        int x = Integer.parseInt(xStr);
                        int y = Integer.parseInt(yStr);
                        
                        for (UIElement element : uiElements) {
                            if (element.containsPoint(x, y) && element.isInteractive()) {
                                hitsClickableElement = true;
                                break;
                            }
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing coordinates", e);
                    }
                }
                
                // Only add taps on clickable elements, except for basic directional taps
                if (!hitsClickableElement && targetIdStr != null) {
                    isValid = false;
                }
            }
            
            if (isValid) {
                validActions.add(action);
            }
        }
        
        return validActions;
    }
    
    @Override
    public void setLearningRate(float learningRate) {
        this.learningRate = learningRate;
    }
    
    @Override
    public float getLearningRate() {
        return learningRate;
    }
    
    @Override
    public void setDiscountFactor(float discountFactor) {
        this.discountFactor = discountFactor;
    }
    
    @Override
    public float getDiscountFactor() {
        return discountFactor;
    }
    
    @Override
    public void setExplorationRate(float explorationRate) {
        this.explorationRate = explorationRate;
    }
    
    @Override
    public float getExplorationRate() {
        return explorationRate;
    }
    
    /**
     * Normalize a value to the range [0, 1]
     * 
     * @param value The value to normalize
     * @param min The minimum value
     * @param max The maximum value
     * @return The normalized value
     */
    protected float normalize(float value, float min, float max) {
        if (max == min) {
            return 0.5f;
        }
        
        return Math.max(0, Math.min(1, (value - min) / (max - min)));
    }
    
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}

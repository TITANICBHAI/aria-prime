package com.aiassistant.ml;

import androidx.annotation.NonNull;
import utils.GameTypeHelper;
import androidx.annotation.Nullable;
import utils.GameTypeHelper;
import android.content.Context;
import utils.GameTypeHelper;
import android.graphics.Bitmap;
import utils.GameTypeHelper;
import android.graphics.PointF;
import utils.GameTypeHelper;
import android.graphics.Rect;
import utils.GameTypeHelper;

import com.aiassistant.core.AIController;
import utils.GameTypeHelper;
import com.aiassistant.detection.GameAppElementDetector;
import utils.GameTypeHelper;
import models.AppInfo;
import utils.GameTypeHelper;

import java.util.ArrayList;
import utils.GameTypeHelper;
import java.util.Collections;
import utils.GameTypeHelper;
import java.util.HashMap;
import utils.GameTypeHelper;
import java.util.List;
import utils.GameTypeHelper;
import java.util.Map;
import utils.GameTypeHelper;
import java.util.Objects;
import utils.GameTypeHelper;
import java.util.UUID;
import utils.GameTypeHelper;

/**
 * Predictive Action System for game play
 * 
 * Predicts and generates game actions based on game state analysis
 */
public class PredictiveActionSystem {
    
    private Context context;
    private AIController aiController;
    private AIController.GameType currentGameType = AIController.GameType.OTHER;
    private List<SuggestionListener> suggestionListeners = new ArrayList<>();
    
    /**
     * Constructor with context and AI controller
     * 
     * @param context Application context
     * @param aiController AI controller instance
     */
    public PredictiveActionSystem(Context context, AIController aiController) {
        this.context = context;
        this.aiController = aiController;
    }
    
    /**
     * Default constructor
     */
    public PredictiveActionSystem() {
        // Default constructor
    }
    
    /**
     * Select an action based on the current state vector
     * 
     * @param stateVector Map of state data or float array representing the state
     * @return Selected action as an integer
     */
    public int selectAction(Map<String, Object> stateVector) {
        // Implementation would analyze the state vector and return an appropriate action
        // For now, return a default action (0)
        return 0;
    }
    
    /**
     * Select an action based on the state vector as a float array
     * 
     * @param stateVector Float array representing the state
     * @return Selected action as an integer
     */
    public int selectAction(float[] stateVector) {
        // Convert float array to map for compatibility with the main selectAction method
        Map<String, Object> stateMap = new HashMap<>();
        if (stateVector != null) {
            for (int i = 0; i < stateVector.length; i++) {
                stateMap.put("state_" + i, stateVector[i]);
            }
        }
        return selectAction(stateMap);
    }
    
    /**
     * Select an action based on a generic object representing the state
     * 
     * @param state Object representing the state
     * @return Selected action as an integer
     */
    public int selectAction(Object state) {
        if (state instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> stateMap = (Map<String, Object>) state;
            return selectAction(stateMap);
        } else if (state instanceof float[]) {
            return selectAction((float[]) state);
        } else {
            // Handle unknown state format
            return 0;
        }
    }
    
    /**
     * Constructor with context only
     * 
     * @param context Application context
     */
    public PredictiveActionSystem(Context context) {
        this.context = context;
        this.aiController = null;
    }
    
    /**
     * Get a singleton instance
     * 
     * @param context Application context
     * @return Singleton instance
     */
    public static PredictiveActionSystem getInstance(Context context) {
        // This is a simplified singleton pattern
        return new PredictiveActionSystem(context, null);
    }
    
    /**
     * Initialize the system
     */
    public void initialize() {
        // Initialize prediction models and systems
    }
    
    /**
     * Release resources
     */
    public void release() {
        // Release resources
        suggestionListeners.clear();
    }
    
    /**
     * Start the system
     */
    public void start() {
        // Start processing
    }
    
    /**
     * Stop the system
     */
    public void stop() {
        // Stop processing
    }
    
    /**
     * Set the current game type
     * 
     * @param gameType Game type string
     */
    public void setGameType(String gameType) {
        // Convert string to AIController.GameType
        if (gameType == null || gameType.isEmpty()) {
            this.currentGameType = AIController.GameType.OTHER;
            return;
        }
        
        String lowerGameType = gameType.toLowerCase();
        switch (lowerGameType) {
            case "action":
                this.currentGameType = AIController.GameTypeHelper.ACTION;
                break;
            case "strategy":
                this.currentGameType = AIController.GameType.STRATEGY;
                break;
            case "puzzle":
                this.currentGameType = AIController.GameTypeHelper.PUZZLE;
                break;
            case "racing":
                this.currentGameType = AIController.GameTypeHelper.RACING;
                break;
            case "sports":
                this.currentGameType = AIController.GameTypeHelper.SPORTS;
                break;
            case "arcade":
                this.currentGameType = AIController.GameTypeHelper.ARCADE;
                break;
            default:
                this.currentGameType = AIController.GameType.OTHER;
                break;
        }
    }
    
    /**
     * Set the current game type
     * 
     * @param gameType Game type
     */
    public void setGameType(AIController.GameType gameType) {
        this.currentGameType = gameType;
    }
    
    /**
     * Process a frame from the game
     * 
     * @param frame Frame bitmap
     * @param elements Detected UI elements
     */
    public void processFrame(Bitmap frame, List<GameAppElementDetector.UIElement> elements) {
        // Process frame and detected elements
    }
    
    /**
     * Get the current game state
     * 
     * @return Current game state
     */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     public GameState getCurrentState() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         // Return a default state for now
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         return new GameState();
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * Get suggestions for the current app
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * @param app Current app info
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * @param contextData Context data
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     public void getSuggestions(AppInfo app, Map<String, Object> contextData) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         // Generate suggestions based on app and context
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * Interface for suggestion listeners
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     public interface SuggestionListener {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Called when a new suggestion is available
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param suggestions List of action suggestions
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         void onSuggestionAvailable(List<com.aiassistant.ml.ActionSuggestion> suggestions);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Called when an error occurs during suggestion generation
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param error Error message
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         void onSuggestionError(String error);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     }
// DUPLICATE: // DUPLICATE: // DUPLICATE:     
// DUPLICATE: // DUPLICATE: // DUPLICATE:     /**
// DUPLICATE: // DUPLICATE: // DUPLICATE:      * Interface for prediction callbacks
// DUPLICATE: // DUPLICATE: // DUPLICATE:      */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     public interface PredictionCallback {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Called when a new action recommendation is available
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param action Recommended game action
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         void onActionRecommendation(GameAction action);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Called when an error occurs during action prediction
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param error Error message
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         void onPredictionError(String error);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * Add a suggestion listener
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * @param listener Listener to add
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     public void addSuggestionListener(SuggestionListener listener) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         if (listener != null && !suggestionListeners.contains(listener)) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             suggestionListeners.add(listener);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * Remove a suggestion listener
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * @param listener Listener to remove
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     public void removeSuggestionListener(SuggestionListener listener) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         suggestionListeners.remove(listener);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     // List of registered prediction callbacks
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     private List<PredictionCallback> predictionCallbacks = new ArrayList<>();
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     // Current game state
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     private GameState currentState;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * Register a prediction callback
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * @param callback Callback to register
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     public void registerCallback(PredictionCallback callback) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         if (callback != null && !predictionCallbacks.contains(callback)) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             predictionCallbacks.add(callback);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * Unregister a prediction callback
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * @param callback Callback to unregister
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     public void unregisterCallback(PredictionCallback callback) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         predictionCallbacks.remove(callback);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * Get the current game state
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * @return The current game state or null if no state is available
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     public GameState getCurrentState() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         return currentState;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * Get statistics about the system
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * @return Map of statistics
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     public Map<String, Object> getStats() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         Map<String, Object> stats = new HashMap<>();
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         // Add basic statistics
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         stats.put("predictionsGenerated", 0);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         stats.put("accuracy", 0.0f);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         stats.put("callbacksRegistered", predictionCallbacks.size());
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         stats.put("listenersRegistered", suggestionListeners.size());
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         // Add performance statistics
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         stats.put("avgPredictionTime", 0.0f);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         stats.put("successfulPredictions", 0);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         return stats;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * Types of actions the system can predict
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     public enum ActionType {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         TAP("tap"),
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         SWIPE("swipe"),
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         LONG_PRESS("long_press"),
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         MULTI_TAP("multi_tap"),
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         DRAG("drag"),
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         PINCH("pinch"),
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         ZOOM("zoom"),
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         ROTATE("rotate"),
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         CUSTOM("custom");
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         private final String value;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         ActionType(String value) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this.value = value;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public String getValue() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return value;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @NonNull
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public static ActionType fromString(String value) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             if (value == null) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 return TAP;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             String lowerValue = value.toLowerCase();
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             for (ActionType actionType : values()) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 if (actionType.value.equals(lowerValue)) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     return actionType;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return TAP;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @Override
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public String toString() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return value;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * Represents a game action to be performed
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     public static class GameAction {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         private final String id;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         private final ActionType type;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         private final Map<String, Object> parameters;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         private float priority;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         private float confidence;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         private String displayName;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         private AIController.GameType gameType;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Create a new game action
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param type Action type
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param parameters Action parameters
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param priority Action priority (0.0-1.0)
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param confidence Confidence in the action (0.0-1.0)
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public GameAction(
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 @NonNull ActionType type,
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 @Nullable Map<String, Object> parameters,
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 float priority,
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 float confidence) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this.id = UUID.randomUUID().toString();
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this.type = type;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this.priority = Math.max(0.0f, Math.min(1.0f, priority));
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this.confidence = Math.max(0.0f, Math.min(1.0f, confidence));
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this.displayName = generateDisplayName();
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this.gameType = AIController.GameType.OTHER;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Create a new game action with default values
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param type Action type
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public GameAction(@NonNull ActionType type) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this(type, null, 0.5f, 0.5f);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get the action ID
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Action ID
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @NonNull
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public String getId() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return id;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get the action type
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Action type
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @NonNull
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public ActionType getType() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return type;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get the action parameters
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Unmodifiable map of parameters
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @NonNull
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public Map<String, Object> getParameters() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return Collections.unmodifiableMap(parameters);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Add a parameter to the action
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param key Parameter key
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param value Parameter value
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public void addParameter(@NonNull String key, @Nullable Object value) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             parameters.put(key, value);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get a parameter value
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param key Parameter key
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param <T> Parameter type
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Parameter value or null if not found
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @SuppressWarnings("unchecked")
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @Nullable
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public <T> T getParameter(@NonNull String key) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return (T) parameters.get(key);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get a parameter value with a default
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param key Parameter key
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param defaultValue Default value if parameter not found
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param <T> Parameter type
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Parameter value or default
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @SuppressWarnings("unchecked")
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public <T> T getParameter(@NonNull String key, T defaultValue) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             Object value = parameters.get(key);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             if (value == null) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 return defaultValue;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             try {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 return (T) value;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             } catch (ClassCastException e) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 return defaultValue;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Check if the action has a parameter
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param key Parameter key
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Whether the parameter exists
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public boolean hasParameter(@NonNull String key) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return parameters.containsKey(key);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get the action priority
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Priority (0.0-1.0)
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public float getPriority() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return priority;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Set the action priority
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param priority Priority (0.0-1.0)
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public void setPriority(float priority) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this.priority = Math.max(0.0f, Math.min(1.0f, priority));
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get the action confidence
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Confidence (0.0-1.0)
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public float getConfidence() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return confidence;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Set the action confidence
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param confidence Confidence (0.0-1.0)
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public void setConfidence(float confidence) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this.confidence = Math.max(0.0f, Math.min(1.0f, confidence));
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get the action display name
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Display name
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @NonNull
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public String getDisplayName() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return displayName;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Set the action display name
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param displayName Display name
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public void setDisplayName(@NonNull String displayName) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this.displayName = displayName;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get the game type
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Game type
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @NonNull
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public AIController.GameType getGameType() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return gameType;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Set the game type
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param gameType Game type
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public void setGameType(@NonNull AIController.GameType gameType) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this.gameType = gameType;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get the action location if available
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Location point or null if not specified
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @Nullable
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public PointF getLocation() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             if (parameters.containsKey("x") && parameters.containsKey("y")) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 try {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     float x = 0;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     float y = 0;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     Object xObj = parameters.get("x");
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     Object yObj = parameters.get("y");
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     if (xObj instanceof Number) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                         x = ((Number) xObj).floatValue();
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     } else if (xObj instanceof String) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                         x = Float.parseFloat((String) xObj);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     if (yObj instanceof Number) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                         y = ((Number) yObj).floatValue();
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     } else if (yObj instanceof String) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                         y = Float.parseFloat((String) yObj);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     return new PointF(x, y);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 } catch (NumberFormatException e) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     return null;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             } else if (parameters.containsKey("location") && parameters.get("location") instanceof PointF) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 return (PointF) parameters.get("location");
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return null;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Set the action location
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param x X coordinate
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param y Y coordinate
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public void setLocation(float x, float y) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             parameters.put("x", x);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             parameters.put("y", y);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get the action bounds if available
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Bounds rectangle or null if not specified
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @Nullable
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public Rect getBounds() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             if (parameters.containsKey("bounds") && parameters.get("bounds") instanceof Rect) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 return (Rect) parameters.get("bounds");
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return null;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Set the action bounds
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param bounds Bounds rectangle
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public void setBounds(@NonNull Rect bounds) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             parameters.put("bounds", bounds);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Generate a display name based on action type and parameters
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Generated display name
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         private String generateDisplayName() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             StringBuilder name = new StringBuilder(type.toString().toUpperCase());
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             if (parameters.containsKey("target")) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 name.append(" on ").append(parameters.get("target"));
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             } else if (parameters.containsKey("item")) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 name.append(" ").append(parameters.get("item"));
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             } else if (parameters.containsKey("action_name")) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 name = new StringBuilder(parameters.get("action_name").toString());
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return name.toString();
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @Override
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public boolean equals(Object o) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             if (this == o) return true;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             if (o == null || getClass() != o.getClass()) return false;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             GameAction that = (GameAction) o;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return id.equals(that.id);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @Override
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public int hashCode() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return Objects.hash(id);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @Override
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public String toString() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return "GameAction{" +
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     "type=" + type +
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     ", priority=" + priority +
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     ", confidence=" + confidence +
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     ", name='" + displayName + '\'' +
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     '}';
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * Represents a game state at a point in time
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     public static class GameState {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         private final String id;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         private final long timestamp;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         private final List<GameAppElementDetector.UIElement> elements;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         private final Map<String, Object> stateData;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         private final Map<String, Object> metadata;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Create a new game state
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param elements UI elements in the game state
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param stateData Additional state data
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param metadata Metadata about the state
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public GameState(
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 @Nullable List<GameAppElementDetector.UIElement> elements,
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 @Nullable Map<String, Object> stateData,
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 @Nullable Map<String, Object> metadata) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this.id = UUID.randomUUID().toString();
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this.timestamp = System.currentTimeMillis();
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this.elements = elements != null ? 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     new ArrayList<>(elements) : new ArrayList<>();
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this.stateData = stateData != null ? 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     new HashMap<>(stateData) : new HashMap<>();
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this.metadata = metadata != null ? 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     new HashMap<>(metadata) : new HashMap<>();
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Create a new game state with default values
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public GameState() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             this(null, null, null);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get the state ID
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return State ID
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @NonNull
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public String getId() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return id;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get the state timestamp
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Timestamp in milliseconds
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public long getTimestamp() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return timestamp;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get the UI elements in the state
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Unmodifiable list of UI elements
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @NonNull
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public List<GameAppElementDetector.UIElement> getElements() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return Collections.unmodifiableList(elements);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get the state data
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Unmodifiable map of state data
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @NonNull
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public Map<String, Object> getStateData() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return Collections.unmodifiableMap(stateData);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get the state metadata
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Unmodifiable map of metadata
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @NonNull
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public Map<String, Object> getMetadata() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return Collections.unmodifiableMap(metadata);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Add an element to the state
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param element UI element to add
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public void addElement(@NonNull GameAppElementDetector.UIElement element) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             elements.add(element);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Add state data
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param key Data key
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param value Data value
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public void addStateData(@NonNull String key, @Nullable Object value) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             stateData.put(key, value);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Add metadata
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param key Metadata key
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param value Metadata value
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public void addMetadata(@NonNull String key, @Nullable Object value) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             metadata.put(key, value);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get an element by ID
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param elementId Element ID
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return UI element or null if not found
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @Nullable
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public GameAppElementDetector.UIElement getElementById(@NonNull String elementId) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             for (GameAppElementDetector.UIElement element : elements) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 if (element.getId().equals(elementId)) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     return element;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return null;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get elements by type
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param elementType Element type
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return List of UI elements of the specified type
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @NonNull
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public List<GameAppElementDetector.UIElement> getElementsByType(@NonNull String elementType) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             List<GameAppElementDetector.UIElement> result = new ArrayList<>();
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             for (GameAppElementDetector.UIElement element : elements) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 if (element.getType().equals(elementType)) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     result.add(element);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return result;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get a state data value
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param key Data key
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param <T> Data type
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Data value or null if not found
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @SuppressWarnings("unchecked")
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @Nullable
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public <T> T getStateDataValue(@NonNull String key) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return (T) stateData.get(key);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get a state data value with a default
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param key Data key
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param defaultValue Default value if data not found
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param <T> Data type
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Data value or default
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @SuppressWarnings("unchecked")
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public <T> T getStateDataValue(@NonNull String key, T defaultValue) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             Object value = stateData.get(key);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             if (value == null) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 return defaultValue;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             try {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 return (T) value;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             } catch (ClassCastException e) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 return defaultValue;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get a metadata value
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param key Metadata key
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param <T> Metadata type
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Metadata value or null if not found
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @SuppressWarnings("unchecked")
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @Nullable
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public <T> T getMetadataValue(@NonNull String key) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return (T) metadata.get(key);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Get a metadata value with a default
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param key Metadata key
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param defaultValue Default value if metadata not found
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param <T> Metadata type
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @return Metadata value or default
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @SuppressWarnings("unchecked")
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public <T> T getMetadataValue(@NonNull String key, T defaultValue) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             Object value = metadata.get(key);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             if (value == null) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 return defaultValue;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             try {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 return (T) value;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             } catch (ClassCastException e) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                 return defaultValue;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @Override
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public boolean equals(Object o) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             if (this == o) return true;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             if (o == null || getClass() != o.getClass()) return false;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             GameState that = (GameState) o;
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return id.equals(that.id);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @Override
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public int hashCode() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return Objects.hash(id);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         @Override
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         public String toString() {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:             return "GameState{" +
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     "id='" + id + '\'' +
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     ", timestamp=" + timestamp +
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     ", elements=" + elements.size() +
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:                     '}';
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * Callback interface for prediction notifications
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     public interface PredictionCallback {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Called when a state prediction is made
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param currentState Current game state
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param predictedState Predicted future game state
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         void onStatePrediction(GameState currentState, GameState predictedState);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * Called when an action is recommended
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          * @param recommendedAction Recommended action
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:          */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         void onActionRecommendation(GameAction recommendedAction);
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     /**
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * Register a prediction callback
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * 
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      * @param callback Callback to register
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:      */
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     public void registerCallback(PredictionCallback callback) {
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:         // Implementation would go here
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     }
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE:     // We now use AIController.GameType instead of a local GameType enum
// DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: // DUPLICATE: }
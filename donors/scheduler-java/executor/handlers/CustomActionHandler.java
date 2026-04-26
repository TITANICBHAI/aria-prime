package com.aiassistant.scheduler.executor.handlers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import utils.ActionCallback;
import utils.StandardizedActionHandler;
import utils.ActionResult;
import utils.ActionStatus;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for handlers that execute custom actions
 * 
 * This interface extends StandardizedActionHandler to ensure consistent implementation
 * across all custom action handlers in the application.
 */
public interface CustomActionHandler extends StandardizedActionHandler {
    /**
     * Execute a custom action with the given parameters
     * 
     * @param params Action parameters
     * @return True if execution was successful, false otherwise
     */
    boolean executeAction(Map<String, Object> params);
    
    /**
     * Execute a custom action with the given parameters and callback
     * 
     * @param parameters Action parameters
     * @param callback Callback for action execution results
     */
    void executeAction(Map<String, Object> parameters, ActionCallback callback);
    
    /**
     * Cancel any ongoing action execution
     */
    void cancel();
    
    /**
     * Get the type of action this handler can process
     * 
     * @return Action handler type
     */
    String getHandlerType();
    
    /**
     * Implementation of StandardizedActionHandler.executeAction
     * 
     * @param actionId Unique identifier for the action
     * @param parameters Parameters for the action
     * @return CompletableFuture with the action result
     */
    @NonNull
    default CompletableFuture<ActionResult> executeAction(
            @NonNull String actionId,
            @Nullable Map<String, Object> parameters) {
        CompletableFuture<ActionResult> future = new CompletableFuture<>();
        
        try {
            boolean success = executeAction(parameters != null ? parameters : Map.of());
            if (success) {
                future.complete(ActionResult.success("Action executed successfully"));
            } else {
                future.complete(ActionResult.failure("Action execution failed"));
            }
        } catch (Exception e) {
            future.complete(ActionResult.failure("Error executing action: " + e.getMessage()));
        }
        
        return future;
    }
    
    /**
     * Implementation of StandardizedActionHandler.getActionType
     * 
     * @return Action type string
     */
    @NonNull
    default String getActionType() {
        return getHandlerType();
    }
    
    /**
     * Implementation of StandardizedActionHandler.canExecute
     * 
     * @param actionId Unique identifier for the action
     * @return Whether this handler can execute the action
     */
    default boolean canExecute(@NonNull String actionId) {
        return true; // Default implementation always returns true
    }
    
    /**
     * Implementation of StandardizedActionHandler.validateParameters
     * 
     * @param actionId Unique identifier for the action
     * @param parameters Parameters for the action
     * @return Whether the parameters are valid
     */
    default boolean validateParameters(
            @NonNull String actionId,
            @Nullable Map<String, Object> parameters) {
        return true; // Default implementation always returns true
    }
    
    /**
     * Implementation of StandardizedActionHandler.getRequiredParameters
     * 
     * @param actionId Unique identifier for the action
     * @return Map of parameter names to parameter types
     */
    @NonNull
    default Map<String, Class<?>> getRequiredParameters(@NonNull String actionId) {
        return Map.of(); // Default implementation returns empty map
    }
    
    /**
     * Implementation of StandardizedActionHandler.cancelAction
     * 
     * @param actionId Unique identifier for the action
     * @return Whether the action was successfully cancelled
     */
    default boolean cancelAction(@NonNull String actionId) {
        cancel();
        return true;
    }
    
    /**
     * Implementation of StandardizedActionHandler.getActionStatus
     * 
     * @param actionId Unique identifier for the action
     * @return Current action status or null if not found
     */
    @Nullable
    default ActionStatus getActionStatus(@NonNull String actionId) {
        return null; // Default implementation returns null
    }
}
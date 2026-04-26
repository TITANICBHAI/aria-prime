package com.aiassistant.scheduler.executor;

import java.util.Map;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for action handlers that execute different types of actions
 * 
 * This interface defines common methods for action execution in the application.
 */
public interface ActionHandler {
    
    /**
     * Execute an action with the given parameters
     * 
     * @param params Action parameters
     * @return true if execution was successful
     */
    boolean executeAction(Map<String, Object> params);
    
    /**
     * Execute an action with the given parameters and callback
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
     * Execute an action with the given identifier and parameters
     * 
     * This method is designed to be compatible with StandardizedActionHandler interface
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
     * Get the action type this handler can process
     * 
     * @return Action type string
     */
    @NonNull
    default String getActionType() {
        return getHandlerType();
    }
    
    /**
     * Check if this handler can execute the given action
     * 
     * @param actionId Unique identifier for the action
     * @return Whether this handler can execute the action
     */
    default boolean canExecute(@NonNull String actionId) {
        return true; // Default implementation always returns true
    }
    
    /**
     * Validate the parameters for the given action
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
     * Get the required parameters for the given action
     * 
     * @param actionId Unique identifier for the action
     * @return Map of parameter names to parameter types
     */
    @NonNull
    default Map<String, Class<?>> getRequiredParameters(@NonNull String actionId) {
        return Map.of(); // Default implementation returns empty map
    }
    
    /**
     * Cancel the given action
     * 
     * @param actionId Unique identifier for the action
     * @return Whether the action was successfully cancelled
     */
    default boolean cancelAction(@NonNull String actionId) {
        cancel();
        return true;
    }
    
    /**
     * Get the current status of the given action
     * 
     * @param actionId Unique identifier for the action
     * @return Current action status or null if not found
     */
    @Nullable
    default ActionStatus getActionStatus(@NonNull String actionId) {
        return null; // Default implementation returns null
    }
}
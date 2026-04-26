package com.aiassistant.scheduler.executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Standardized interface for action handlers
 */
public interface StandardizedActionHandler {
    
    /**
     * Execute an action with the given identifier and parameters
     * 
     * @param actionId Unique identifier for the action
     * @param parameters Parameters for the action
     * @return CompletableFuture with the action result
     */
    @NonNull
    CompletableFuture<ActionResult> executeAction(
            @NonNull String actionId,
            @Nullable Map<String, Object> parameters);
    
    /**
     * Get the action type this handler can process
     * 
     * @return Action type string
     */
    @NonNull
    String getActionType();
    
    /**
     * Check if this handler can execute the given action
     * 
     * @param actionId Unique identifier for the action
     * @return Whether this handler can execute the action
     */
    boolean canExecute(@NonNull String actionId);
    
    /**
     * Validate the parameters for the given action
     * 
     * @param actionId Unique identifier for the action
     * @param parameters Parameters for the action
     * @return Whether the parameters are valid
     */
    boolean validateParameters(
            @NonNull String actionId,
            @Nullable Map<String, Object> parameters);
    
    /**
     * Get the required parameters for the given action
     * 
     * @param actionId Unique identifier for the action
     * @return Map of parameter names to parameter types
     */
    @NonNull
    Map<String, Class<?>> getRequiredParameters(@NonNull String actionId);
    
    /**
     * Cancel the given action
     * 
     * @param actionId Unique identifier for the action
     * @return Whether the action was successfully cancelled
     */
    boolean cancelAction(@NonNull String actionId);
    
    /**
     * Get the current status of the given action
     * 
     * @param actionId Unique identifier for the action
     * @return Current action status or null if not found
     */
    @Nullable
    ActionStatus getActionStatus(@NonNull String actionId);
}
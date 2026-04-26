package com.aiassistant.scheduler.executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Adapter class that converts a regular ActionHandler to a StandardizedActionHandler
 * This solves the issue of having two distinct ActionHandler interfaces
 */
public class StandardizedActionHandlerAdapter implements StandardizedActionHandler {
    
    private final ActionHandler actionHandler;
    
    /**
     * Create a new adapter for the given action handler
     * 
     * @param actionHandler Action handler to adapt
     */
    public StandardizedActionHandlerAdapter(@NonNull ActionHandler actionHandler) {
        this.actionHandler = actionHandler;
    }
    
    /**
     * Get the wrapped action handler
     * 
     * @return The wrapped action handler
     */
    @NonNull
    public ActionHandler getActionHandler() {
        return actionHandler;
    }
    
    @Override
    @NonNull
    public CompletableFuture<ActionResult> executeAction(
            @NonNull String actionId,
            @Nullable Map<String, Object> parameters) {
        // Use the default implementation from the interface
        return actionHandler.executeStandardizedAction(actionId, parameters);
    }
    
    @Override
    @NonNull
    public String getActionType() {
        return actionHandler.getHandlerType();
    }
    
    @Override
    public boolean canExecute(@NonNull String actionId) {
        return true; // Use simple implementation to delegate to wrapped handler
    }
    
    @Override
    public boolean validateParameters(
            @NonNull String actionId,
            @Nullable Map<String, Object> parameters) {
        return true; // Use simple implementation to delegate to wrapped handler
    }
    
    @Override
    @NonNull
    public Map<String, Class<?>> getRequiredParameters(@NonNull String actionId) {
        return actionHandler.getRequiredParameters(actionId);
    }
    
    @Override
    public boolean cancelAction(@NonNull String actionId) {
        actionHandler.cancel();
        return true;
    }
    
    @Override
    @Nullable
    public ActionStatus getActionStatus(@NonNull String actionId) {
        return actionHandler.getActionStatus(actionId);
    }
}
package com.aiassistant.scheduler.executor;

import java.util.Map;

/**
 * Callback interface for asynchronous action execution
 */
public interface ActionCallback {
    
    /**
     * Called when the action completes successfully
     * 
     * @param result Action result data
     */
    void onComplete(Map<String, Object> result);
    
    /**
     * Called when the action fails
     * 
     * @param errorMessage Error message
     */
    void onError(String errorMessage);
}
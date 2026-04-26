package com.aiassistant.scheduler.executor;

import android.content.Context;
import android.util.Log;

import com.aiassistant.scheduler.executor.handlers.ActionHandler;
import com.aiassistant.scheduler.executor.handlers.ApiCallHandler;
import com.aiassistant.scheduler.executor.handlers.AppControlHandler;
import com.aiassistant.scheduler.executor.handlers.CustomActionHandler;
import com.aiassistant.scheduler.executor.handlers.EmailHandler;
import com.aiassistant.scheduler.executor.handlers.NotificationHandler;
import com.aiassistant.scheduler.executor.handlers.SystemActionHandler;
import com.aiassistant.scheduler.executor.handlers.WaitHandler;
import com.aiassistant.scheduler.model.ActionType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Execute actions based on their type
 */
public class ActionExecutor {
    private static final String TAG = "ActionExecutor";
    
    // Context
    private final Context context;
    
    // Action handlers
    private final Map<ActionType, ActionHandler> handlers = new ConcurrentHashMap<>();
    
    // Custom action handlers
    private final Map<String, CustomActionHandler> customHandlers = new ConcurrentHashMap<>();
    
    /**
     * Create a new action executor
     */
    public ActionExecutor(Context context) {
        this.context = context;
        
        // Register default handlers
        registerDefaultHandlers();
    }
    
    /**
     * Register default action handlers
     */
    private void registerDefaultHandlers() {
        // Register built-in handlers
        handlers.put(ActionType.WAIT, new WaitHandler());
        handlers.put(ActionType.SYSTEM, new SystemActionHandler(context));
        handlers.put(ActionType.APP_CONTROL, new AppControlHandler(context));
        handlers.put(ActionType.API_CALL, new ApiCallHandler());
        handlers.put(ActionType.NOTIFICATION, new NotificationHandler(context));
        handlers.put(ActionType.EMAIL, new EmailHandler(context));
    }
    
    /**
     * Register a custom action handler
     */
    public void registerCustomHandler(String handlerId, CustomActionHandler handler) {
        if (handlerId != null && handler != null) {
            customHandlers.put(handlerId, handler);
            Log.d(TAG, "Registered custom handler: " + handlerId);
        }
    }
    
    /**
     * Unregister a custom action handler
     */
    public void unregisterCustomHandler(String handlerId) {
        if (customHandlers.remove(handlerId) != null) {
            Log.d(TAG, "Unregistered custom handler: " + handlerId);
        }
    }
    
    /**
     * Register an action handler for a specific action type
     */
    public void registerHandler(ActionType actionType, ActionHandler handler) {
        if (actionType != null && handler != null) {
            handlers.put(actionType, handler);
            Log.d(TAG, "Registered handler for action type: " + actionType);
        }
    }
    
    /**
     * Execute an action
     */
    public boolean executeAction(ActionType actionType, Map<String, Object> parameters) {
        if (actionType == null) {
            Log.e(TAG, "Cannot execute action with null type");
            return false;
        }
        
        // Ensure parameters is not null
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        
        try {
            Log.d(TAG, "Executing action: " + actionType);
            
            // Handle custom actions
            if (actionType == ActionType.CUSTOM) {
                return executeCustomAction(parameters);
            }
            
            // Get handler for this action type
            ActionHandler handler = handlers.get(actionType);
            
            if (handler == null) {
                Log.e(TAG, "No handler registered for action type: " + actionType);
                return false;
            }
            
            // Execute the action
            return handler.executeAction(parameters);
            
        } catch (Exception e) {
            Log.e(TAG, "Error executing action: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Execute a custom action
     */
    private boolean executeCustomAction(Map<String, Object> parameters) {
        // Get custom handler ID
        Object handlerId = parameters.get("handler");
        
        if (handlerId == null) {
            Log.e(TAG, "Custom action missing handler ID");
            return false;
        }
        
        String handlerIdStr = handlerId.toString();
        
        // Get custom handler
        CustomActionHandler handler = customHandlers.get(handlerIdStr);
        
        if (handler == null) {
            Log.e(TAG, "No custom handler registered with ID: " + handlerIdStr);
            return false;
        }
        
        // Get action data
        Object data = parameters.get("data");
        
        if (data == null) {
            data = new HashMap<String, Object>();
        }
        
        // Execute custom action
        try {
            Map<String, Object> customParams = new HashMap<>();
            customParams.put("actionId", handlerIdStr);
            customParams.put("data", data);
            return handler.executeAction(customParams);
        } catch (Exception e) {
            Log.e(TAG, "Error executing custom action: " + e.getMessage(), e);
            return false;
        }
    }
}
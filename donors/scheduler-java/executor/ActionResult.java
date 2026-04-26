package com.aiassistant.scheduler.executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Result of an action execution
 */
public class ActionResult {
    private final boolean success;
    private final String message;
    private final Map<String, Object> data;
    private final long executionTimeMs;
    private final String errorCode;
    
    /**
     * Create a new action result
     * 
     * @param success Whether the action was successful
     * @param message Result message
     * @param data Additional result data
     * @param executionTimeMs Execution time in milliseconds
     * @param errorCode Error code if the action failed
     */
    public ActionResult(
            boolean success,
            @NonNull String message,
            @Nullable Map<String, Object> data,
            long executionTimeMs,
            @Nullable String errorCode) {
        this.success = success;
        this.message = message;
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
        this.executionTimeMs = executionTimeMs;
        this.errorCode = errorCode;
    }
    
    /**
     * Create a successful action result
     * 
     * @param message Success message
     * @param data Additional result data
     * @param executionTimeMs Execution time in milliseconds
     * @return Successful action result
     */
    public static ActionResult success(
            @NonNull String message,
            @Nullable Map<String, Object> data,
            long executionTimeMs) {
        return new ActionResult(true, message, data, executionTimeMs, null);
    }
    
    /**
     * Create a successful action result without data
     * 
     * @param message Success message
     * @param executionTimeMs Execution time in milliseconds
     * @return Successful action result
     */
    public static ActionResult success(
            @NonNull String message,
            long executionTimeMs) {
        return success(message, null, executionTimeMs);
    }
    
    /**
     * Create a successful action result with default execution time
     * 
     * @param message Success message
     * @return Successful action result
     */
    public static ActionResult success(@NonNull String message) {
        return success(message, null, 0);
    }
    
    /**
     * Create a failed action result
     * 
     * @param message Error message
     * @param executionTimeMs Execution time in milliseconds
     * @param errorCode Error code
     * @return Failed action result
     */
    public static ActionResult failure(
            @NonNull String message,
            long executionTimeMs,
            @Nullable String errorCode) {
        return new ActionResult(false, message, null, executionTimeMs, errorCode);
    }
    
    /**
     * Create a failed action result without error code
     * 
     * @param message Error message
     * @param executionTimeMs Execution time in milliseconds
     * @return Failed action result
     */
    public static ActionResult failure(
            @NonNull String message,
            long executionTimeMs) {
        return failure(message, executionTimeMs, "ERROR_UNKNOWN");
    }
    
    /**
     * Create a failed action result with default execution time
     * 
     * @param message Error message
     * @return Failed action result
     */
    public static ActionResult failure(@NonNull String message) {
        return failure(message, 0, "ERROR_UNKNOWN");
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    @NonNull
    public String getMessage() {
        return message;
    }
    
    @NonNull
    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }
    
    /**
     * Get a specific value from the result data
     * 
     * @param key Data key
     * @param <T> Expected value type
     * @return Data value or null if not found
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getValue(String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return null;
        }
    }
    
    /**
     * Get a specific value from the result data with default value
     * 
     * @param key Data key
     * @param defaultValue Default value if not found or wrong type
     * @param <T> Expected value type
     * @return Data value or default value if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(String key, T defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    @Nullable
    public String getErrorCode() {
        return errorCode;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionResult that = (ActionResult) o;
        return success == that.success &&
                executionTimeMs == that.executionTimeMs &&
                message.equals(that.message) &&
                Objects.equals(data, that.data) &&
                Objects.equals(errorCode, that.errorCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(success, message, data, executionTimeMs, errorCode);
    }
    
    @Override
    public String toString() {
        return "ActionResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", executionTime=" + executionTimeMs + "ms" +
                (errorCode != null ? ", errorCode='" + errorCode + '\'' : "") +
                '}';
    }
}
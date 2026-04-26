package com.aiassistant.core/orchestration;

import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CircuitBreaker {
    private static final String TAG = "CircuitBreaker";
    
    private final String componentId;
    private final int failureThreshold;
    private final long cooldownPeriodMs;
    
    private final AtomicInteger failureCount;
    private final AtomicLong lastFailureTime;
    private final AtomicInteger executionCount;
    
    private volatile State state;
    
    public CircuitBreaker(String componentId, int failureThreshold, long cooldownPeriodMs) {
        this.componentId = componentId;
        this.failureThreshold = failureThreshold;
        this.cooldownPeriodMs = cooldownPeriodMs;
        this.failureCount = new AtomicInteger(0);
        this.lastFailureTime = new AtomicLong(0);
        this.executionCount = new AtomicInteger(0);
        this.state = State.CLOSED;
    }
    
    public boolean allowExecution() {
        executionCount.incrementAndGet();
        
        if (state == State.OPEN) {
            long currentTime = System.currentTimeMillis();
            long timeSinceFailure = currentTime - lastFailureTime.get();
            
            if (timeSinceFailure >= cooldownPeriodMs) {
                Log.i(TAG, "Circuit breaker for " + componentId + 
                      " entering HALF_OPEN state after cooldown");
                state = State.HALF_OPEN;
                return true;
            }
            
            Log.w(TAG, "Circuit breaker for " + componentId + " is OPEN - blocking execution");
            return false;
        }
        
        return true;
    }
    
    public void recordSuccess() {
        failureCount.set(0);
        
        if (state == State.HALF_OPEN) {
            Log.i(TAG, "Circuit breaker for " + componentId + " transitioning to CLOSED");
            state = State.CLOSED;
        }
    }
    
    public void recordFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        
        if (failures >= failureThreshold && state != State.OPEN) {
            Log.w(TAG, "Circuit breaker for " + componentId + 
                  " OPEN - threshold reached (" + failures + "/" + failureThreshold + ")");
            state = State.OPEN;
        }
    }
    
    public void reset() {
        failureCount.set(0);
        state = State.CLOSED;
        Log.i(TAG, "Circuit breaker for " + componentId + " reset to CLOSED");
    }
    
    public State getState() {
        return state;
    }
    
    public int getFailureCount() {
        return failureCount.get();
    }
    
    public int getExecutionCount() {
        return executionCount.get();
    }
    
    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
}

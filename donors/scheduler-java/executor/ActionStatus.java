package com.aiassistant.scheduler.executor;

import androidx.annotation.NonNull;
import java.util.Objects;

/**
 * Status of an action execution
 */
public class ActionStatus {
    private final String actionId;
    private final String status;
    private final long startTimeMs;
    private final long lastUpdateTimeMs;
    private final int progressPercent;
    private final String statusMessage;
    
    /**
     * Create a new action status
     * 
     * @param actionId Unique identifier for the action
     * @param status Status string (starting, running, completed, failed, cancelled)
     * @param startTimeMs Time when the action started (milliseconds since epoch)
     * @param lastUpdateTimeMs Time of the last status update (milliseconds since epoch)
     * @param progressPercent Progress percentage (0-100)
     * @param statusMessage Status message
     */
    public ActionStatus(
            @NonNull String actionId,
            @NonNull String status,
            long startTimeMs,
            long lastUpdateTimeMs,
            int progressPercent,
            @NonNull String statusMessage) {
        this.actionId = actionId;
        this.status = status;
        this.startTimeMs = startTimeMs;
        this.lastUpdateTimeMs = lastUpdateTimeMs;
        this.progressPercent = Math.max(0, Math.min(100, progressPercent));
        this.statusMessage = statusMessage;
    }
    
    @NonNull
    public String getActionId() {
        return actionId;
    }
    
    @NonNull
    public String getStatus() {
        return status;
    }
    
    public long getStartTimeMs() {
        return startTimeMs;
    }
    
    public long getLastUpdateTimeMs() {
        return lastUpdateTimeMs;
    }
    
    public int getProgressPercent() {
        return progressPercent;
    }
    
    @NonNull
    public String getStatusMessage() {
        return statusMessage;
    }
    
    /**
     * Get the elapsed time since the action started
     * 
     * @return Elapsed time in milliseconds
     */
    public long getElapsedTimeMs() {
        return lastUpdateTimeMs - startTimeMs;
    }
    
    /**
     * Check if the action is in progress
     * 
     * @return Whether the action is in progress
     */
    public boolean isInProgress() {
        return status.equals("starting") || status.equals("running");
    }
    
    /**
     * Check if the action is completed
     * 
     * @return Whether the action is completed
     */
    public boolean isCompleted() {
        return status.equals("completed");
    }
    
    /**
     * Check if the action failed
     * 
     * @return Whether the action failed
     */
    public boolean isFailed() {
        return status.equals("failed");
    }
    
    /**
     * Check if the action was cancelled
     * 
     * @return Whether the action was cancelled
     */
    public boolean isCancelled() {
        return status.equals("cancelled");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionStatus that = (ActionStatus) o;
        return startTimeMs == that.startTimeMs &&
                lastUpdateTimeMs == that.lastUpdateTimeMs &&
                progressPercent == that.progressPercent &&
                actionId.equals(that.actionId) &&
                status.equals(that.status) &&
                statusMessage.equals(that.statusMessage);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(actionId, status, startTimeMs, lastUpdateTimeMs, progressPercent, statusMessage);
    }
    
    @Override
    public String toString() {
        return "ActionStatus{" +
                "actionId='" + actionId + '\'' +
                ", status='" + status + '\'' +
                ", progress=" + progressPercent + "%" +
                ", message='" + statusMessage + '\'' +
                ", elapsed=" + getElapsedTimeMs() + "ms" +
                '}';
    }
}
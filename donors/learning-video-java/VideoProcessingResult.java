package com.aiassistant.learning.video;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Result of video processing operation
 */
public class VideoProcessingResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String processId;
    private String videoUri;
    private String title;
    private String description;
    private long startTime;
    private long endTime;
    private VideoProcessingStatus status;
    private int progress;
    private int frameCount;
    private String dominantAppPackage;
    private String dominantAppName;
    private float dominantAppPercentage;
    private Map<String, Integer> appDetectionCounts;
    private String errorMessage;
    private Map<String, Object> additionalData; // For storing advanced processing results
    
    /**
     * Create a new video processing result
     */
    public VideoProcessingResult(String processId, String videoUri, String title, String description) {
        this.processId = processId;
        this.videoUri = videoUri;
        this.title = title;
        this.description = description;
        this.startTime = System.currentTimeMillis();
        this.status = VideoProcessingStatus.PENDING;
        this.progress = 0;
        this.frameCount = 0;
        this.dominantAppPercentage = 0;
        this.appDetectionCounts = new HashMap<>();
        this.additionalData = new HashMap<>();
    }
    
    // Getters and setters
    
    public String getProcessId() {
        return processId;
    }
    
    public String getVideoUri() {
        return videoUri;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public VideoProcessingStatus getStatus() {
        return status;
    }
    
    public void setStatus(VideoProcessingStatus status) {
        this.status = status;
        
        if (status == VideoProcessingStatus.COMPLETED || 
            status == VideoProcessingStatus.FAILED) {
            this.endTime = System.currentTimeMillis();
        }
    }
    
    public int getProgress() {
        return progress;
    }
    
    public void setProgress(int progress) {
        this.progress = progress;
    }
    
    public int getFrameCount() {
        return frameCount;
    }
    
    public void setFrameCount(int frameCount) {
        this.frameCount = frameCount;
    }
    
    public String getDominantAppPackage() {
        return dominantAppPackage;
    }
    
    public void setDominantAppPackage(String dominantAppPackage) {
        this.dominantAppPackage = dominantAppPackage;
    }
    
    public String getDominantAppName() {
        return dominantAppName;
    }
    
    public void setDominantAppName(String dominantAppName) {
        this.dominantAppName = dominantAppName;
    }
    
    public float getDominantAppPercentage() {
        return dominantAppPercentage;
    }
    
    public void setDominantAppPercentage(float dominantAppPercentage) {
        this.dominantAppPercentage = dominantAppPercentage;
    }
    
    public Map<String, Integer> getAppDetectionCounts() {
        return appDetectionCounts;
    }
    
    public void setAppDetectionCounts(Map<String, Integer> appDetectionCounts) {
        this.appDetectionCounts = appDetectionCounts;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public long getProcessingDurationMs() {
        if (endTime == 0) {
            return System.currentTimeMillis() - startTime;
        }
        return endTime - startTime;
    }
    
    /**
     * Set additional data for enhanced processing results
     * 
     * @param key Data key
     * @param value Data value
     */
    public void setAdditionalData(String key, Object value) {
        if (additionalData == null) {
            additionalData = new HashMap<>();
        }
        additionalData.put(key, value);
    }
    
    /**
     * Get additional data by key
     * 
     * @param key Data key
     * @return Data value or null if not found
     */
    public Object getAdditionalData(String key) {
        if (additionalData == null) {
            return null;
        }
        return additionalData.get(key);
    }
    
    /**
     * Get all additional data
     * 
     * @return Map of all additional data
     */
    public Map<String, Object> getAllAdditionalData() {
        if (additionalData == null) {
            additionalData = new HashMap<>();
        }
        return additionalData;
    }
    
    @Override
    public String toString() {
        return "VideoProcessingResult{" +
                "processId='" + processId + '\'' +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", progress=" + progress +
                ", dominantApp='" + dominantAppName + '\'' +
                ", additionalDataKeys=" + (additionalData != null ? additionalData.keySet() : "none") +
                '}';
    }
}
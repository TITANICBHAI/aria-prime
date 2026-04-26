package com.aiassistant.learning.video;

import java.io.Serializable;

/**
 * Status of a video processing operation
 */
public enum VideoProcessingStatus implements Serializable {
    PENDING("pending"),
    PROCESSING("processing"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELLED("cancelled");
    
    private final String value;
    
    VideoProcessingStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
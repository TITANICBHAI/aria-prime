package com.aiassistant.detection;

/**
 * Configuration options for the object detector.
 */
public class ObjectDetectorOptions {
    
    /**
     * Detection mode for the object detector
     */
    public enum DetectionMode {
        SINGLE_IMAGE,
        STREAM
    }
    
    private final DetectionMode detectionMode;
    private final float scoreThreshold;
    private final int maxResults;
    private final boolean isMultipleObjectsEnabled;
    private final boolean isClassificationEnabled;
    
    private ObjectDetectorOptions(Builder builder) {
        this.detectionMode = builder.detectionMode;
        this.scoreThreshold = builder.scoreThreshold;
        this.maxResults = builder.maxResults;
        this.isMultipleObjectsEnabled = builder.isMultipleObjectsEnabled;
        this.isClassificationEnabled = builder.isClassificationEnabled;
    }
    
    /**
     * Get detection mode
     * 
     * @return Detection mode
     */
    public DetectionMode getDetectionMode() {
        return detectionMode;
    }
    
    /**
     * Get score threshold
     * 
     * @return Score threshold
     */
    public float getScoreThreshold() {
        return scoreThreshold;
    }
    
    /**
     * Get maximum results
     * 
     * @return Maximum results
     */
    public int getMaxResults() {
        return maxResults;
    }
    
    /**
     * Check if multiple objects detection is enabled
     * 
     * @return true if enabled
     */
    public boolean isMultipleObjectsEnabled() {
        return isMultipleObjectsEnabled;
    }
    
    /**
     * Check if classification is enabled
     * 
     * @return true if enabled
     */
    public boolean isClassificationEnabled() {
        return isClassificationEnabled;
    }
    
    /**
     * Builder for ObjectDetectorOptions
     */
    public static class Builder {
        private DetectionMode detectionMode = DetectionMode.SINGLE_IMAGE;
        private float scoreThreshold = 0.5f;
        private int maxResults = 10;
        private boolean isMultipleObjectsEnabled = true;
        private boolean isClassificationEnabled = true;
        
        /**
         * Set detection mode
         * 
         * @param detectionMode Detection mode
         * @return This Builder instance
         */
        public Builder setDetectionMode(DetectionMode detectionMode) {
            this.detectionMode = detectionMode;
            return this;
        }
        
        /**
         * Set score threshold
         * 
         * @param scoreThreshold Score threshold (0.0-1.0)
         * @return This Builder instance
         */
        public Builder setScoreThreshold(float scoreThreshold) {
            this.scoreThreshold = scoreThreshold;
            return this;
        }
        
        /**
         * Set maximum results
         * 
         * @param maxResults Maximum number of results
         * @return This Builder instance
         */
        public Builder setMaxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }
        
        /**
         * Set whether multiple objects detection is enabled
         * 
         * @param enabled Whether enabled
         * @return This Builder instance
         */
        public Builder setMultipleObjectsEnabled(boolean enabled) {
            this.isMultipleObjectsEnabled = enabled;
            return this;
        }
        
        /**
         * Set whether classification is enabled
         * 
         * @param enabled Whether enabled
         * @return This Builder instance
         */
        public Builder setClassificationEnabled(boolean enabled) {
            this.isClassificationEnabled = enabled;
            return this;
        }
        
        /**
         * Build ObjectDetectorOptions instance
         * 
         * @return ObjectDetectorOptions instance
         */
        public ObjectDetectorOptions build() {
            return new ObjectDetectorOptions(this);
        }
    }
}
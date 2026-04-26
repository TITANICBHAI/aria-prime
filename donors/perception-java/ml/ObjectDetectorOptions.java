package com.aiassistant.ml;

import androidx.annotation.NonNull;

/**
 * Configuration options for the object detector
 */
public class ObjectDetectorOptions {
    private final float scoreThreshold;
    private final int maxResults;
    private final boolean useGpu;
    private final int numThreads;
    private final boolean enableClassification;
    private final boolean enableMultipleObjects;
    
    /**
     * Create ObjectDetectorOptions with specific settings
     * 
     * @param scoreThreshold Confidence score threshold (0.0-1.0)
     * @param maxResults Maximum number of results to return
     * @param useGpu Whether to use GPU acceleration
     * @param numThreads Number of threads to use
     * @param enableClassification Whether to enable object classification
     * @param enableMultipleObjects Whether to detect multiple objects
     */
    public ObjectDetectorOptions(
            float scoreThreshold,
            int maxResults,
            boolean useGpu,
            int numThreads,
            boolean enableClassification,
            boolean enableMultipleObjects) {
        this.scoreThreshold = scoreThreshold;
        this.maxResults = maxResults;
        this.useGpu = useGpu;
        this.numThreads = numThreads;
        this.enableClassification = enableClassification;
        this.enableMultipleObjects = enableMultipleObjects;
    }
    
    /**
     * Create ObjectDetectorOptions with default settings
     */
    public ObjectDetectorOptions() {
        this(0.5f, 10, false, 2, true, true);
    }

    public float getScoreThreshold() {
        return scoreThreshold;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public boolean isUseGpu() {
        return useGpu;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public boolean isEnableClassification() {
        return enableClassification;
    }

    public boolean isEnableMultipleObjects() {
        return enableMultipleObjects;
    }
    
    /**
     * Create a new ObjectDetectorOptions with updated score threshold
     * 
     * @param threshold New threshold value
     * @return New ObjectDetectorOptions instance
     */
    @NonNull
    public ObjectDetectorOptions withScoreThreshold(float threshold) {
        return new ObjectDetectorOptions(
                threshold, maxResults, useGpu, numThreads, 
                enableClassification, enableMultipleObjects);
    }
    
    /**
     * Create a new ObjectDetectorOptions with updated max results
     * 
     * @param max New max results value
     * @return New ObjectDetectorOptions instance
     */
    @NonNull
    public ObjectDetectorOptions withMaxResults(int max) {
        return new ObjectDetectorOptions(
                scoreThreshold, max, useGpu, numThreads, 
                enableClassification, enableMultipleObjects);
    }
    
    /**
     * Create a new ObjectDetectorOptions with updated GPU usage setting
     * 
     * @param use Whether to use GPU
     * @return New ObjectDetectorOptions instance
     */
    @NonNull
    public ObjectDetectorOptions withGpuUsage(boolean use) {
        return new ObjectDetectorOptions(
                scoreThreshold, maxResults, use, numThreads, 
                enableClassification, enableMultipleObjects);
    }
    
    /**
     * Create a new ObjectDetectorOptions with updated thread count
     * 
     * @param threads Number of threads to use
     * @return New ObjectDetectorOptions instance
     */
    @NonNull
    public ObjectDetectorOptions withNumThreads(int threads) {
        return new ObjectDetectorOptions(
                scoreThreshold, maxResults, useGpu, threads, 
                enableClassification, enableMultipleObjects);
    }
    
    /**
     * Create a new ObjectDetectorOptions with updated classification setting
     * 
     * @param enable Whether to enable classification
     * @return New ObjectDetectorOptions instance
     */
    @NonNull
    public ObjectDetectorOptions withClassification(boolean enable) {
        return new ObjectDetectorOptions(
                scoreThreshold, maxResults, useGpu, numThreads, 
                enable, enableMultipleObjects);
    }
    
    /**
     * Create a new ObjectDetectorOptions with updated multiple objects setting
     * 
     * @param enable Whether to enable multiple object detection
     * @return New ObjectDetectorOptions instance
     */
    @NonNull
    public ObjectDetectorOptions withMultipleObjects(boolean enable) {
        return new ObjectDetectorOptions(
                scoreThreshold, maxResults, useGpu, numThreads, 
                enableClassification, enable);
    }

    @Override
    public String toString() {
        return "ObjectDetectorOptions{" +
                "scoreThreshold=" + scoreThreshold +
                ", maxResults=" + maxResults +
                ", useGpu=" + useGpu +
                ", numThreads=" + numThreads +
                ", enableClassification=" + enableClassification +
                ", enableMultipleObjects=" + enableMultipleObjects +
                '}';
    }
    
    /**
     * Builder class for ObjectDetectorOptions
     */
    public static class Builder {
        private float scoreThreshold = 0.5f;
        private int maxResults = 10;
        private boolean useGpu = false;
        private int numThreads = 2;
        private boolean enableClassification = true;
        private boolean enableMultipleObjects = true;
        
        /**
         * Set the confidence score threshold
         * 
         * @param threshold Confidence score threshold (0.0-1.0)
         * @return This Builder instance
         */
        @NonNull
        public Builder setScoreThreshold(float threshold) {
            this.scoreThreshold = threshold;
            return this;
        }
        
        /**
         * Set the maximum number of results
         * 
         * @param max Maximum number of results to return
         * @return This Builder instance
         */
        @NonNull
        public Builder setMaxResults(int max) {
            this.maxResults = max;
            return this;
        }
        
        /**
         * Set whether to use GPU acceleration
         * 
         * @param use Whether to use GPU
         * @return This Builder instance
         */
        @NonNull
        public Builder setUseGpu(boolean use) {
            this.useGpu = use;
            return this;
        }
        
        /**
         * Set the number of threads to use
         * 
         * @param threads Number of threads
         * @return This Builder instance
         */
        @NonNull
        public Builder setNumThreads(int threads) {
            this.numThreads = threads;
            return this;
        }
        
        /**
         * Set whether to enable object classification
         * 
         * @param enable Whether to enable classification
         * @return This Builder instance
         */
        @NonNull
        public Builder setEnableClassification(boolean enable) {
            this.enableClassification = enable;
            return this;
        }
        
        /**
         * Set whether to detect multiple objects
         * 
         * @param enable Whether to enable multiple object detection
         * @return This Builder instance
         */
        @NonNull
        public Builder setEnableMultipleObjects(boolean enable) {
            this.enableMultipleObjects = enable;
            return this;
        }
        
        /**
         * Build the ObjectDetectorOptions
         * 
         * @return ObjectDetectorOptions instance
         */
        @NonNull
        public ObjectDetectorOptions build() {
            return new ObjectDetectorOptions(
                    scoreThreshold, maxResults, useGpu, numThreads,
                    enableClassification, enableMultipleObjects);
        }
    }
}
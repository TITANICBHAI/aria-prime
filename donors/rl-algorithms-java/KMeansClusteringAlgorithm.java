package com.aiassistant.core.ai.algorithms;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * K-means clustering algorithm implementation for pattern recognition
 * and data analysis.
 */
public class KMeansClusteringAlgorithm {
    private static final String TAG = "KMeansClustering";
    
    // Maximum iterations to prevent infinite loops
    private static final int MAX_ITERATIONS = 100;
    
    // Convergence threshold (if centroids move less than this, consider converged)
    private static final float CONVERGENCE_THRESHOLD = 0.01f;
    
    // Number of clusters to identify
    private final int k;
    
    // Random generator for initialization
    private final Random random = new Random();
    
    /**
     * Constructor with number of clusters
     * @param k Number of clusters to identify
     */
    public KMeansClusteringAlgorithm(int k) {
        this.k = Math.max(2, k); // At least 2 clusters
    }
    
    /**
     * Run clustering on a set of feature vectors
     * @param featureVectors List of feature vectors to cluster
     * @return Array of cluster assignments (indices correspond to featureVectors indices)
     */
    public int[] cluster(List<float[]> featureVectors) {
        if (featureVectors == null || featureVectors.isEmpty() || 
            featureVectors.size() < k) {
            Log.w(TAG, "Not enough data points for clustering");
            return new int[0];
        }
        
        int numPoints = featureVectors.size();
        int numDimensions = featureVectors.get(0).length;
        
        // Initialize cluster assignments
        int[] assignments = new int[numPoints];
        
        // Initialize centroids (using k-means++ strategy for better initial centroids)
        List<float[]> centroids = initializeCentroids(featureVectors, numDimensions);
        
        // Main clustering loop
        boolean converged = false;
        int iteration = 0;
        
        while (!converged && iteration < MAX_ITERATIONS) {
            // Assign points to nearest centroid
            boolean assignmentChanged = assignPointsToClusters(featureVectors, centroids, assignments);
            
            // Update centroids based on new assignments
            float totalCentroidMovement = updateCentroids(featureVectors, centroids, assignments, numDimensions);
            
            // Check for convergence
            converged = !assignmentChanged || totalCentroidMovement < CONVERGENCE_THRESHOLD;
            
            iteration++;
        }
        
        Log.d(TAG, "K-means clustering converged after " + iteration + " iterations");
        
        return assignments;
    }
    
    /**
     * Initialize centroids using k-means++ strategy
     */
    private List<float[]> initializeCentroids(List<float[]> featureVectors, int numDimensions) {
        List<float[]> centroids = new ArrayList<>();
        
        // Choose first centroid randomly
        int firstIndex = random.nextInt(featureVectors.size());
        float[] firstCentroid = new float[numDimensions];
        System.arraycopy(featureVectors.get(firstIndex), 0, firstCentroid, 0, numDimensions);
        centroids.add(firstCentroid);
        
        // Choose remaining centroids using k-means++ strategy
        for (int i = 1; i < k; i++) {
            // Compute squared distances to the nearest centroid for each point
            float[] distances = new float[featureVectors.size()];
            float totalDistance = 0;
            
            for (int j = 0; j < featureVectors.size(); j++) {
                float[] point = featureVectors.get(j);
                float minDist = Float.MAX_VALUE;
                
                // Find distance to closest centroid
                for (float[] centroid : centroids) {
                    float dist = calculateSquaredDistance(point, centroid);
                    if (dist < minDist) {
                        minDist = dist;
                    }
                }
                
                distances[j] = minDist;
                totalDistance += minDist;
            }
            
            // Choose next centroid with probability proportional to squared distance
            float targetDistance = random.nextFloat() * totalDistance;
            float currentDistance = 0;
            int nextCentroidIndex = -1;
            
            for (int j = 0; j < distances.length; j++) {
                currentDistance += distances[j];
                if (currentDistance >= targetDistance) {
                    nextCentroidIndex = j;
                    break;
                }
            }
            
            // If we couldn't find a point (due to floating point issues), take the last one
            if (nextCentroidIndex == -1) {
                nextCentroidIndex = distances.length - 1;
            }
            
            // Add the new centroid
            float[] nextCentroid = new float[numDimensions];
            System.arraycopy(featureVectors.get(nextCentroidIndex), 0, nextCentroid, 0, numDimensions);
            centroids.add(nextCentroid);
        }
        
        return centroids;
    }
    
    /**
     * Assign each point to the nearest centroid
     * @return True if any assignment changed
     */
    private boolean assignPointsToClusters(List<float[]> points, List<float[]> centroids, 
                                           int[] assignments) {
        boolean changed = false;
        
        for (int i = 0; i < points.size(); i++) {
            float[] point = points.get(i);
            
            // Find closest centroid
            int closestCentroidIndex = 0;
            float minDistance = Float.MAX_VALUE;
            
            for (int j = 0; j < centroids.size(); j++) {
                float distance = calculateSquaredDistance(point, centroids.get(j));
                
                if (distance < minDistance) {
                    minDistance = distance;
                    closestCentroidIndex = j;
                }
            }
            
            // Update assignment if changed
            if (assignments[i] != closestCentroidIndex) {
                assignments[i] = closestCentroidIndex;
                changed = true;
            }
        }
        
        return changed;
    }
    
    /**
     * Update centroids based on point assignments
     * @return Total centroid movement
     */
    private float updateCentroids(List<float[]> points, List<float[]> centroids, 
                                  int[] assignments, int numDimensions) {
        // Count points in each cluster and initialize sum arrays
        int[] clusterSizes = new int[k];
        float[][] sums = new float[k][numDimensions];
        
        // Calculate sum for each dimension in each cluster
        for (int i = 0; i < points.size(); i++) {
            int clusterIndex = assignments[i];
            float[] point = points.get(i);
            
            clusterSizes[clusterIndex]++;
            
            for (int d = 0; d < numDimensions; d++) {
                sums[clusterIndex][d] += point[d];
            }
        }
        
        // Calculate new centroids and total movement
        float totalMovement = 0;
        
        for (int i = 0; i < k; i++) {
            if (clusterSizes[i] > 0) {
                float[] oldCentroid = centroids.get(i);
                float[] newCentroid = new float[numDimensions];
                
                for (int d = 0; d < numDimensions; d++) {
                    newCentroid[d] = sums[i][d] / clusterSizes[i];
                    totalMovement += Math.abs(newCentroid[d] - oldCentroid[d]);
                }
                
                // Update centroid
                System.arraycopy(newCentroid, 0, oldCentroid, 0, numDimensions);
            }
        }
        
        return totalMovement;
    }
    
    /**
     * Calculate squared Euclidean distance between two vectors
     */
    private float calculateSquaredDistance(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have the same dimensions");
        }
        
        float sum = 0;
        for (int i = 0; i < a.length; i++) {
            float diff = a[i] - b[i];
            sum += diff * diff;
        }
        
        return sum;
    }
    
    /**
     * Get cluster statistics (sizes, variances, etc.)
     * @param points Data points
     * @param assignments Cluster assignments
     * @return Array of cluster sizes
     */
    public int[] getClusterSizes(List<float[]> points, int[] assignments) {
        if (points == null || assignments == null || points.size() != assignments.length) {
            return new int[0];
        }
        
        int[] sizes = new int[k];
        
        for (int assignment : assignments) {
            if (assignment >= 0 && assignment < k) {
                sizes[assignment]++;
            }
        }
        
        return sizes;
    }
}

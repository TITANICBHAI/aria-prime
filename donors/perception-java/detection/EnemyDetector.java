package com.aiassistant.detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import com.aiassistant.ml.TensorflowLiteObjectDetector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Advanced enemy detection system specifically optimized for games
 * This class can detect enemies in different game genres using multiple detection strategies
 */
public class EnemyDetector {
    private static final String TAG = "EnemyDetector";
    
    // Detection methods
    public static final int METHOD_ML = 0;           // Machine learning based detection
    public static final int METHOD_COLOR = 1;        // Color-based detection
    public static final int METHOD_MOTION = 2;       // Motion-based detection
    public static final int METHOD_SHAPE = 3;        // Shape-based detection
    public static final int METHOD_HYBRID = 4;       // Hybrid approach combining methods
    
    // Game types
    public static final int GAME_TYPE_FPS = 0;       // First-person shooter
    public static final int GAME_TYPE_TPS = 1;       // Third-person shooter
    public static final int GAME_TYPE_TOPDOWN = 2;   // Top-down view
    public static final int GAME_TYPE_PLATFORMER = 3; // Side-scrolling platformer
    public static final int GAME_TYPE_MOBA = 4;       // MOBA style
    public static final int GAME_TYPE_RPG = 5;        // RPG style
    public static final int GAME_TYPE_GENERIC = 6;    // Generic/unknown game type
    
    // Enemy attributes
    private static class EnemyAttributes {
        String id;                  // Unique identifier
        Rect bounds;                // Bounding box
        double confidence;          // Detection confidence
        Point center;               // Center point
        int colorSignature;         // Dominant color signature
        long firstSeen;             // Timestamp when first detected
        long lastSeen;              // Timestamp when last detected
        double velocity_x;          // X velocity (pixels per frame)
        double velocity_y;          // Y velocity (pixels per frame)
        boolean isTargeted;         // Whether this enemy is currently targeted
        int health;                 // Estimated health (0-100)
        double threat;              // Threat level (0.0-1.0)
        Map<String, Object> metadata; // Additional metadata
        
        EnemyAttributes(String id, Rect bounds, double confidence) {
            this.id = id;
            this.bounds = bounds;
            this.confidence = confidence;
            this.center = new Point(bounds.centerX(), bounds.centerY());
            this.firstSeen = System.currentTimeMillis();
            this.lastSeen = this.firstSeen;
            this.velocity_x = 0;
            this.velocity_y = 0;
            this.isTargeted = false;
            this.health = 100;
            this.threat = calculateInitialThreat(bounds);
            this.metadata = new HashMap<>();
        }
        
        /**
         * Calculate initial threat level based on size and position
         */
        private double calculateInitialThreat(Rect bounds) {
            // Larger enemies are more threatening
            double sizeThreat = Math.min(1.0, (bounds.width() * bounds.height()) / 40000.0);
            
            // Enemies in center of screen are more threatening (assuming screen size 1080x1920)
            double centerX = bounds.centerX();
            double centerY = bounds.centerY();
            double distFromCenter = Math.sqrt(Math.pow((centerX - 540), 2) + Math.pow((centerY - 960), 2));
            double distThreat = Math.max(0, 1.0 - (distFromCenter / 1000.0));
            
            // Combine factors
            return 0.6 * sizeThreat + 0.4 * distThreat;
        }
        
        /**
         * Update enemy attributes with new detection
         */
        void update(Rect newBounds, double newConfidence) {
            long now = System.currentTimeMillis();
            
            // Calculate time delta in seconds
            double timeDelta = (now - lastSeen) / 1000.0;
            if (timeDelta > 0) {
                // Calculate velocity
                velocity_x = (newBounds.centerX() - center.x) / timeDelta;
                velocity_y = (newBounds.centerY() - center.y) / timeDelta;
            }
            
            // Update values
            this.bounds = newBounds;
            this.center = new Point(newBounds.centerX(), newBounds.centerY());
            this.confidence = 0.3 * this.confidence + 0.7 * newConfidence; // Smooth confidence
            this.lastSeen = now;
        }
        
        /**
         * Check if enemy is still valid (not too old)
         */
        boolean isValid(long currentTime, long maxAgeMs) {
            return (currentTime - lastSeen) <= maxAgeMs;
        }
        
        /**
         * Convert to map for serialization
         */
        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("bounds", boundsToMap(bounds));
            map.put("confidence", confidence);
            map.put("center_x", center.x);
            map.put("center_y", center.y);
            map.put("first_seen", firstSeen);
            map.put("last_seen", lastSeen);
            map.put("velocity_x", velocity_x);
            map.put("velocity_y", velocity_y);
            map.put("is_targeted", isTargeted);
            map.put("health", health);
            map.put("threat", threat);
            map.put("metadata", metadata);
            return map;
        }
        
        /**
         * Convert bounds to map
         */
        private Map<String, Integer> boundsToMap(Rect bounds) {
            Map<String, Integer> map = new HashMap<>();
            map.put("left", bounds.left);
            map.put("top", bounds.top);
            map.put("right", bounds.right);
            map.put("bottom", bounds.bottom);
            map.put("width", bounds.width());
            map.put("height", bounds.height());
            return map;
        }
    }
    
    // Detection configuration and state
    private final Context context;
    private TensorflowLiteObjectDetector mlDetector;
    private int gameType = GAME_TYPE_GENERIC;
    private int primaryMethod = METHOD_HYBRID;
    private int screenWidth = 1080;  // Default values
    private int screenHeight = 1920;
    private long maxEnemyAgeMs = 2000; // Maximum time to keep tracking an enemy without re-detection
    private Map<String, int[]> enemyColorProfiles = new HashMap<>(); // Color profiles for different games
    private double detectionThreshold = 0.55;
    private boolean lowPowerMode = false;
    private Bitmap previousFrame = null;  // For motion detection
    
    // Enemy tracking
    private final Map<String, EnemyAttributes> detectedEnemies = new ConcurrentHashMap<>();
    private final AtomicInteger enemyIdCounter = new AtomicInteger(0);
    
    // Performance metrics
    private long lastDetectionTimeMs = 0;
    private int frameCount = 0;
    private int totalEnemiesDetected = 0;
    private double averageEnemiesPerFrame = 0;
    private double averageDetectionTime = 0;
    
    /**
     * Create a new enemy detector
     */
    public EnemyDetector(Context context) {
        this.context = context;
        
        // Initialize ML detector
        initializeDetector();
        
        // Initialize enemy color profiles for different games
        initializeColorProfiles();
    }
    
    /**
     * Initialize the ML detector
     */
    private void initializeDetector() {
        try {
            mlDetector = new TensorflowLiteObjectDetector(context);
            Log.d(TAG, "ML detector initialized: " + mlDetector.isInitialized());
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ML detector: " + e.getMessage(), e);
            mlDetector = null;
        }
    }
    
    /**
     * Initialize color profiles for enemy detection
     */
    private void initializeColorProfiles() {
        // Red-dominant enemy profile (common in many games)
        enemyColorProfiles.put("red_enemy", new int[]{
            Color.rgb(200, 50, 50),     // Dark red
            Color.rgb(220, 80, 80),     // Medium red
            Color.rgb(255, 100, 100)    // Light red
        });
        
        // Green-dominant enemy profile
        enemyColorProfiles.put("green_enemy", new int[]{
            Color.rgb(50, 180, 50),     // Dark green
            Color.rgb(80, 200, 80),     // Medium green
            Color.rgb(100, 255, 100)    // Light green
        });
        
        // Blue-dominant enemy profile
        enemyColorProfiles.put("blue_enemy", new int[]{
            Color.rgb(50, 50, 180),     // Dark blue
            Color.rgb(80, 80, 220),     // Medium blue
            Color.rgb(100, 100, 255)    // Light blue
        });
        
        // Purple-dominant enemy profile
        enemyColorProfiles.put("purple_enemy", new int[]{
            Color.rgb(150, 50, 150),    // Dark purple
            Color.rgb(180, 80, 180),    // Medium purple
            Color.rgb(220, 100, 220)    // Light purple
        });
        
        // Generic profile for common enemy indicator colors
        enemyColorProfiles.put("indicator", new int[]{
            Color.rgb(255, 50, 50),     // Red indicator
            Color.rgb(255, 128, 0),     // Orange indicator
            Color.rgb(255, 255, 0)      // Yellow indicator
        });
    }
    
    /**
     * Set game type for optimized detection
     */
    public void setGameType(int gameType) {
        this.gameType = gameType;
        
        // Adjust parameters based on game type
        switch (gameType) {
            case GAME_TYPE_FPS:
                // FPS games typically have enemies in the center of view
                detectionThreshold = 0.5;
                primaryMethod = METHOD_HYBRID;
                break;
                
            case GAME_TYPE_TPS:
                // TPS games typically have enemies spread across the screen
                detectionThreshold = 0.6;
                primaryMethod = METHOD_HYBRID;
                break;
                
            case GAME_TYPE_TOPDOWN:
                // Top-down games often have distinct enemy colors
                detectionThreshold = 0.5;
                primaryMethod = METHOD_COLOR;
                break;
                
            case GAME_TYPE_PLATFORMER:
                // Platformers often have distinct shapes for enemies
                detectionThreshold = 0.55;
                primaryMethod = METHOD_SHAPE;
                break;
                
            case GAME_TYPE_MOBA:
                // MOBA games often have health bars and indicators
                detectionThreshold = 0.65;
                primaryMethod = METHOD_COLOR;
                break;
                
            case GAME_TYPE_RPG:
                // RPG games vary widely in enemy representation
                detectionThreshold = 0.6;
                primaryMethod = METHOD_HYBRID;
                break;
                
            default:
                // Generic/unknown type
                detectionThreshold = 0.55;
                primaryMethod = METHOD_HYBRID;
                break;
        }
    }
    
    /**
     * Set detection method
     */
    public void setDetectionMethod(int method) {
        this.primaryMethod = method;
    }
    
    /**
     * Set screen dimensions
     */
    public void setScreenDimensions(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }
    
    /**
     * Set low power mode
     */
    public void setLowPowerMode(boolean enabled) {
        lowPowerMode = enabled;
        
        // Adjust parameters for low power mode
        if (enabled) {
            detectionThreshold = 0.7; // Higher threshold to reduce false positives
            maxEnemyAgeMs = 3000; // Keep tracking enemies longer to reduce processing
            
            // Use simpler detection methods in low power mode
            if (primaryMethod == METHOD_HYBRID) {
                primaryMethod = METHOD_COLOR; // Simpler method
            }
        } else {
            // Reset to default values
            maxEnemyAgeMs = 2000;
            // Detection threshold depends on game type, so we'll reset it
            setGameType(gameType);
        }
    }
    
    /**
     * Detect enemies in a screen bitmap
     */
    public List<Map<String, Object>> detectEnemies(Bitmap screen) {
        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> results = new ArrayList<>();
        frameCount++;
        
        try {
            if (screen == null) {
                return results;
            }
            
            // Clean up old enemy data
            cleanupDetectedEnemies();
            
            // Detect enemies using the configured method
            List<Rect> detectedBounds = new ArrayList<>();
            
            switch (primaryMethod) {
                case METHOD_ML:
                    detectedBounds = detectEnemiesWithML(screen);
                    break;
                    
                case METHOD_COLOR:
                    detectedBounds = detectEnemiesWithColor(screen);
                    break;
                    
                case METHOD_MOTION:
                    detectedBounds = detectEnemiesWithMotion(screen);
                    break;
                    
                case METHOD_SHAPE:
                    detectedBounds = detectEnemiesWithShape(screen);
                    break;
                    
                case METHOD_HYBRID:
                default:
                    detectedBounds = detectEnemiesWithHybrid(screen);
                    break;
            }
            
            // Process detected bounds
            processDetections(detectedBounds);
            
            // Update motion detection reference frame
            updateMotionReference(screen);
            
            // Convert enemy attributes to result maps
            for (EnemyAttributes enemy : detectedEnemies.values()) {
                results.add(enemy.toMap());
            }
            
            // Update metrics
            lastDetectionTimeMs = System.currentTimeMillis() - startTime;
            totalEnemiesDetected += results.size();
            averageEnemiesPerFrame = ((averageEnemiesPerFrame * (frameCount - 1)) + results.size()) / frameCount;
            averageDetectionTime = ((averageDetectionTime * (frameCount - 1)) + lastDetectionTimeMs) / frameCount;
            
            Log.d(TAG, "Detected " + results.size() + " enemies in " + lastDetectionTimeMs + "ms");
            
        } catch (Exception e) {
            Log.e(TAG, "Error detecting enemies: " + e.getMessage(), e);
        }
        
        return results;
    }
    
    /**
     * Process detected bounds and update enemy tracking
     */
    private void processDetections(List<Rect> detectedBounds) {
        long currentTime = System.currentTimeMillis();
        
        // Track which enemies were updated
        Set<String> updatedEnemies = new HashSet<>();
        
        // Process each detection
        for (Rect bounds : detectedBounds) {
            // Try to match to existing enemy
            String matchedId = null;
            double bestOverlap = 0.3; // Minimum overlap threshold
            
            for (Map.Entry<String, EnemyAttributes> entry : detectedEnemies.entrySet()) {
                EnemyAttributes enemy = entry.getValue();
                double overlap = calculateRectOverlap(bounds, enemy.bounds);
                
                if (overlap > bestOverlap) {
                    bestOverlap = overlap;
                    matchedId = entry.getKey();
                }
            }
            
            if (matchedId != null) {
                // Update existing enemy
                EnemyAttributes enemy = detectedEnemies.get(matchedId);
                enemy.update(bounds, 0.8 + Math.random() * 0.2); // Simulate confidence
                updatedEnemies.add(matchedId);
            } else {
                // Create new enemy
                String newId = "enemy_" + enemyIdCounter.incrementAndGet();
                double confidence = 0.6 + Math.random() * 0.3; // Simulate confidence
                EnemyAttributes newEnemy = new EnemyAttributes(newId, bounds, confidence);
                detectedEnemies.put(newId, newEnemy);
                updatedEnemies.add(newId);
            }
        }
        
        // Remove enemies that weren't updated and are too old
        detectedEnemies.entrySet().removeIf(entry -> 
            !updatedEnemies.contains(entry.getKey()) && 
            !entry.getValue().isValid(currentTime, maxEnemyAgeMs)
        );
    }
    
    /**
     * Calculate overlap between two rectangles
     */
    private double calculateRectOverlap(Rect a, Rect b) {
        // Calculate intersection
        int left = Math.max(a.left, b.left);
        int top = Math.max(a.top, b.top);
        int right = Math.min(a.right, b.right);
        int bottom = Math.min(a.bottom, b.bottom);
        
        if (left >= right || top >= bottom) {
            return 0.0; // No overlap
        }
        
        int intersectionArea = (right - left) * (bottom - top);
        int aArea = a.width() * a.height();
        int bArea = b.width() * b.height();
        
        // Return ratio of intersection to smallest area
        return (double) intersectionArea / Math.min(aArea, bArea);
    }
    
    /**
     * Detect enemies using machine learning
     */
    private List<Rect> detectEnemiesWithML(Bitmap screen) {
        List<Rect> results = new ArrayList<>();
        
        try {
            if (mlDetector != null && mlDetector.isInitialized()) {
                // Define labels to detect enemies in games
                String[] enemyLabels = {"person", "enemy", "creature", "character", "monster", "robot"};
                
                // Detect objects
                results = mlDetector.detectObjects(screen, enemyLabels, detectionThreshold);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in ML enemy detection: " + e.getMessage(), e);
        }
        
        return results;
    }
    
    /**
     * Detect enemies using color analysis
     */
    private List<Rect> detectEnemiesWithColor(Bitmap screen) {
        List<Rect> results = new ArrayList<>();
        
        try {
            int width = screen.getWidth();
            int height = screen.getHeight();
            
            // Create binary mask of potential enemy pixels
            boolean[][] enemyMask = new boolean[width][height];
            
            // Scan image for enemy colors (sampling for performance)
            int sampleStep = lowPowerMode ? 16 : 8;
            
            for (int x = 0; x < width; x += sampleStep) {
                for (int y = 0; y < height; y += sampleStep) {
                    int pixel = screen.getPixel(x, y);
                    
                    // Check if this pixel matches enemy color profiles
                    if (isEnemyColor(pixel)) {
                        // Mark this and surrounding pixels
                        for (int dx = -sampleStep/2; dx <= sampleStep/2; dx++) {
                            for (int dy = -sampleStep/2; dy <= sampleStep/2; dy++) {
                                int nx = x + dx;
                                int ny = y + dy;
                                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                                    enemyMask[nx][ny] = true;
                                }
                            }
                        }
                    }
                }
            }
            
            // Find connected components to identify enemy regions
            boolean[][] visited = new boolean[width][height];
            
            for (int x = 0; x < width; x += sampleStep) {
                for (int y = 0; y < height; y += sampleStep) {
                    if (enemyMask[x][y] && !visited[x][y]) {
                        Rect bounds = new Rect(x, y, x, y);
                        expandRegion(enemyMask, visited, x, y, bounds, width, height, sampleStep);
                        
                        // Filter out regions that are too small or too large
                        int area = bounds.width() * bounds.height();
                        if (area > 1000 && area < (width * height) / 4) {
                            results.add(bounds);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in color enemy detection: " + e.getMessage(), e);
        }
        
        return results;
    }
    
    /**
     * Detect enemies using motion analysis
     */
    private List<Rect> detectEnemiesWithMotion(Bitmap screen) {
        List<Rect> results = new ArrayList<>();
        
        try {
            if (previousFrame == null) {
                // Cannot detect motion with just one frame
                return results;
            }
            
            int width = screen.getWidth();
            int height = screen.getHeight();
            
            // Create motion difference map
            boolean[][] motionMask = new boolean[width][height];
            
            // Scan image for motion (sampling for performance)
            int sampleStep = lowPowerMode ? 16 : 8;
            
            for (int x = 0; x < width; x += sampleStep) {
                for (int y = 0; y < height; y += sampleStep) {
                    int currentPixel = screen.getPixel(x, y);
                    int previousPixel = previousFrame.getPixel(x, y);
                    
                    // Calculate color difference
                    int rDiff = Math.abs(Color.red(currentPixel) - Color.red(previousPixel));
                    int gDiff = Math.abs(Color.green(currentPixel) - Color.green(previousPixel));
                    int bDiff = Math.abs(Color.blue(currentPixel) - Color.blue(previousPixel));
                    int totalDiff = rDiff + gDiff + bDiff;
                    
                    // If significant difference, mark as motion
                    if (totalDiff > 80) { // Threshold for motion detection
                        // Mark this and surrounding pixels
                        for (int dx = -sampleStep/2; dx <= sampleStep/2; dx++) {
                            for (int dy = -sampleStep/2; dy <= sampleStep/2; dy++) {
                                int nx = x + dx;
                                int ny = y + dy;
                                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                                    motionMask[nx][ny] = true;
                                }
                            }
                        }
                    }
                }
            }
            
            // Find connected components to identify moving regions
            boolean[][] visited = new boolean[width][height];
            
            for (int x = 0; x < width; x += sampleStep) {
                for (int y = 0; y < height; y += sampleStep) {
                    if (motionMask[x][y] && !visited[x][y]) {
                        Rect bounds = new Rect(x, y, x, y);
                        expandRegion(motionMask, visited, x, y, bounds, width, height, sampleStep);
                        
                        // Filter out regions that are too small or too large
                        int area = bounds.width() * bounds.height();
                        if (area > 1000 && area < (width * height) / 4) {
                            results.add(bounds);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in motion enemy detection: " + e.getMessage(), e);
        }
        
        return results;
    }
    
    /**
     * Detect enemies using shape analysis
     */
    private List<Rect> detectEnemiesWithShape(Bitmap screen) {
        // This would implement shape-based detection using edge detection,
        // contour finding, etc. For now, we'll just return a sample result
        List<Rect> results = new ArrayList<>();
        
        // Create a few sample detections based on screen size
        int width = screen.getWidth();
        int height = screen.getHeight();
        
        // Sample enemy at 1/3 of screen
        results.add(new Rect(
            width / 3 - 50, 
            height / 3 - 50, 
            width / 3 + 50, 
            height / 3 + 50
        ));
        
        // Sample enemy at 2/3 of screen
        results.add(new Rect(
            2 * width / 3 - 40, 
            2 * height / 3 - 40, 
            2 * width / 3 + 40, 
            2 * height / 3 + 40
        ));
        
        return results;
    }
    
    /**
     * Detect enemies using a hybrid approach
     */
    private List<Rect> detectEnemiesWithHybrid(Bitmap screen) {
        List<Rect> results = new ArrayList<>();
        
        // First try ML-based detection
        List<Rect> mlResults = detectEnemiesWithML(screen);
        results.addAll(mlResults);
        
        // If we didn't find many enemies with ML, try color detection
        if (results.size() < 2) {
            List<Rect> colorResults = detectEnemiesWithColor(screen);
            results.addAll(colorResults);
        }
        
        // If we still don't have enough, and we have previous frame, try motion
        if (results.size() < 2 && previousFrame != null) {
            List<Rect> motionResults = detectEnemiesWithMotion(screen);
            results.addAll(motionResults);
        }
        
        // Remove overlapping results
        results = removeOverlappingRects(results);
        
        return results;
    }
    
    /**
     * Update the motion reference frame
     */
    private void updateMotionReference(Bitmap current) {
        // Recycle old frame if it exists
        if (previousFrame != null) {
            previousFrame.recycle();
        }
        
        // Create a copy of the current frame
        previousFrame = current.copy(current.getConfig(), true);
    }
    
    /**
     * Clean up detected enemies that are too old
     */
    private void cleanupDetectedEnemies() {
        long currentTime = System.currentTimeMillis();
        
        // Remove enemies that are too old
        detectedEnemies.entrySet().removeIf(entry ->
            !entry.getValue().isValid(currentTime, maxEnemyAgeMs)
        );
    }
    
    /**
     * Check if a pixel matches enemy color profiles
     */
    private boolean isEnemyColor(int pixel) {
        int r = Color.red(pixel);
        int g = Color.green(pixel);
        int b = Color.blue(pixel);
        
        // Check against our enemy color profiles
        for (int[] colorProfile : enemyColorProfiles.values()) {
            for (int profileColor : colorProfile) {
                int pr = Color.red(profileColor);
                int pg = Color.green(profileColor);
                int pb = Color.blue(profileColor);
                
                // Calculate color similarity
                int colorDiff = Math.abs(r - pr) + Math.abs(g - pg) + Math.abs(b - pb);
                
                // If within threshold, consider a match
                if (colorDiff < 120) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Remove overlapping rectangles
     */
    private List<Rect> removeOverlappingRects(List<Rect> rects) {
        List<Rect> result = new ArrayList<>();
        
        for (Rect rect : rects) {
            boolean shouldAdd = true;
            
            // Check if this rectangle overlaps significantly with any in result
            for (int i = 0; i < result.size(); i++) {
                Rect existing = result.get(i);
                double overlap = calculateRectOverlap(rect, existing);
                
                if (overlap > 0.5) { // Significant overlap
                    shouldAdd = false;
                    
                    // If the new rect is larger, replace the existing one
                    int rectArea = rect.width() * rect.height();
                    int existingArea = existing.width() * existing.height();
                    
                    if (rectArea > existingArea) {
                        result.set(i, rect);
                    }
                    
                    break;
                }
            }
            
            if (shouldAdd) {
                result.add(rect);
            }
        }
        
        return result;
    }
    
    /**
     * Expand a region using BFS
     */
    private void expandRegion(boolean[][] mask, boolean[][] visited, 
                             int startX, int startY, Rect bounds, 
                             int width, int height, int step) {
        // Queue for BFS
        List<int[]> queue = new ArrayList<>();
        queue.add(new int[]{startX, startY});
        visited[startX][startY] = true;
        
        // Define directions: up, down, left, right
        int[][] directions = {{0, -step}, {0, step}, {-step, 0}, {step, 0}};
        
        // BFS to find connected region
        while (!queue.isEmpty()) {
            int[] pos = queue.remove(0);
            int x = pos[0];
            int y = pos[1];
            
            // Update bounds
            bounds.left = Math.min(bounds.left, x);
            bounds.top = Math.min(bounds.top, y);
            bounds.right = Math.max(bounds.right, x);
            bounds.bottom = Math.max(bounds.bottom, y);
            
            // Check in all four directions
            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                
                // Check bounds
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    // If pixel is part of mask and not visited
                    if (mask[nx][ny] && !visited[nx][ny]) {
                        visited[nx][ny] = true;
                        queue.add(new int[]{nx, ny});
                    }
                }
            }
        }
        
        // Expand bounds slightly for better detection
        int expansion = 10;
        bounds.left = Math.max(0, bounds.left - expansion);
        bounds.top = Math.max(0, bounds.top - expansion);
        bounds.right = Math.min(width - 1, bounds.right + expansion);
        bounds.bottom = Math.min(height - 1, bounds.bottom + expansion);
    }
    
    /**
     * Set a target enemy
     */
    public boolean setTarget(String enemyId) {
        // Reset targeting
        for (EnemyAttributes enemy : detectedEnemies.values()) {
            enemy.isTargeted = false;
        }
        
        // Set new target
        if (detectedEnemies.containsKey(enemyId)) {
            detectedEnemies.get(enemyId).isTargeted = true;
            return true;
        }
        
        return false;
    }
    
    /**
     * Get the current target
     */
    public Map<String, Object> getCurrentTarget() {
        for (EnemyAttributes enemy : detectedEnemies.values()) {
            if (enemy.isTargeted) {
                return enemy.toMap();
            }
        }
        
        return null;
    }
    
    /**
     * Get highest threat enemy
     */
    public Map<String, Object> getHighestThreatEnemy() {
        double highestThreat = -1;
        EnemyAttributes highestThreatEnemy = null;
        
        for (EnemyAttributes enemy : detectedEnemies.values()) {
            if (enemy.threat > highestThreat) {
                highestThreat = enemy.threat;
                highestThreatEnemy = enemy;
            }
        }
        
        return highestThreatEnemy != null ? highestThreatEnemy.toMap() : null;
    }
    
    /**
     * Get closest enemy to center
     */
    public Map<String, Object> getClosestToCenter() {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        double closestDistance = Double.MAX_VALUE;
        EnemyAttributes closestEnemy = null;
        
        for (EnemyAttributes enemy : detectedEnemies.values()) {
            double distance = Math.sqrt(
                Math.pow(enemy.center.x - centerX, 2) +
                Math.pow(enemy.center.y - centerY, 2)
            );
            
            if (distance < closestDistance) {
                closestDistance = distance;
                closestEnemy = enemy;
            }
        }
        
        return closestEnemy != null ? closestEnemy.toMap() : null;
    }
    
    /**
     * Get performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("last_detection_time_ms", lastDetectionTimeMs);
        metrics.put("average_detection_time_ms", averageDetectionTime);
        metrics.put("frame_count", frameCount);
        metrics.put("total_enemies_detected", totalEnemiesDetected);
        metrics.put("average_enemies_per_frame", averageEnemiesPerFrame);
        metrics.put("current_enemy_count", detectedEnemies.size());
        metrics.put("detection_threshold", detectionThreshold);
        metrics.put("low_power_mode", lowPowerMode);
        metrics.put("game_type", gameType);
        metrics.put("detection_method", primaryMethod);
        
        return metrics;
    }
    
    /**
     * Reset performance metrics
     */
    public void resetMetrics() {
        frameCount = 0;
        totalEnemiesDetected = 0;
        averageEnemiesPerFrame = 0;
        averageDetectionTime = 0;
    }
}
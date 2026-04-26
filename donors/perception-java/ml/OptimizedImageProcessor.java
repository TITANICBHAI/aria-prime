package com.aiassistant.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicResize;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optimized Image Processor for Fast Image Analysis
 * 
 * This class provides GPU-accelerated image processing for faster image analysis
 * in fast-paced games and applications.
 */
public class OptimizedImageProcessor {
    private static final String TAG = "OptimizedImageProcessor";
    
    // Singleton instance
    private static OptimizedImageProcessor instance;
    
    // Context
    private final Context context;
    
    // RenderScript instance
    private RenderScript renderScript;
    
    // Processing configuration
    private float resizeRatio = 1.0f;
    private float blurRadius = 0.0f;
    private boolean convertToGrayscale = false;
    private boolean enableHistogramEqualization = false;
    private boolean enableRegionOfInterestProcessing = false;
    private List<RegionOfInterest> regionsOfInterest = new ArrayList<>();
    
    // Cached allocations for reuse
    private Allocation inputAllocation;
    private Allocation outputAllocation;
    private Bitmap outputBitmap;
    
    // Processing stats
    private long lastProcessingTimeMs = 0;
    private long totalProcessingTimeMs = 0;
    private int frameCount = 0;
    
    // Thread pool for parallel processing
    private final ExecutorService executorService;
    
    // Processing state
    private final AtomicBoolean processing = new AtomicBoolean(false);
    
    /**
     * Region of interest for targeted processing
     */
    public static class RegionOfInterest {
        private final int left;
        private final int top;
        private final int right;
        private final int bottom;
        private final float processingIntensity;
        private final String name;
        
        public RegionOfInterest(int left, int top, int right, int bottom, 
                               float processingIntensity, String name) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.processingIntensity = processingIntensity;
            this.name = name;
        }
        
        public int getLeft() {
            return left;
        }
        
        public int getTop() {
            return top;
        }
        
        public int getRight() {
            return right;
        }
        
        public int getBottom() {
            return bottom;
        }
        
        public float getProcessingIntensity() {
            return processingIntensity;
        }
        
        public String getName() {
            return name;
        }
        
        public int getWidth() {
            return right - left;
        }
        
        public int getHeight() {
            return bottom - top;
        }
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized OptimizedImageProcessor getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new OptimizedImageProcessor(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private OptimizedImageProcessor(Context context) {
        this.context = context;
        this.executorService = Executors.newFixedThreadPool(
                Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
        
        // Initialize RenderScript
        initializeRenderScript();
        
        Log.i(TAG, "Optimized image processor created");
    }
    
    /**
     * Initialize RenderScript
     */
    private void initializeRenderScript() {
        try {
            renderScript = RenderScript.create(context);
            Log.i(TAG, "RenderScript initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize RenderScript: " + e.getMessage());
        }
    }
    
    /**
     * Set resize ratio
     */
    public void setResizeRatio(float ratio) {
        if (ratio <= 0.0f || ratio > 1.0f) {
            Log.w(TAG, "Invalid resize ratio. Using default value.");
            ratio = 1.0f;
        }
        this.resizeRatio = ratio;
    }
    
    /**
     * Set blur radius
     */
    public void setBlurRadius(float radius) {
        if (radius < 0.0f || radius > 25.0f) {
            Log.w(TAG, "Invalid blur radius. Using default value.");
            radius = 0.0f;
        }
        this.blurRadius = radius;
    }
    
    /**
     * Set grayscale conversion
     */
    public void setConvertToGrayscale(boolean convert) {
        this.convertToGrayscale = convert;
    }
    
    /**
     * Set histogram equalization
     */
    public void setEnableHistogramEqualization(boolean enable) {
        this.enableHistogramEqualization = enable;
    }
    
    /**
     * Enable/disable region of interest processing
     */
    public void setEnableRegionOfInterestProcessing(boolean enable) {
        this.enableRegionOfInterestProcessing = enable;
    }
    
    /**
     * Add a region of interest
     */
    public void addRegionOfInterest(RegionOfInterest roi) {
        if (roi != null) {
            regionsOfInterest.add(roi);
        }
    }
    
    /**
     * Clear regions of interest
     */
    public void clearRegionsOfInterest() {
        regionsOfInterest.clear();
    }
    
    /**
     * Process an image using configured settings
     */
    public Bitmap processImage(Bitmap inputImage) {
        if (inputImage == null || inputImage.isRecycled() || renderScript == null) {
            return inputImage;
        }
        
        // Prevent concurrent processing
        if (!processing.compareAndSet(false, true)) {
            return inputImage;
        }
        
        try {
            long startTime = SystemClock.elapsedRealtime();
            
            // Process the image
            Bitmap processedImage = applyProcessing(inputImage);
            
            // Update stats
            lastProcessingTimeMs = SystemClock.elapsedRealtime() - startTime;
            totalProcessingTimeMs += lastProcessingTimeMs;
            frameCount++;
            
            return processedImage;
        } catch (Exception e) {
            Log.e(TAG, "Error processing image: " + e.getMessage());
            return inputImage;
        } finally {
            processing.set(false);
        }
    }
    
    /**
     * Apply processing to the image
     */
    private Bitmap applyProcessing(Bitmap inputImage) {
        Bitmap result = inputImage;
        
        // Resize if needed
        if (resizeRatio < 1.0f) {
            result = resizeImage(result, resizeRatio);
        }
        
        // Apply blur if needed
        if (blurRadius > 0.0f) {
            result = applyBlur(result, blurRadius);
        }
        
        // Convert to grayscale if needed
        if (convertToGrayscale) {
            result = convertToGrayscale(result);
        }
        
        // Apply histogram equalization if needed
        if (enableHistogramEqualization) {
            result = equalizeHistogram(result);
        }
        
        // Process regions of interest if enabled
        if (enableRegionOfInterestProcessing && !regionsOfInterest.isEmpty()) {
            result = processRegionsOfInterest(result);
        }
        
        return result;
    }
    
    /**
     * Resize an image using RenderScript
     */
    private Bitmap resizeImage(Bitmap input, float ratio) {
        if (renderScript == null || ratio >= 1.0f) {
            return input;
        }
        
        try {
            // Calculate new dimensions
            int newWidth = Math.max(1, (int) (input.getWidth() * ratio));
            int newHeight = Math.max(1, (int) (input.getHeight() * ratio));
            
            // Create output bitmap
            Bitmap outputBitmap = Bitmap.createBitmap(newWidth, newHeight, input.getConfig());
            
            // Create allocations
            Allocation inputAlloc = Allocation.createFromBitmap(renderScript, input);
            Allocation outputAlloc = Allocation.createFromBitmap(renderScript, outputBitmap);
            
            // Create script and set parameters
            ScriptIntrinsicResize resizeScript = ScriptIntrinsicResize.create(renderScript);
            resizeScript.setInput(inputAlloc);
            resizeScript.forEach_bicubic(outputAlloc);
            
            // Copy to output
            outputAlloc.copyTo(outputBitmap);
            
            // Clean up
            inputAlloc.destroy();
            outputAlloc.destroy();
            resizeScript.destroy();
            
            return outputBitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error resizing image: " + e.getMessage());
            return input;
        }
    }
    
    /**
     * Apply blur using RenderScript
     */
    private Bitmap applyBlur(Bitmap input, float radius) {
        if (renderScript == null || radius <= 0.0f) {
            return input;
        }
        
        try {
            // Create output bitmap
            Bitmap outputBitmap = Bitmap.createBitmap(
                    input.getWidth(), input.getHeight(), input.getConfig());
            
            // Create allocations
            Allocation inputAlloc = Allocation.createFromBitmap(renderScript, input);
            Allocation outputAlloc = Allocation.createFromBitmap(renderScript, outputBitmap);
            
            // Create script and set parameters
            ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(
                    renderScript, Element.U8_4(renderScript));
            blurScript.setInput(inputAlloc);
            blurScript.setRadius(radius);
            
            // Execute and copy to output
            blurScript.forEach(outputAlloc);
            outputAlloc.copyTo(outputBitmap);
            
            // Clean up
            inputAlloc.destroy();
            outputAlloc.destroy();
            blurScript.destroy();
            
            return outputBitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error applying blur: " + e.getMessage());
            return input;
        }
    }
    
    /**
     * Convert to grayscale
     */
    private Bitmap convertToGrayscale(Bitmap input) {
        if (input == null) {
            return null;
        }
        
        try {
            int width = input.getWidth();
            int height = input.getHeight();
            
            // Create output bitmap
            Bitmap outputBitmap = Bitmap.createBitmap(width, height, input.getConfig());
            
            // Create and execute grayscale conversion (manually since no intrinsic available)
            int[] pixels = new int[width * height];
            input.getPixels(pixels, 0, width, 0, 0, width, height);
            
            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                int alpha = (pixel >> 24) & 0xff;
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;
                
                // Standard grayscale conversion
                int gray = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
                
                // Set the pixel
                pixels[i] = (alpha << 24) | (gray << 16) | (gray << 8) | gray;
            }
            
            outputBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            
            return outputBitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error converting to grayscale: " + e.getMessage());
            return input;
        }
    }
    
    /**
     * Equalize histogram
     */
    private Bitmap equalizeHistogram(Bitmap input) {
        if (input == null) {
            return null;
        }
        
        try {
            int width = input.getWidth();
            int height = input.getHeight();
            
            // Create output bitmap
            Bitmap outputBitmap = Bitmap.createBitmap(width, height, input.getConfig());
            
            // Get pixels
            int[] pixels = new int[width * height];
            input.getPixels(pixels, 0, width, 0, 0, width, height);
            
            // Calculate histogram
            int[] histogramR = new int[256];
            int[] histogramG = new int[256];
            int[] histogramB = new int[256];
            
            for (int pixel : pixels) {
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;
                
                histogramR[red]++;
                histogramG[green]++;
                histogramB[blue]++;
            }
            
            // Calculate cumulative histogram
            int[] cumulativeR = new int[256];
            int[] cumulativeG = new int[256];
            int[] cumulativeB = new int[256];
            
            cumulativeR[0] = histogramR[0];
            cumulativeG[0] = histogramG[0];
            cumulativeB[0] = histogramB[0];
            
            for (int i = 1; i < 256; i++) {
                cumulativeR[i] = cumulativeR[i - 1] + histogramR[i];
                cumulativeG[i] = cumulativeG[i - 1] + histogramG[i];
                cumulativeB[i] = cumulativeB[i - 1] + histogramB[i];
            }
            
            // Normalize cumulative histogram
            float totalPixels = width * height;
            int[] mapR = new int[256];
            int[] mapG = new int[256];
            int[] mapB = new int[256];
            
            for (int i = 0; i < 256; i++) {
                mapR[i] = Math.round((cumulativeR[i] / totalPixels) * 255);
                mapG[i] = Math.round((cumulativeG[i] / totalPixels) * 255);
                mapB[i] = Math.round((cumulativeB[i] / totalPixels) * 255);
            }
            
            // Apply equalization
            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                int alpha = (pixel >> 24) & 0xff;
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;
                
                // Map to equalized values
                red = mapR[red];
                green = mapG[green];
                blue = mapB[blue];
                
                // Set the pixel
                pixels[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
            }
            
            outputBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            
            return outputBitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error equalizing histogram: " + e.getMessage());
            return input;
        }
    }
    
    /**
     * Process regions of interest
     */
    private Bitmap processRegionsOfInterest(Bitmap input) {
        if (input == null || regionsOfInterest.isEmpty()) {
            return input;
        }
        
        try {
            final Bitmap outputBitmap = input.copy(input.getConfig(), true);
            
            // Process each region in parallel
            List<Runnable> tasks = new ArrayList<>();
            
            for (final RegionOfInterest roi : regionsOfInterest) {
                tasks.add(() -> {
                    try {
                        // Extract region
                        Bitmap region = Bitmap.createBitmap(
                                input,
                                roi.getLeft(),
                                roi.getTop(),
                                roi.getWidth(),
                                roi.getHeight());
                        
                        // Process region with higher intensity if needed
                        if (roi.getProcessingIntensity() > 1.0f) {
                            // Apply more aggressive processing to this region
                            if (blurRadius > 0) {
                                region = applyBlur(region, blurRadius * roi.getProcessingIntensity());
                            }
                            
                            if (enableHistogramEqualization) {
                                region = equalizeHistogram(region);
                            }
                        }
                        
                        // Copy processed region back to output
                        int[] pixels = new int[roi.getWidth() * roi.getHeight()];
                        region.getPixels(pixels, 0, roi.getWidth(), 0, 0, 
                                         roi.getWidth(), roi.getHeight());
                        
                        synchronized (outputBitmap) {
                            outputBitmap.setPixels(pixels, 0, roi.getWidth(), 
                                                 roi.getLeft(), roi.getTop(), 
                                                 roi.getWidth(), roi.getHeight());
                        }
                        
                        // Clean up
                        if (region != input) {
                            region.recycle();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing region " + roi.getName() + 
                                 ": " + e.getMessage());
                    }
                });
            }
            
            // Execute tasks in parallel
            try {
                for (Runnable task : tasks) {
                    executorService.execute(task);
                }
                
                // Wait for completion (with timeout)
                Thread.sleep(50); // Short wait time for parallel processing
            } catch (InterruptedException e) {
                Log.e(TAG, "Region processing interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
            
            return outputBitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error processing regions of interest: " + e.getMessage());
            return input;
        }
    }
    
    /**
     * Get processing statistics
     */
    public Map<String, Object> getProcessingStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("lastProcessingTimeMs", lastProcessingTimeMs);
        stats.put("avgProcessingTimeMs", frameCount > 0 ? 
                (float) totalProcessingTimeMs / frameCount : 0);
        stats.put("frameCount", frameCount);
        stats.put("resizeRatio", resizeRatio);
        stats.put("blurRadius", blurRadius);
        stats.put("convertToGrayscale", convertToGrayscale);
        stats.put("enableHistogramEqualization", enableHistogramEqualization);
        stats.put("regionsOfInterestCount", regionsOfInterest.size());
        
        return stats;
    }
    
    /**
     * Reset processing statistics
     */
    public void resetStats() {
        lastProcessingTimeMs = 0;
        totalProcessingTimeMs = 0;
        frameCount = 0;
    }
    
    /**
     * Release resources
     */
    public void release() {
        // Shutdown executor
        executorService.shutdown();
        
        // Clean up cached allocations
        if (inputAllocation != null) {
            inputAllocation.destroy();
            inputAllocation = null;
        }
        
        if (outputAllocation != null) {
            outputAllocation.destroy();
            outputAllocation = null;
        }
        
        if (outputBitmap != null && !outputBitmap.isRecycled()) {
            outputBitmap.recycle();
            outputBitmap = null;
        }
        
        // Destroy RenderScript context
        if (renderScript != null) {
            renderScript.destroy();
            renderScript = null;
        }
        
        Log.i(TAG, "Optimized image processor released");
    }
}
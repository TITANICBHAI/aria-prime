package com.aiassistant.detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
// To avoid naming conflicts with our own ObjectDetector class, we don't import MLKit's ObjectDetector
// but use the fully qualified name: com.google.mlkit.vision.objects.ObjectDetector

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Detects objects in a bitmap using ML Kit's object detection API
 * Optimized for detecting UI elements like buttons, switches, etc.
 */
public class ObjectDetector {
    private static final String TAG = "ObjectDetector";
    
    // Singleton instance
    private static ObjectDetector instance;
    
    // ML Kit object detector
    private final com.google.mlkit.vision.objects.ObjectDetector mlkitDetector;
    
    // UI element categories
    public enum UIElementType {
        BUTTON,
        TOGGLE,
        CHECKBOX,
        SLIDER,
        TEXT_FIELD,
        MENU,
        ICON,
        OTHER
    }
    
    /**
     * UI element detected in the image
     */
    public static class UIElement {
        private final RectF boundingBox;
        private final UIElementType type;
        private final float confidence;
        private final Map<String, Object> attributes;
        
        public UIElement(RectF boundingBox, UIElementType type, float confidence) {
            this.boundingBox = boundingBox;
            this.type = type;
            this.confidence = confidence;
            this.attributes = new HashMap<>();
        }
        
        public RectF getBoundingBox() {
            return boundingBox;
        }
        
        public UIElementType getType() {
            return type;
        }
        
        public float getConfidence() {
            return confidence;
        }
        
        public Map<String, Object> getAttributes() {
            return attributes;
        }
        
        public void addAttribute(String key, Object value) {
            attributes.put(key, value);
        }
        
        @Override
        public String toString() {
            return "UIElement{" +
                    "type=" + type +
                    ", confidence=" + confidence +
                    ", boundingBox=" + boundingBox +
                    '}';
        }
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized ObjectDetector getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new ObjectDetector(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private ObjectDetector(Context context) {
        // Configure object detector with high accuracy mode
        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build();
        
        // Create ML Kit detector
        mlkitDetector = ObjectDetection.getClient(options);
        
        Log.i(TAG, "Object detector initialized");
    }
    
    /**
     * Detect UI elements in bitmap synchronously
     * 
     * @param bitmap Image to analyze
     * @return List of detected UI elements
     */
    public List<UIElement> detectUIElements(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            Log.e(TAG, "Cannot detect UI elements: invalid bitmap");
            return new ArrayList<>();
        }
        
        final AtomicReference<List<UIElement>> resultRef = new AtomicReference<>(new ArrayList<>());
        final CountDownLatch latch = new CountDownLatch(1);
        
        // Create input image
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        
        // Process image
        mlkitDetector.process(image)
                .addOnSuccessListener(detectedObjects -> {
                    List<UIElement> elements = new ArrayList<>();
                    
                    for (DetectedObject object : detectedObjects) {
                        RectF boundingBox = object.getBoundingBox();
                        
                        // Determine UI element type based on classification
                        UIElementType elementType = determineElementType(object);
                        
                        // Calculate confidence
                        float confidence = calculateConfidence(object);
                        
                        // Create UI element
                        UIElement element = new UIElement(boundingBox, elementType, confidence);
                        
                        // Add additional attributes if available
                        addElementAttributes(element, object);
                        
                        elements.add(element);
                    }
                    
                    resultRef.set(elements);
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error detecting objects: " + e.getMessage());
                    latch.countDown();
                });
                
        try {
            // Wait for detection to complete (with timeout)
            boolean completed = latch.await(3, TimeUnit.SECONDS);
            if (!completed) {
                Log.w(TAG, "Object detection timed out");
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Object detection interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
        
        return resultRef.get();
    }
    
    /**
     * Detect UI elements asynchronously
     */
    public Task<List<UIElement>> detectUIElementsAsync(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            Log.e(TAG, "Cannot detect UI elements: invalid bitmap");
            return Task.forResult(new ArrayList<>());
        }
        
        // Create input image
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        
        // Process image and transform result
        return mlkitDetector.process(image)
                .continueWith(task -> {
                    List<UIElement> elements = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DetectedObject object : task.getResult()) {
                            RectF boundingBox = object.getBoundingBox();
                            UIElementType elementType = determineElementType(object);
                            float confidence = calculateConfidence(object);
                            
                            UIElement element = new UIElement(boundingBox, elementType, confidence);
                            addElementAttributes(element, object);
                            
                            elements.add(element);
                        }
                    }
                    return elements;
                });
    }
    
    /**
     * Determine UI element type based on detected object
     */
    private UIElementType determineElementType(DetectedObject object) {
        // Default to OTHER if no classifications available
        if (object.getLabels().isEmpty()) {
            return UIElementType.OTHER;
        }
        
        // Get highest confidence classification
        DetectedObject.Label topLabel = object.getLabels().get(0);
        for (DetectedObject.Label label : object.getLabels()) {
            if (label.getConfidence() > topLabel.getConfidence()) {
                topLabel = label;
            }
        }
        
        // Map classification to UI element type
        String labelText = topLabel.getText().toLowerCase();
        
        if (labelText.contains("button") || labelText.contains("clickable")) {
            return UIElementType.BUTTON;
        } else if (labelText.contains("toggle") || labelText.contains("switch")) {
            return UIElementType.TOGGLE;
        } else if (labelText.contains("checkbox")) {
            return UIElementType.CHECKBOX;
        } else if (labelText.contains("slider") || labelText.contains("seekbar")) {
            return UIElementType.SLIDER;
        } else if (labelText.contains("text") || labelText.contains("input") || labelText.contains("edit")) {
            return UIElementType.TEXT_FIELD;
        } else if (labelText.contains("menu") || labelText.contains("dropdown")) {
            return UIElementType.MENU;
        } else if (labelText.contains("icon") || labelText.contains("image")) {
            return UIElementType.ICON;
        } else {
            return UIElementType.OTHER;
        }
    }
    
    /**
     * Calculate confidence score for UI element
     */
    private float calculateConfidence(DetectedObject object) {
        if (object.getLabels().isEmpty()) {
            return 0.5f; // Default confidence
        }
        
        // Find highest confidence
        float maxConfidence = 0f;
        for (DetectedObject.Label label : object.getLabels()) {
            if (label.getConfidence() > maxConfidence) {
                maxConfidence = label.getConfidence();
            }
        }
        
        return maxConfidence;
    }
    
    /**
     * Add additional attributes to UI element
     */
    private void addElementAttributes(UIElement element, DetectedObject object) {
        // Add tracking ID if available
        if (object.getTrackingId() != null) {
            element.addAttribute("trackingId", object.getTrackingId());
        }
        
        // Add all labels with confidence
        int labelIndex = 0;
        for (DetectedObject.Label label : object.getLabels()) {
            element.addAttribute("label_" + labelIndex, label.getText());
            element.addAttribute("confidence_" + labelIndex, label.getConfidence());
            labelIndex++;
        }
        
        // Add bounding box dimensions
        RectF box = object.getBoundingBox();
        element.addAttribute("width", box.width());
        element.addAttribute("height", box.height());
        element.addAttribute("area", box.width() * box.height());
        element.addAttribute("centerX", box.centerX());
        element.addAttribute("centerY", box.centerY());
    }
    
    /**
     * Release resources
     */
    public void release() {
        mlkitDetector.close();
        Log.i(TAG, "Object detector released");
    }
}
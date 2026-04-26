package com.aiassistant.ml;

import android.graphics.Rect;

import com.aiassistant.detection.UIElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context class for maintaining the current state and context information
 * for the Predictive Action System.
 * This class tracks UI elements, app state, and other contextual information
 * needed for predictions.
 */
public class Context {
    
    private List<UIElement> currentElements;
    private Map<String, Object> appState;
    private long timestamp;
    private Map<String, List<Double>> numericalFeatures;
    private Map<String, List<String>> categoricalFeatures;
    private List<ActionSuggestion> lastPredictions;
    private String currentAppPackage;
    private String currentAppActivity;
    private Map<String, Integer> elementCounts;
    
    /**
     * Default constructor
     */
    public Context() {
        this.currentElements = new ArrayList<>();
        this.appState = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
        this.numericalFeatures = new HashMap<>();
        this.categoricalFeatures = new HashMap<>();
        this.lastPredictions = new ArrayList<>();
        this.elementCounts = new HashMap<>();
    }
    
    /**
     * Get current UI elements
     * 
     * @return List of current UI elements
     */
    public List<UIElement> getCurrentElements() {
        return currentElements;
    }
    
    /**
     * Set current UI elements
     * 
     * @param currentElements List of current UI elements
     */
    public void setCurrentElements(List<UIElement> currentElements) {
        this.currentElements = currentElements != null ? currentElements : new ArrayList<>();
        updateElementCounts();
    }
    
    /**
     * Update element counts based on current elements
     */
    private void updateElementCounts() {
        elementCounts.clear();
        
        for (UIElement element : currentElements) {
            String type = element.getType().toString();
            elementCounts.put(type, elementCounts.getOrDefault(type, 0) + 1);
        }
    }
    
    /**
     * Get app state
     * 
     * @return App state map
     */
    public Map<String, Object> getAppState() {
        return appState;
    }
    
    /**
     * Set app state
     * 
     * @param appState App state map
     */
    public void setAppState(Map<String, Object> appState) {
        this.appState = appState != null ? appState : new HashMap<>();
    }
    
    /**
     * Get timestamp
     * 
     * @return Context timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Set timestamp
     * 
     * @param timestamp Context timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Update timestamp to current time
     */
    public void updateTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Get numerical features
     * 
     * @return Map of numerical features
     */
    public Map<String, List<Double>> getNumericalFeatures() {
        return numericalFeatures;
    }
    
    /**
     * Set numerical features
     * 
     * @param numericalFeatures Map of numerical features
     */
    public void setNumericalFeatures(Map<String, List<Double>> numericalFeatures) {
        this.numericalFeatures = numericalFeatures != null ? numericalFeatures : new HashMap<>();
    }
    
    /**
     * Add numerical feature
     * 
     * @param key Feature key
     * @param value Feature value
     */
    public void addNumericalFeature(String key, double value) {
        if (!numericalFeatures.containsKey(key)) {
            numericalFeatures.put(key, new ArrayList<>());
        }
        numericalFeatures.get(key).add(value);
    }
    
    /**
     * Get categorical features
     * 
     * @return Map of categorical features
     */
    public Map<String, List<String>> getCategoricalFeatures() {
        return categoricalFeatures;
    }
    
    /**
     * Set categorical features
     * 
     * @param categoricalFeatures Map of categorical features
     */
    public void setCategoricalFeatures(Map<String, List<String>> categoricalFeatures) {
        this.categoricalFeatures = categoricalFeatures != null ? categoricalFeatures : new HashMap<>();
    }
    
    /**
     * Add categorical feature
     * 
     * @param key Feature key
     * @param value Feature value
     */
    public void addCategoricalFeature(String key, String value) {
        if (!categoricalFeatures.containsKey(key)) {
            categoricalFeatures.put(key, new ArrayList<>());
        }
        categoricalFeatures.get(key).add(value);
    }
    
    /**
     * Get last predictions
     * 
     * @return List of last predictions
     */
    public List<ActionSuggestion> getLastPredictions() {
        return lastPredictions;
    }
    
    /**
     * Set last predictions
     * 
     * @param lastPredictions List of last predictions
     */
    public void setLastPredictions(List<ActionSuggestion> lastPredictions) {
        this.lastPredictions = lastPredictions != null ? lastPredictions : new ArrayList<>();
    }
    
    /**
     * Get current app package
     * 
     * @return Current app package
     */
    public String getCurrentAppPackage() {
        return currentAppPackage;
    }
    
    /**
     * Set current app package
     * 
     * @param currentAppPackage Current app package
     */
    public void setCurrentAppPackage(String currentAppPackage) {
        this.currentAppPackage = currentAppPackage;
    }
    
    /**
     * Get current app activity
     * 
     * @return Current app activity
     */
    public String getCurrentAppActivity() {
        return currentAppActivity;
    }
    
    /**
     * Set current app activity
     * 
     * @param currentAppActivity Current app activity
     */
    public void setCurrentAppActivity(String currentAppActivity) {
        this.currentAppActivity = currentAppActivity;
    }
    
    /**
     * Get element counts
     * 
     * @return Map of element type counts
     */
    public Map<String, Integer> getElementCounts() {
        return elementCounts;
    }
    
    /**
     * Find UI element at position
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return UI element at position, or null if none found
     */
    public UIElement findElementAt(int x, int y) {
        for (UIElement element : currentElements) {
            if (element.contains(x, y)) {
                return element;
            }
        }
        return null;
    }
    
    /**
     * Find UI elements in rect
     * 
     * @param rect Rectangle to search in
     * @return List of UI elements in rectangle
     */
    public List<UIElement> findElementsInRect(Rect rect) {
        List<UIElement> result = new ArrayList<>();
        
        for (UIElement element : currentElements) {
            Rect elementBounds = element.getBounds();
            if (elementBounds != null && Rect.intersects(elementBounds, rect)) {
                result.add(element);
            }
        }
        
        return result;
    }
    
    /**
     * Clear context
     */
    public void clear() {
        currentElements.clear();
        appState.clear();
        numericalFeatures.clear();
        categoricalFeatures.clear();
        lastPredictions.clear();
        elementCounts.clear();
        currentAppPackage = null;
        currentAppActivity = null;
        updateTimestamp();
    }
}
package com.aiassistant.detection;

import android.graphics.Rect;
import utils.UIElementConverter;
import androidx.annotation.NonNull;
import utils.UIElementConverter;
import androidx.annotation.Nullable;
import utils.UIElementConverter;
import models.StandardizedUIElement;
import utils.UIElementConverter;
import models.StandardizedUIElementType;
import utils.UIElementConverter;

/**
 * UI Element class used by detection system
 * This implementation is designed to be compatible with the element detector.
 */
public class UIElement {
    private final String id;
    private final String type;
    private final Rect bounds;
    private final String text;
    private final float confidence;
    
    /**
     * Create a new UIElement
     * 
     * @param id Unique identifier for this element
     * @param type Type of UI element
     * @param bounds Bounds of the element on screen
     * @param text Text content of the element (if any)
     * @param confidence Confidence score for the element detection
     */
    public UIElement(
            @NonNull String id,
            @NonNull String type,
            @NonNull Rect bounds,
            @Nullable String text,
            float confidence) {
        this.id = id;
        this.type = type;
        this.bounds = bounds;
        this.text = text;
        this.confidence = confidence;
    }
    
    /**
     * Create a new UIElement with default confidence
     * 
     * @param id Unique identifier for this element
     * @param type Type of UI element
     * @param bounds Bounds of the element on screen
     * @param text Text content of the element (if any)
     */
    public UIElement(
            @NonNull String id,
            @NonNull String type,
            @NonNull Rect bounds,
            @Nullable String text) {
        this(id, type, bounds, text, 1.0f);
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getType() {
        return type;
    }

    @NonNull
    public Rect getBounds() {
        return bounds;
    }

    @Nullable
    public String getText() {
        return text;
    }

    public float getConfidence() {
        return confidence;
    }
    
    /**
     * Get the center X coordinate of this element
     * 
     * @return Center X coordinate
     */
    public int getCenterX() {
        return bounds.centerX();
    }
    
    /**
     * Get the center Y coordinate of this element
     * 
     * @return Center Y coordinate
     */
    public int getCenterY() {
        return bounds.centerY();
    }
    
    /**
     * Convert this UIElement to a StandardizedUIElement
     * 
     * @return StandardizedUIElement representation
     */
    @NonNull
    public StandardizedUIElement toStandardized() {
        StandardizedUIElementType elementType = StandardizedUIElementType.fromString(type);
        return new StandardizedUIElement(
                id,
                elementType,
                bounds,
                text,
                null,  // No content description available
                true,  // Assume clickable by default
                false, // Assume not scrollable by default
                true,  // Assume enabled by default
                confidence
        );
    }
    
    /**
     * Create a UIElement from a StandardizedUIElement
     * 
     * @param standardized StandardizedUIElement to convert
     * @return UIElement representation
     */
    @NonNull
    public static UIElement fromStandardized(@NonNull StandardizedUIElement standardized) {
        return new UIElement(
                standardized.getId(),
                standardized.getTypeString(),
                standardized.getBounds(),
                standardized.getText(),
                standardized.getConfidence()
        );
    }

    @Override
    public String toString() {
        return "UIElement{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", bounds=" + bounds +
                ", text='" + text + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
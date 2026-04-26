package com.aiassistant.detection;

import android.graphics.Rect;
import models.StandardizedUIElementType;
import models.UIElementType;

/**
 * Utility class for element detection and type comparisons.
 * Provides helper methods for common operations on UI elements.
 */
public class ElementUtils {
    
    /**
     * Compare element types safely regardless of their actual class implementation
     * 
     * @param type1 First element type (can be String, UIElementType, or GameAppElementDetector.ElementType)
     * @param type2 Second element type (can be String, UIElementType, or GameAppElementDetector.ElementType)
     * @return True if the types are equivalent
     */
    public static boolean areTypesEqual(Object type1, Object type2) {
        if (type1 == null || type2 == null) {
            return type1 == type2;
        }
        
        // Convert both to strings for comparison
        String type1Str = convertTypeToString(type1);
        String type2Str = convertTypeToString(type2);
        
        return type1Str.equals(type2Str);
    }
    
    /**
     * Convert element type to string representation
     * 
     * @param type Element type to convert
     * @return String representation of the type
     */
    public static String convertTypeToString(Object type) {
        if (type == null) {
            return "UNKNOWN";
        }
        
        if (type instanceof String) {
            return (String) type;
        }
        
        if (type instanceof StandardizedUIElementType) {
            return ((StandardizedUIElementType) type).name();
        }
        
        if (type instanceof UIElementType) {
            return ((UIElementType) type).name();
        }
        
        if (type instanceof GameAppElementDetector.ElementType) {
            return ((GameAppElementDetector.ElementType) type).name();
        }
        
        return type.toString();
    }
    
    /**
     * Get element type as StandardizedUIElementType (the new unified type system)
     * 
     * @param type Element type (can be String, UIElementType, or GameAppElementDetector.ElementType)
     * @return StandardizedUIElementType equivalent
     */
    public static StandardizedUIElementType getAsStandardizedType(Object type) {
        if (type == null) {
            return StandardizedUIElementType.UNKNOWN;
        }
        
        if (type instanceof StandardizedUIElementType) {
            return (StandardizedUIElementType) type;
        }
        
        if (type instanceof UIElementType) {
            return StandardizedUIElementType.fromUIElementType((UIElementType) type);
        }
        
        if (type instanceof GameAppElementDetector.ElementType) {
            return StandardizedUIElementType.fromElementType((GameAppElementDetector.ElementType) type);
        }
        
        if (type instanceof String) {
            return StandardizedUIElementType.fromString((String) type);
        }
        
        return StandardizedUIElementType.UNKNOWN;
    }
    
    /**
     * Get element type as UIElementType (legacy support)
     * 
     * @param type Element type (can be String, ElementType, or GameAppElementDetector.ElementType)
     * @return UIElementType equivalent
     */
    public static UIElementType getAsUIElementType(Object type) {
        if (type == null) {
            return UIElementType.UNKNOWN;
        }
        
        if (type instanceof UIElementType) {
            return (UIElementType) type;
        }
        
        String typeStr = convertTypeToString(type);
        
        // Try to match to UIElementType
        for (UIElementType t : UIElementType.values()) {
            if (t.name().equals(typeStr)) {
                return t;
            }
        }
        
        // Map GameAppElementDetector.ElementType to UIElementType
        if (type instanceof GameAppElementDetector.ElementType) {
            switch ((GameAppElementDetector.ElementType) type) {
                case BUTTON:
                    return UIElementType.BUTTON;
                case MENU:
                    return UIElementType.MENU_ITEM;
                case DIALOG:
                    return UIElementType.CONTAINER;
                case PLAYER:
                case ENEMY:
                case CHARACTER:
                    return UIElementType.IMAGE;
                default:
                    return UIElementType.UNKNOWN;
            }
        }
        
        // Map StandardizedUIElementType to UIElementType
        if (type instanceof StandardizedUIElementType) {
            StandardizedUIElementType stdType = (StandardizedUIElementType) type;
            try {
                return UIElementType.valueOf(stdType.name());
            } catch (IllegalArgumentException e) {
                // Best effort mapping for types that don't have exact matches
                switch (stdType) {
                    case BUTTON: return UIElementType.BUTTON;
                    case TEXT: return UIElementType.TEXT;
                    case IMAGE: return UIElementType.IMAGE;
                    case CONTAINER: return UIElementType.CONTAINER;
                    case LIST: return UIElementType.LIST;
                    case CHECKBOX: return UIElementType.CHECKBOX;
                    case TOGGLE: return UIElementType.TOGGLE;
                    default: return UIElementType.UNKNOWN;
                }
            }
        }
        
        return UIElementType.UNKNOWN;
    }
    
    /**
     * Get element type as GameAppElementDetector.ElementType (legacy support)
     * 
     * @param type Element type (can be String, ElementType, or GameAppElementDetector.ElementType)
     * @return GameAppElementDetector.ElementType equivalent
     */
    public static GameAppElementDetector.ElementType getAsGameElementType(Object type) {
        if (type == null) {
            return GameAppElementDetector.ElementType.UNKNOWN;
        }
        
        if (type instanceof GameAppElementDetector.ElementType) {
            return (GameAppElementDetector.ElementType) type;
        }
        
        String typeStr = convertTypeToString(type);
        
        // Try to match to GameAppElementDetector.ElementType
        for (GameAppElementDetector.ElementType t : GameAppElementDetector.ElementType.values()) {
            if (t.name().equals(typeStr)) {
                return t;
            }
        }
        
        // Map UIElementType to GameAppElementDetector.ElementType
        if (type instanceof UIElementType) {
            switch ((UIElementType) type) {
                case BUTTON:
                    return GameAppElementDetector.ElementType.BUTTON;
                case MENU_ITEM:
                    return GameAppElementDetector.ElementType.MENU;
                case CONTAINER:
                    return GameAppElementDetector.ElementType.DIALOG;
                case IMAGE:
                    return GameAppElementDetector.ElementType.CHARACTER;
                default:
                    return GameAppElementDetector.ElementType.UNKNOWN;
            }
        }
        
        // Map StandardizedUIElementType to GameAppElementDetector.ElementType
        if (type instanceof StandardizedUIElementType) {
            StandardizedUIElementType stdType = (StandardizedUIElementType) type;
            try {
                return GameAppElementDetector.ElementType.valueOf(stdType.name());
            } catch (IllegalArgumentException e) {
                // Best effort mapping for types that don't have exact matches
                switch (stdType) {
                    case BUTTON: return GameAppElementDetector.ElementType.BUTTON;
                    case TEXT: return GameAppElementDetector.ElementType.TEXT;
                    case IMAGE: return GameAppElementDetector.ElementType.IMAGE;
                    case MENU: return GameAppElementDetector.ElementType.MENU;
                    case DIALOG: return GameAppElementDetector.ElementType.DIALOG;
                    case CHARACTER: return GameAppElementDetector.ElementType.CHARACTER;
                    case ENEMY: return GameAppElementDetector.ElementType.ENEMY;
                    default: return GameAppElementDetector.ElementType.UNKNOWN;
                }
            }
        }
        
        return GameAppElementDetector.ElementType.UNKNOWN;
    }
    
    /**
     * Check if rectangles have any overlap
     * 
     * @param rect1 First rectangle
     * @param rect2 Second rectangle
     * @return True if rectangles overlap
     */
    public static boolean doRectsOverlap(Rect rect1, Rect rect2) {
        if (rect1 == null || rect2 == null) {
            return false;
        }
        
        return Rect.intersects(rect1, rect2);
    }
    
    /**
     * Calculate overlap area between two rectangles
     * 
     * @param rect1 First rectangle
     * @param rect2 Second rectangle
     * @return Area of overlap (0 if no overlap)
     */
    public static int getOverlapArea(Rect rect1, Rect rect2) {
        if (rect1 == null || rect2 == null || !Rect.intersects(rect1, rect2)) {
            return 0;
        }
        
        Rect intersection = new Rect();
        intersection.setIntersect(rect1, rect2);
        
        return intersection.width() * intersection.height();
    }
    
    /**
     * Calculate distance between rectangles (0 if overlapping)
     * 
     * @param rect1 First rectangle
     * @param rect2 Second rectangle
     * @return Distance between rectangles
     */
    public static double getDistance(Rect rect1, Rect rect2) {
        if (rect1 == null || rect2 == null) {
            return Double.MAX_VALUE;
        }
        
        if (Rect.intersects(rect1, rect2)) {
            return 0;
        }
        
        // Calculate centers
        int centerX1 = rect1.centerX();
        int centerY1 = rect1.centerY();
        int centerX2 = rect2.centerX();
        int centerY2 = rect2.centerY();
        
        // Calculate distance between centers
        return Math.sqrt(Math.pow(centerX2 - centerX1, 2) + Math.pow(centerY2 - centerY1, 2));
    }
}
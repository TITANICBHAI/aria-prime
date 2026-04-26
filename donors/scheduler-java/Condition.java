package com.aiassistant.scheduler;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A condition that can be evaluated for task execution
 * Based on Condition class from advanced_task_scheduler.py
 */
public class Condition implements Serializable {
    private static final String TAG = "Condition";
    
    private Object left;
    private ConditionOperator operator;
    private Object right;
    private List<Condition> subConditions;
    
    /**
     * Create a simple condition
     */
    public Condition(Object left, ConditionOperator operator, Object right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
        this.subConditions = null;
    }
    
    /**
     * Create a logical condition (AND, OR, NOT)
     */
    public Condition(ConditionOperator operator, List<Condition> subConditions) {
        this.left = null;
        this.operator = operator;
        this.right = null;
        this.subConditions = subConditions;
    }
    
    /**
     * Evaluate the condition in a given context
     *
     * @param context Dictionary of context variables
     * @return Boolean result of condition evaluation
     */
    public boolean evaluate(Map<String, Object> context) {
        // Handle nested logic operators (AND, OR, NOT)
        if (operator == ConditionOperator.AND && subConditions != null) {
            for (Condition cond : subConditions) {
                if (!cond.evaluate(context)) {
                    return false;
                }
            }
            return true;
        }
        
        if (operator == ConditionOperator.OR && subConditions != null) {
            for (Condition cond : subConditions) {
                if (cond.evaluate(context)) {
                    return true;
                }
            }
            return false;
        }
        
        if (operator == ConditionOperator.NOT && subConditions != null && !subConditions.isEmpty()) {
            return !subConditions.get(0).evaluate(context);
        }
        
        // Resolve variables in left and right if they're strings
        Object leftValue = resolveValue(left, context);
        Object rightValue = resolveValue(right, context);
        
        // Handle null values
        if (leftValue == null || rightValue == null) {
            if (operator == ConditionOperator.EQUALS) {
                return leftValue == rightValue;
            } else if (operator == ConditionOperator.NOT_EQUALS) {
                return leftValue != rightValue;
            } else {
                return false; // Other comparisons with nulls return false
            }
        }
        
        // Simple operators
        switch (operator) {
            case EQUALS:
                return leftValue.equals(rightValue);
                
            case NOT_EQUALS:
                return !leftValue.equals(rightValue);
                
            case GREATER_THAN:
                if (leftValue instanceof Number && rightValue instanceof Number) {
                    return ((Number) leftValue).doubleValue() > ((Number) rightValue).doubleValue();
                }
                return false;
                
            case LESS_THAN:
                if (leftValue instanceof Number && rightValue instanceof Number) {
                    return ((Number) leftValue).doubleValue() < ((Number) rightValue).doubleValue();
                }
                return false;
                
            case GREATER_THAN_EQUAL:
                if (leftValue instanceof Number && rightValue instanceof Number) {
                    return ((Number) leftValue).doubleValue() >= ((Number) rightValue).doubleValue();
                }
                return false;
                
            case LESS_THAN_EQUAL:
                if (leftValue instanceof Number && rightValue instanceof Number) {
                    return ((Number) leftValue).doubleValue() <= ((Number) rightValue).doubleValue();
                }
                return false;
                
            // String operators
            case CONTAINS:
                return leftValue.toString().contains(rightValue.toString());
                
            case NOT_CONTAINS:
                return !leftValue.toString().contains(rightValue.toString());
                
            case STARTS_WITH:
                return leftValue.toString().startsWith(rightValue.toString());
                
            case ENDS_WITH:
                return leftValue.toString().endsWith(rightValue.toString());
                
            case MATCHES_REGEX:
                try {
                    Pattern pattern = Pattern.compile(rightValue.toString());
                    return pattern.matcher(leftValue.toString()).matches();
                } catch (Exception e) {
                    Log.e(TAG, "Regex error: " + e.getMessage());
                    return false;
                }
                
            default:
                Log.w(TAG, "Unknown operator: " + operator);
                return false;
        }
    }
    
    /**
     * Resolve a value that might be a variable reference
     *
     * @param value Value to resolve
     * @param context Dictionary of context variables
     * @return Resolved value
     */
    private Object resolveValue(Object value, Map<String, Object> context) {
        if (value instanceof String && ((String) value).startsWith("$")) {
            // Variable reference - remove $ and get from context
            String varName = ((String) value).substring(1);
            if (varName.contains(".")) {
                // Handle nested properties with dot notation
                String[] parts = varName.split("\\.");
                Object current = context;
                for (String part : parts) {
                    if (current instanceof Map) {
                        current = ((Map<?, ?>) current).get(part);
                    } else {
                        Log.w(TAG, "Variable not found: " + varName);
                        return null;
                    }
                }
                return current;
            } else {
                return context.get(varName);
            }
        }
        
        return value;
    }
    
    /**
     * Create a logical AND condition
     */
    public static Condition and(List<Condition> conditions) {
        return new Condition(ConditionOperator.AND, conditions);
    }
    
    /**
     * Create a logical OR condition
     */
    public static Condition or(List<Condition> conditions) {
        return new Condition(ConditionOperator.OR, conditions);
    }
    
    /**
     * Create a logical NOT condition
     */
    public static Condition not(Condition condition) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition);
        return new Condition(ConditionOperator.NOT, conditions);
    }
    
    // Getters and setters
    public Object getLeft() {
        return left;
    }
    
    public void setLeft(Object left) {
        this.left = left;
    }
    
    public ConditionOperator getOperator() {
        return operator;
    }
    
    public void setOperator(ConditionOperator operator) {
        this.operator = operator;
    }
    
    public Object getRight() {
        return right;
    }
    
    public void setRight(Object right) {
        this.right = right;
    }
    
    public List<Condition> getSubConditions() {
        return subConditions;
    }
    
    public void setSubConditions(List<Condition> subConditions) {
        this.subConditions = subConditions;
    }
}
package com.aiassistant.scheduler.model;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Condition for task execution
 */
public class Condition implements Serializable {
    private static final String TAG = "Condition";
    
    private static final long serialVersionUID = 1L;
    
    // Condition properties
    private String conditionId;
    private String name;
    private String description;
    private ConditionOperator operator;
    private Object leftOperand;
    private Object rightOperand;
    private List<Condition> subConditions;
    
    // Constructor for simple conditions
    public Condition(ConditionOperator operator, Object leftOperand, Object rightOperand) {
        this.conditionId = "cond_" + System.currentTimeMillis();
        this.operator = operator;
        this.leftOperand = leftOperand;
        this.rightOperand = rightOperand;
    }
    
    // Constructor for logical conditions (AND, OR, NOT)
    public Condition(ConditionOperator operator, List<Condition> subConditions) {
        this.conditionId = "cond_" + System.currentTimeMillis();
        this.operator = operator;
        this.subConditions = subConditions;
    }
    
    // Empty constructor for serialization
    public Condition() {
        this.conditionId = "cond_" + System.currentTimeMillis();
    }
    
    /**
     * Evaluate the condition with given context values
     */
    public boolean evaluate(Map<String, Object> context) {
        try {
            // Handle logical operators first
            if (operator == ConditionOperator.AND) {
                return evaluateAnd(context);
            } else if (operator == ConditionOperator.OR) {
                return evaluateOr(context);
            } else if (operator == ConditionOperator.NOT) {
                return evaluateNot(context);
            }
            
            // Resolve values from context if needed
            Object leftValue = resolveValue(leftOperand, context);
            Object rightValue = resolveValue(rightOperand, context);
            
            // Handle null values
            if (leftValue == null) {
                return (operator == ConditionOperator.NOT_EXISTS || 
                        (operator == ConditionOperator.EQUALS && rightValue == null) ||
                        (operator == ConditionOperator.NOT_EQUALS && rightValue != null));
            }
            
            // Handle special operators that don't need right value
            if (operator == ConditionOperator.EXISTS) {
                return true; // We already know leftValue is not null
            } else if (operator == ConditionOperator.IS_TRUE) {
                return Boolean.TRUE.equals(leftValue);
            } else if (operator == ConditionOperator.IS_FALSE) {
                return Boolean.FALSE.equals(leftValue);
            }
            
            // Handle operators that require both values
            return evaluateComparison(leftValue, rightValue);
            
        } catch (Exception e) {
            Log.e(TAG, "Error evaluating condition: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Evaluate logical AND condition
     */
    private boolean evaluateAnd(Map<String, Object> context) {
        if (subConditions == null || subConditions.isEmpty()) {
            return true;
        }
        
        for (Condition condition : subConditions) {
            if (!condition.evaluate(context)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Evaluate logical OR condition
     */
    private boolean evaluateOr(Map<String, Object> context) {
        if (subConditions == null || subConditions.isEmpty()) {
            return false;
        }
        
        for (Condition condition : subConditions) {
            if (condition.evaluate(context)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Evaluate logical NOT condition
     */
    private boolean evaluateNot(Map<String, Object> context) {
        if (subConditions == null || subConditions.isEmpty()) {
            return true;
        }
        
        // NOT applies to the first sub-condition
        return !subConditions.get(0).evaluate(context);
    }
    
    /**
     * Evaluate comparison operators
     */
    private boolean evaluateComparison(Object left, Object right) {
        // Handle null value for right operand
        if (right == null) {
            return (operator == ConditionOperator.NOT_EQUALS);
        }
        
        // Handle numeric comparisons
        if (left instanceof Number && right instanceof Number) {
            return evaluateNumericComparison((Number) left, (Number) right);
        }
        
        // Handle string comparisons
        if (left instanceof String) {
            return evaluateStringComparison((String) left, right);
        }
        
        // Handle boolean comparisons
        if (left instanceof Boolean && right instanceof Boolean) {
            return evaluateBooleanComparison((Boolean) left, (Boolean) right);
        }
        
        // Handle collection comparisons
        if (right instanceof Collection) {
            return evaluateCollectionComparison(left, (Collection<?>) right);
        }
        
        // Default equals/not equals comparison
        if (operator == ConditionOperator.EQUALS) {
            return left.equals(right);
        } else if (operator == ConditionOperator.NOT_EQUALS) {
            return !left.equals(right);
        }
        
        // Default case: not comparable
        return false;
    }
    
    /**
     * Evaluate numeric comparison
     */
    private boolean evaluateNumericComparison(Number left, Number right) {
        double leftValue = left.doubleValue();
        double rightValue = right.doubleValue();
        
        switch (operator) {
            case EQUALS:
                return leftValue == rightValue;
            case NOT_EQUALS:
                return leftValue != rightValue;
            case GREATER_THAN:
                return leftValue > rightValue;
            case LESS_THAN:
                return leftValue < rightValue;
            case GREATER_THAN_EQUAL:
                return leftValue >= rightValue;
            case LESS_THAN_EQUAL:
                return leftValue <= rightValue;
            default:
                return false;
        }
    }
    
    /**
     * Evaluate string comparison
     */
    private boolean evaluateStringComparison(String left, Object right) {
        String rightStr = right.toString();
        
        switch (operator) {
            case EQUALS:
                return left.equals(rightStr);
            case NOT_EQUALS:
                return !left.equals(rightStr);
            case CONTAINS:
                return left.contains(rightStr);
            case NOT_CONTAINS:
                return !left.contains(rightStr);
            case STARTS_WITH:
                return left.startsWith(rightStr);
            case ENDS_WITH:
                return left.endsWith(rightStr);
            case MATCHES:
                return Pattern.matches(rightStr, left);
            default:
                return false;
        }
    }
    
    /**
     * Evaluate boolean comparison
     */
    private boolean evaluateBooleanComparison(Boolean left, Boolean right) {
        switch (operator) {
            case EQUALS:
                return left.equals(right);
            case NOT_EQUALS:
                return !left.equals(right);
            default:
                return false;
        }
    }
    
    /**
     * Evaluate collection comparison
     */
    private boolean evaluateCollectionComparison(Object left, Collection<?> right) {
        switch (operator) {
            case IN:
                return right.contains(left);
            case NOT_IN:
                return !right.contains(left);
            default:
                return false;
        }
    }
    
    /**
     * Resolve a value from context if it's a variable reference
     */
    private Object resolveValue(Object value, Map<String, Object> context) {
        if (value instanceof String && ((String) value).startsWith("${") && ((String) value).endsWith("}")) {
            // This is a variable reference, resolve it from context
            String varName = ((String) value).substring(2, ((String) value).length() - 1);
            return context.getOrDefault(varName, null);
        }
        
        return value;
    }
    
    /**
     * Create a simple equals condition
     */
    public static Condition equals(Object left, Object right) {
        return new Condition(ConditionOperator.EQUALS, left, right);
    }
    
    /**
     * Create a simple not equals condition
     */
    public static Condition notEquals(Object left, Object right) {
        return new Condition(ConditionOperator.NOT_EQUALS, left, right);
    }
    
    /**
     * Create a simple greater than condition
     */
    public static Condition greaterThan(Object left, Object right) {
        return new Condition(ConditionOperator.GREATER_THAN, left, right);
    }
    
    /**
     * Create a simple less than condition
     */
    public static Condition lessThan(Object left, Object right) {
        return new Condition(ConditionOperator.LESS_THAN, left, right);
    }
    
    /**
     * Create an AND condition
     */
    public static Condition and(Condition... conditions) {
        List<Condition> conditionList = new ArrayList<>();
        for (Condition condition : conditions) {
            conditionList.add(condition);
        }
        return new Condition(ConditionOperator.AND, conditionList);
    }
    
    /**
     * Create an OR condition
     */
    public static Condition or(Condition... conditions) {
        List<Condition> conditionList = new ArrayList<>();
        for (Condition condition : conditions) {
            conditionList.add(condition);
        }
        return new Condition(ConditionOperator.OR, conditionList);
    }
    
    /**
     * Create a NOT condition
     */
    public static Condition not(Condition condition) {
        List<Condition> conditionList = new ArrayList<>();
        conditionList.add(condition);
        return new Condition(ConditionOperator.NOT, conditionList);
    }
    
    // Getters and setters
    
    public String getConditionId() {
        return conditionId;
    }
    
    public void setConditionId(String conditionId) {
        this.conditionId = conditionId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public ConditionOperator getOperator() {
        return operator;
    }
    
    public void setOperator(ConditionOperator operator) {
        this.operator = operator;
    }
    
    public Object getLeftOperand() {
        return leftOperand;
    }
    
    public void setLeftOperand(Object leftOperand) {
        this.leftOperand = leftOperand;
    }
    
    public Object getRightOperand() {
        return rightOperand;
    }
    
    public void setRightOperand(Object rightOperand) {
        this.rightOperand = rightOperand;
    }
    
    public List<Condition> getSubConditions() {
        return subConditions;
    }
    
    public void setSubConditions(List<Condition> subConditions) {
        this.subConditions = subConditions;
    }
}
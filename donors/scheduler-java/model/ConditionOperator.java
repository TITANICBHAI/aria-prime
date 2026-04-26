package com.aiassistant.scheduler.model;

import java.io.Serializable;

/**
 * Condition operators for task conditions
 */
public enum ConditionOperator implements Serializable {
    EQUALS("=="),                  // Equal to
    NOT_EQUALS("!="),              // Not equal to
    GREATER_THAN(">"),             // Greater than
    LESS_THAN("<"),                // Less than
    GREATER_THAN_EQUAL(">="),      // Greater than or equal to
    LESS_THAN_EQUAL("<="),         // Less than or equal to
    CONTAINS("contains"),          // Contains substring
    NOT_CONTAINS("not_contains"),  // Does not contain substring
    STARTS_WITH("starts_with"),    // Starts with substring
    ENDS_WITH("ends_with"),        // Ends with substring
    MATCHES("matches"),            // Matches regular expression
    IN("in"),                      // In collection
    NOT_IN("not_in"),              // Not in collection
    EXISTS("exists"),              // Value exists
    NOT_EXISTS("not_exists"),      // Value does not exist
    IS_TRUE("is_true"),            // Value is true
    IS_FALSE("is_false"),          // Value is false
    AND("and"),                    // Logical AND
    OR("or"),                      // Logical OR
    NOT("not");                    // Logical NOT
    
    private final String value;
    
    ConditionOperator(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Get condition operator from string value
     */
    public static ConditionOperator fromString(String value) {
        for (ConditionOperator op : ConditionOperator.values()) {
            if (op.getValue().equals(value)) {
                return op;
            }
        }
        
        return EQUALS; // Default
    }
}
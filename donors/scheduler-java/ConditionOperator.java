package com.aiassistant.scheduler;

import java.io.Serializable;

/**
 * Operators for conditional logic
 * Based on ConditionOperator enum from advanced_task_scheduler.py
 */
public enum ConditionOperator implements Serializable {
    EQUALS("=="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    GREATER_THAN_EQUAL(">="),
    LESS_THAN_EQUAL("<="),
    CONTAINS("contains"),
    NOT_CONTAINS("not_contains"),
    STARTS_WITH("starts_with"),
    ENDS_WITH("ends_with"),
    MATCHES_REGEX("matches_regex"),
    AND("and"),
    OR("or"),
    NOT("not");
    
    private final String value;
    
    ConditionOperator(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
package com.aiassistant.core.orchestration;

import java.util.Map;

public interface ValidationContract {
    
    ValidationResult validate(String componentId, Map<String, Object> output);
    
    public static class ValidationResult {
        public final boolean isValid;
        public final String message;
        public final Map<String, Object> details;
        
        public ValidationResult(boolean isValid, String message, Map<String, Object> details) {
            this.isValid = isValid;
            this.message = message;
            this.details = details;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, "Validation passed", null);
        }
        
        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message, null);
        }
        
        public static ValidationResult failure(String message, Map<String, Object> details) {
            return new ValidationResult(false, message, details);
        }
    }
}

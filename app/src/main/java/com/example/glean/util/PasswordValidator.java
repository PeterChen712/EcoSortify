package com.example.glean.util;

/**
 * Utility class for password validation
 * Provides password strength validation according to security requirements
 */
public class PasswordValidator {
    
    // Password validation constants
    public static final int MIN_PASSWORD_LENGTH = 8;
    
    /**
     * Validates password according to security requirements:
     * - Minimum 8 characters
     * - At least 1 uppercase letter
     * - At least 1 lowercase letter
     * - At least 1 digit
     * 
     * @param password The password to validate
     * @return ValidationResult containing validation status and error message
     */
    public static ValidationResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "Password tidak boleh kosong");
        }
        
        // Check minimum length
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return new ValidationResult(false, 
                "Password harus minimal " + MIN_PASSWORD_LENGTH + " karakter");
        }
        
        // Check for at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            return new ValidationResult(false, 
                "Password harus mengandung minimal 1 huruf besar (A-Z)");
        }
        
        // Check for at least one lowercase letter
        if (!password.matches(".*[a-z].*")) {
            return new ValidationResult(false, 
                "Password harus mengandung minimal 1 huruf kecil (a-z)");
        }
        
        // Check for at least one digit
        if (!password.matches(".*[0-9].*")) {
            return new ValidationResult(false, 
                "Password harus mengandung minimal 1 angka (0-9)");
        }
        
        return new ValidationResult(true, "Password valid");
    }
    
    /**
     * Validates password with a comprehensive error message
     * @param password The password to validate
     * @return ValidationResult with detailed error message if invalid
     */
    public static ValidationResult validatePasswordWithDetailedMessage(String password) {
        ValidationResult result = validatePassword(password);
        
        if (!result.isValid()) {
            // Return detailed message for all requirements
            return new ValidationResult(false, 
                "Password harus minimal 8 karakter, mengandung huruf besar, huruf kecil, dan angka.");
        }
        
        return result;
    }
    
    /**
     * Result class for password validation
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final String errorMessage;
        
        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
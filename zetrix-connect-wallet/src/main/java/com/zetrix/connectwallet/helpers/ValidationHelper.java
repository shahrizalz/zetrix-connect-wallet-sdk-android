package com.zetrix.connectwallet.helpers;

import java.util.Map;

/**
 * Validation helper for wallet connect operations.
 * <p>
 * Provides validation methods for blob signing and transaction parameters
 * to ensure all required fields are present before sending requests to wallet apps.
 * </p>
 * <p>
 * Ported from Flutter SDK's validation_helper.dart.
 * </p>
 */
public final class ValidationHelper {

    // Prevent instantiation
    private ValidationHelper() {
        throw new AssertionError("Cannot instantiate ValidationHelper class");
    }

    /**
     * Validates blob signing parameters.
     * <p>
     * Checks if the required 'message' field is present in the parameters.
     * Blob signing is used to sign arbitrary binary data or messages with the wallet.
     * </p>
     *
     * @param param the parameters map to validate
     * @return empty string if valid, otherwise returns the name of the missing required field
     */
    public static String verifyBlobParam(Map<String, Object> param) {
        if (param == null) {
            return "param";
        }

        if (!param.containsKey("message") || param.get("message") == null) {
            return "message";
        }

        return "";
    }

    /**
     * Validates transaction parameters.
     * <p>
     * Checks if all required transaction fields are present:
     * - from: sender address
     * - to: recipient address
     * - amount: transaction amount (can be "0" or 0)
     * - gasFee: gas fee for transaction (can be "0" or 0)
     * - nonce: account nonce
     * </p>
     *
     * @param param the transaction parameters map to validate
     * @return empty string if valid, otherwise returns the name of the first missing required field
     */
    public static String verifyTransactionParam(Map<String, Object> param) {
        if (param == null) {
            return "param";
        }

        // Validate 'from' field
        if (!param.containsKey("from") || param.get("from") == null) {
            return "from";
        }

        // Validate 'to' field
        if (!param.containsKey("to") || param.get("to") == null) {
            return "to";
        }

        // Validate 'amount' field
        // Note: The Flutter logic is: if amount is null AND not "0" AND not 0
        // This seems to be checking if amount is missing (not allowing null unless it's 0)
        // Simplified: amount must exist, but can be null if explicitly set to "0" or 0
        Object amount = param.get("amount");
        if (amount == null) {
            // Check if the key exists but is explicitly null
            if (!param.containsKey("amount")) {
                return "amount";
            }
            // If amount is null, it should be "0" or 0 (but it's null, so invalid)
            if (!"0".equals(amount) && !Integer.valueOf(0).equals(amount)) {
                return "amount";
            }
        }

        // Validate 'gasFee' field (similar logic to amount)
        Object gasFee = param.get("gasFee");
        if (gasFee == null) {
            if (!param.containsKey("gasFee")) {
                return "gasFee";
            }
            if (!"0".equals(gasFee) && !Integer.valueOf(0).equals(gasFee)) {
                return "gasFee";
            }
        }

        // Validate 'nonce' field
        if (!param.containsKey("nonce") || param.get("nonce") == null) {
            return "nonce";
        }

        return "";
    }

    /**
     * Checks if a parameter validation result indicates success.
     *
     * @param validationResult the result from verifyBlobParam or verifyTransactionParam
     * @return true if validation passed (empty string), false otherwise
     */
    public static boolean isValid(String validationResult) {
        return validationResult == null || validationResult.isEmpty();
    }

    /**
     * Gets a human-readable error message for a validation failure.
     *
     * @param missingField the name of the missing field returned by validation methods
     * @return formatted error message
     */
    public static String getValidationErrorMessage(String missingField) {
        if (missingField == null || missingField.isEmpty()) {
            return "Validation passed";
        }
        return "Missing required parameter: " + missingField;
    }
}

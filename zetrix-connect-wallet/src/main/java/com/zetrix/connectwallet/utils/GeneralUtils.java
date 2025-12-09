package com.zetrix.connectwallet.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class for generic utility operations.
 * <p>
 * Provides general-purpose utility methods for common operations
 * like JSON validation and string manipulation.
 * </p>
 * <p>
 * Ported from Flutter SDK's general_utils.dart.
 * </p>
 */
public final class GeneralUtils {

    // Prevent instantiation
    private GeneralUtils() {
        throw new AssertionError("Cannot instantiate GeneralUtils class");
    }

    /**
     * Check if a string is valid JSON.
     * <p>
     * Attempts to parse the string as JSON and verifies it's either
     * a JSON object or JSON array.
     * </p>
     *
     * @param str the string to validate (can be null)
     * @return true if the string is valid JSON (object or array), false otherwise
     */
    public static boolean isJSON(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }

        try {
            // Try parsing as JSONObject
            new JSONObject(str);
            return true;
        } catch (JSONException e1) {
            try {
                // Try parsing as JSONArray
                new JSONArray(str);
                return true;
            } catch (JSONException e2) {
                // Not valid JSON
                return false;
            }
        }
    }

    /**
     * Safely trim a string.
     * <p>
     * Returns trimmed string, or empty string if input is null.
     * </p>
     *
     * @param str the string to trim
     * @return trimmed string, or empty string if null
     */
    public static String safeTrim(String str) {
        return str != null ? str.trim() : "";
    }

    /**
     * Check if a string is null or empty (after trimming).
     *
     * @param str the string to check
     * @return true if the string is null, empty, or contains only whitespace
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Check if a string is not null and not empty (after trimming).
     *
     * @param str the string to check
     * @return true if the string contains non-whitespace characters
     */
    public static boolean isNotNullOrEmpty(String str) {
        return !isNullOrEmpty(str);
    }
}

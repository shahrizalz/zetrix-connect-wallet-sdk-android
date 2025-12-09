package com.zetrix.connectwallet.helpers;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.UUID;

/**
 * Helper class for wallet session and request management.
 * <p>
 * Provides utility methods for generating session IDs, formatting request parameters,
 * and parsing response data for wallet socket communication.
 * </p>
 * <p>
 * Ported from Flutter SDK's session_helper.dart.
 * </p>
 */
public final class SessionHelper {

    // Prevent instantiation
    private SessionHelper() {
        throw new AssertionError("Cannot instantiate SessionHelper class");
    }

    /**
     * Generate a UUID v4 session ID for wallet connections.
     * <p>
     * Creates a random UUID using Java's UUID.randomUUID() which generates
     * a cryptographically strong pseudo-random UUID v4.
     * </p>
     *
     * @return a UUID v4 string in lowercase (e.g., "550e8400-e29b-41d4-a716-446655440000")
     */
    public static String createSessionId() {
        return UUID.randomUUID().toString().toLowerCase();
    }

    /**
     * Warn incorrect argument log for wallet operations.
     * <p>
     * Generates a standardized error message for incorrect arguments.
     * </p>
     *
     * @param name the name of the incorrect argument
     * @return formatted error message
     */
    public static String warnIncorrectLog(String name) {
        return "The '" + name + "' argument is incorrect";
    }

    /**
     * Fixed request parameters for wallet socket communication.
     * <p>
     * Wraps the provided parameters in the required structure for wallet socket messages:
     * {
     *   "data": {
     *     "source": {
     *       "isBackData": true,
     *       "sourceType": "triggerApp"
     *     },
     *     "payload": { ...oParams }
     *   }
     * }
     * </p>
     *
     * @param oParams optional parameters to include in the payload (can be null)
     * @return JSON string with fixed request structure
     * @throws RuntimeException if JSON encoding fails
     */
    public static String fixedRequestParameters(Map<String, Object> oParams) {
        try {
            JSONObject mustParameters = new JSONObject();
            JSONObject data = new JSONObject();
            JSONObject source = new JSONObject();
            JSONObject payload = new JSONObject();

            // Build source object
            source.put("isBackData", true);
            source.put("sourceType", "triggerApp");

            // Build payload from oParams
            if (oParams != null && !oParams.isEmpty()) {
                for (Map.Entry<String, Object> entry : oParams.entrySet()) {
                    payload.put(entry.getKey(), entry.getValue());
                }
            }

            // Build data object
            data.put("source", source);
            data.put("payload", payload);

            // Build final structure
            mustParameters.put("data", data);

            return mustParameters.toString();
        } catch (JSONException e) {
            throw new RuntimeException("Failed to encode request parameters", e);
        }
    }

    /**
     * Parse response returned data from wallet socket.
     * <p>
     * Extracts the payload and metadata from the wallet socket response structure.
     * The response format is:
     * {
     *   "code": ...,
     *   "message": ...,
     *   "data": {
     *     "payload": { ... },
     *     "source": { "chainId": ... }
     *   }
     * }
     * </p>
     * <p>
     * Returns a flattened structure:
     * {
     *   "code": ...,
     *   "message": ...,
     *   "data": { ...payload, "chainId": ... }
     * }
     * </p>
     *
     * @param res the response JSON object from wallet socket
     * @return parsed response data with code, message, and flattened data
     * @throws RuntimeException if JSON parsing fails
     */
    public static JSONObject resReturnedData(JSONObject res) {
        try {
            JSONObject oParams = new JSONObject();

            // Extract code and message
            if (res.has("code")) {
                oParams.put("code", res.get("code"));
            }
            if (res.has("message")) {
                oParams.put("message", res.get("message"));
            }

            // Extract payload from data
            if (res.has("data")) {
                JSONObject dataObj = res.getJSONObject("data");

                if (dataObj.has("payload")) {
                    oParams.put("data", dataObj.get("payload"));
                } else {
                    oParams.put("data", new JSONObject());
                }

                // Add chainId if present in source
                if (dataObj.has("source")) {
                    JSONObject sourceObj = dataObj.getJSONObject("source");
                    if (sourceObj.has("chainId")) {
                        JSONObject dataPayload = oParams.getJSONObject("data");
                        dataPayload.put("chainId", sourceObj.get("chainId"));
                        oParams.put("data", dataPayload);
                    }
                }
            } else {
                oParams.put("data", new JSONObject());
            }

            return oParams;
        } catch (JSONException e) {
            throw new RuntimeException("Failed to parse response data", e);
        }
    }

    /**
     * Get window URL (not applicable on mobile).
     * <p>
     * This method exists for compatibility with the web version of the SDK.
     * On mobile platforms, this always returns an empty string.
     * </p>
     *
     * @return empty string
     */
    public static String getWindowUrl() {
        return "";
    }

    /**
     * Get window favicon URL (not applicable on mobile).
     * <p>
     * This method exists for compatibility with the web version of the SDK.
     * On mobile platforms, this always returns an empty string.
     * </p>
     *
     * @return empty string
     */
    public static String getWindowICO() {
        return "";
    }
}

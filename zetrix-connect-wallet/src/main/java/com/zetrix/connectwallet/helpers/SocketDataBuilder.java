package com.zetrix.connectwallet.helpers;

import com.zetrix.connectwallet.constants.ZetrixConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Helper class for building socket data structures.
 * <p>
 * Provides methods to create properly formatted data structures for QR code
 * and standard WebSocket communication with wallet applications.
 * </p>
 * <p>
 * Ported from Flutter SDK's socket_data_builder.dart.
 * </p>
 */
public final class SocketDataBuilder {

    // Prevent instantiation
    private SocketDataBuilder() {
        throw new AssertionError("Cannot instantiate SocketDataBuilder class");
    }

    /**
     * Creates QR code socket data structure.
     * <p>
     * Builds a data structure for QR code mode wallet connection.
     * The structure includes session ID, SDK type, and connection mode.
     * </p>
     *
     * @param param     the parameters including sessionId, type, and data
     * @param isQrcode  whether this is for QR code mode (true) or deep link mode (false)
     * @return JSONObject with QR socket data structure
     * @throws RuntimeException if JSON construction fails
     */
    public static JSONObject createQrSocketData(Map<String, Object> param, boolean isQrcode) {
        try {
            JSONObject socketData = new JSONObject();
            JSONObject innerData = new JSONObject();

            // Get sessionId from param
            String sessionId = param.containsKey("sessionId")
                ? String.valueOf(param.get("sessionId"))
                : "";

            // Get type from param
            String type = param.containsKey("type")
                ? String.valueOf(param.get("type"))
                : "";

            // Build inner data object
            innerData.put("sessionId", sessionId);
            innerData.put("sdkType", type);

            // Add data field if present
            if (param.containsKey("data")) {
                Object dataValue = param.get("data");
                if (dataValue instanceof Map) {
                    JSONObject dataObj = mapToJsonObject((Map<String, Object>) dataValue);
                    innerData.put("data", dataObj);
                } else {
                    innerData.put("data", dataValue);
                }
            }

            // Build outer structure
            socketData.put("type", "H5_" + ZetrixConstants.Operations.SET_QR);
            socketData.put("sessionId", sessionId);
            socketData.put("isH5Connect", !isQrcode);
            socketData.put("data", innerData);

            return socketData;
        } catch (JSONException e) {
            throw new RuntimeException("Failed to create QR socket data", e);
        }
    }

    /**
     * Creates standard socket data structure for wallet operations.
     * <p>
     * Builds a data structure for standard wallet socket communication.
     * Includes app name and package ID for better wallet app identification.
     * </p>
     * <p>
     * Note: This method will retrieve session ID, address, app name, and package ID
     * from StorageUtils and DeviceUtils (to be implemented in Tasks 5-6).
     * </p>
     *
     * @param param     the parameters including type and optional data
     * @param isQrcode  whether this is for QR code mode (true) or deep link mode (false)
     * @param sessionId the session ID from storage
     * @param address   the wallet address from storage (can be null)
     * @param appName   the application name
     * @param appPackageId the application package ID
     * @return JSONObject with socket data structure
     * @throws RuntimeException if JSON construction fails
     */
    public static JSONObject createSocketData(
            Map<String, Object> param,
            boolean isQrcode,
            String sessionId,
            String address,
            String appName,
            String appPackageId) {
        try {
            JSONObject socketData = new JSONObject();
            JSONObject dataObj = new JSONObject();

            // Get type from param
            String type = param.containsKey("type")
                ? String.valueOf(param.get("type"))
                : "";

            // Build data object with app info
            dataObj.put("host", appName != null ? appName : "Unknown App");
            dataObj.put("icon", appPackageId != null ? appPackageId : "unknown.package");
            dataObj.put("address", address != null ? address : "");

            // Add additional data from param if present
            if (param.containsKey("data") && param.get("data") instanceof Map) {
                Map<String, Object> additionalData = (Map<String, Object>) param.get("data");
                for (Map.Entry<String, Object> entry : additionalData.entrySet()) {
                    dataObj.put(entry.getKey(), entry.getValue());
                }
            }

            // Build outer structure
            socketData.put("type", type);
            socketData.put("isH5Connect", !isQrcode);
            socketData.put("sessionId", sessionId != null ? sessionId : "");
            socketData.put("data", dataObj);

            return socketData;
        } catch (JSONException e) {
            throw new RuntimeException("Failed to create socket data", e);
        }
    }

    /**
     * Overloaded method for backward compatibility.
     * <p>
     * This version will be used once StorageUtils and DeviceUtils are implemented.
     * For now, it requires explicit parameters.
     * </p>
     *
     * @param param    the parameters including type and optional data
     * @param isQrcode whether this is for QR code mode
     * @return JSONObject with socket data structure
     */
    public static JSONObject createSocketData(Map<String, Object> param, boolean isQrcode) {
        // TODO: Once StorageUtils and DeviceUtils are implemented (Tasks 5-6),
        // this method should retrieve values like this:
        // String sessionId = StorageUtils.getSessionId();
        // String address = StorageUtils.getAddress();
        // String appName = DeviceUtils.getAppName();
        // String appPackageId = DeviceUtils.getAppPackageId();

        // For now, use empty/default values
        return createSocketData(param, isQrcode, "", null, "Unknown App", "unknown.package");
    }

    /**
     * Helper method to convert a Map to JSONObject.
     *
     * @param map the map to convert
     * @return JSONObject representation of the map
     * @throws JSONException if conversion fails
     */
    private static JSONObject mapToJsonObject(Map<String, Object> map) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        if (map != null) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
        }
        return jsonObject;
    }
}

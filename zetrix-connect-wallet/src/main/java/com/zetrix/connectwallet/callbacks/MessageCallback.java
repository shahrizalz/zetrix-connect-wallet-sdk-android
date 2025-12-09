package com.zetrix.connectwallet.callbacks;

import org.json.JSONObject;

/**
 * Callback interface for WebSocket message responses.
 * <p>
 * Used with the promise pool pattern to match requests with responses.
 * </p>
 */
public interface MessageCallback {

    /**
     * Called when a response is received for the request.
     *
     * @param response the response message as JSONObject
     */
    void onResponse(JSONObject response);

    /**
     * Called when an error occurs or timeout happens.
     *
     * @param error the error that occurred
     */
    void onError(Exception error);
}

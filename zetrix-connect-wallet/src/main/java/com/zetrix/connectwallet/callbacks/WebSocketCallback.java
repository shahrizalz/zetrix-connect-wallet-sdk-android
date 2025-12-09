package com.zetrix.connectwallet.callbacks;

import org.json.JSONObject;

/**
 * Callback interface for WebSocket operations.
 * <p>
 * Provides callbacks for connection events, message handling, and errors.
 * </p>
 */
public interface WebSocketCallback {

    /**
     * Called when WebSocket connection is successfully established.
     *
     * @param authInfo authentication information containing address if authenticated
     */
    void onConnected(JSONObject authInfo);

    /**
     * Called when a message is received from the WebSocket.
     *
     * @param message the received message as JSONObject
     */
    void onMessage(JSONObject message);

    /**
     * Called when the WebSocket connection is closed.
     *
     * @param code   the close code
     * @param reason the close reason
     */
    void onClosed(int code, String reason);

    /**
     * Called when an error occurs.
     *
     * @param error the error that occurred
     */
    void onError(Exception error);
}

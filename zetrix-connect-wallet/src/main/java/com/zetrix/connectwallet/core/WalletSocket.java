package com.zetrix.connectwallet.core;

import com.zetrix.connectwallet.callbacks.MessageCallback;
import com.zetrix.connectwallet.callbacks.WebSocketCallback;
import com.zetrix.connectwallet.constants.ZetrixConstants;
import com.zetrix.connectwallet.utils.StorageUtils;
import com.zetrix.connectwallet.utils.ZetrixLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * WalletSocket class for managing WebSocket connections to Zetrix wallet servers.
 * <p>
 * Handles real-time bidirectional communication with wallet applications
 * for authentication, signing, and transaction operations.
 * </p>
 * <p>
 * Features:
 * - Auto-reconnection on connection loss
 * - Promise pool pattern for request/response matching
 * - Thread-safe message handling
 * - Session restoration after reconnect
 * </p>
 * <p>
 * Ported from Flutter SDK's wallet_socket.dart.
 * </p>
 */
public class WalletSocket {

    private static final ZetrixLogger logger = ZetrixLogger.getLogger("WalletSocket");
    private static final long RECONNECT_DELAY_MS = 3000; // 3 seconds

    private final String url;
    private final OkHttpClient httpClient;
    private final Map<String, MessageCallback> promisePool;
    private final WebSocketCallback connectionCallback;

    private WebSocket webSocket;
    private boolean closing;
    private boolean connected;

    /**
     * Create a new WalletSocket instance.
     *
     * @param url                WebSocket URL (mainnet or testnet)
     * @param connectionCallback callback for connection events (can be null)
     */
    public WalletSocket(String url, WebSocketCallback connectionCallback) {
        this.url = url;
        this.connectionCallback = connectionCallback;
        this.promisePool = new ConcurrentHashMap<>();
        this.closing = false;
        this.connected = false;

        // Create HTTP client with WebSocket support
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for WebSocket
                .writeTimeout(30, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS) // Keep-alive ping
                .build();
    }

    /**
     * Create a new WalletSocket instance with default settings.
     *
     * @param url WebSocket URL
     */
    public WalletSocket(String url) {
        this(url, null);
    }

    /**
     * Connect to the WebSocket server.
     * <p>
     * Establishes connection, sets up listeners, and attempts to restore session.
     * </p>
     */
    public void connect() {
        if (connected) {
            logger.warning("Already connected to WebSocket");
            return;
        }

        logger.info("Connecting to WebSocket: " + url);
        closing = false;

        Request request = new Request.Builder()
                .url(url)
                .build();

        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                logger.info("WebSocket connection established");
                connected = true;

                // Send bind message if session exists
                afterOpenToBind();

                // Notify connection callback
                if (connectionCallback != null) {
                    JSONObject authInfo = callbackAuthInfo();
                    connectionCallback.onConnected(authInfo);
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                logger.info("Received WebSocket message: " + text);
                handleMessage(text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                logger.info("Received binary WebSocket message");
                handleMessage(bytes.utf8());
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                logger.info("WebSocket closing: " + code + " - " + reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                logger.info("WebSocket closed: " + code + " - " + reason);
                connected = false;

                if (connectionCallback != null) {
                    connectionCallback.onClosed(code, reason);
                }

                // Auto-reconnect if not manually closed
                if (!closing) {
                    logger.info("Attempting to reconnect in " + RECONNECT_DELAY_MS + "ms");
                    scheduleReconnect();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                logger.severe("WebSocket error: " + t.getMessage(), t);
                connected = false;

                Exception error = new Exception("WebSocket connection failed", t);
                if (connectionCallback != null) {
                    connectionCallback.onError(error);
                }

                // Auto-reconnect on failure if not manually closed
                if (!closing) {
                    logger.info("Reconnecting after failure in " + RECONNECT_DELAY_MS + "ms");
                    scheduleReconnect();
                }
            }
        });
    }

    /**
     * Handle incoming WebSocket message.
     *
     * @param message the message text
     */
    private void handleMessage(String message) {
        try {
            JSONObject resp = new JSONObject(message);

            // Notify connection callback
            if (connectionCallback != null) {
                connectionCallback.onMessage(resp);
            }

            // Check if message has type field
            if (!resp.has("type")) {
                logger.warning("Message missing 'type' field");
                return;
            }

            String type = resp.getString("type");
            logger.fine("Message type: " + type);

            // Extract operation type from message type (e.g., "app_auth" -> "auth")
            String[] parts = type.split("_");
            if (parts.length < 2) {
                logger.warning("Invalid message type format: " + type);
                return;
            }

            String operation = parts[1];

            // Convert "auth" to "bind" for consistency
            if ("auth".equals(operation)) {
                operation = "bind";
            }

            // Reconstruct type as "H5_{operation}"
            String lookupKey = "H5_" + operation;
            logger.fine("Looking for promise with key: " + lookupKey);

            // Find and complete the promise
            MessageCallback callback = promisePool.remove(lookupKey);
            if (callback != null) {
                logger.fine("Completing promise for: " + lookupKey);
                callback.onResponse(resp);
            } else {
                logger.warning("No promise found for type: " + lookupKey);
            }

        } catch (JSONException e) {
            logger.severe("Error parsing WebSocket message", e);
        }
    }

    /**
     * Send data over WebSocket and return response via callback.
     *
     * @param method   the method/type identifier
     * @param data     the data to send
     * @param callback callback for the response
     */
    public void send(String method, JSONObject data, MessageCallback callback) {
        if (!connected || webSocket == null) {
            callback.onError(new Exception("WebSocket is not connected"));
            return;
        }

        try {
            // Store callback in promise pool
            promisePool.put(method, callback);

            // Add type to data
            data.put("type", method);

            // Send message
            String message = data.toString();
            logger.info("Sending WebSocket message: " + message);
            boolean sent = webSocket.send(message);

            if (!sent) {
                promisePool.remove(method);
                callback.onError(new Exception("Failed to send message"));
            }

        } catch (JSONException e) {
            promisePool.remove(method);
            callback.onError(new Exception("Error preparing message", e));
        }
    }

    /**
     * Send H5 bind message.
     *
     * @param data     the bind data
     * @param callback callback for the response
     */
    public void h5Bind(JSONObject data, MessageCallback callback) {
        logger.fine("h5Bind: " + data.toString());
        send("H5_" + ZetrixConstants.Operations.BIND, data, callback);
    }

    /**
     * Send bind message after connection if sessionId exists.
     * <p>
     * This restores the session after reconnection.
     * </p>
     */
    private void afterOpenToBind() {
        String sessionId = StorageUtils.getSessionId();
        logger.info("afterOpenToBind, sessionId: " + sessionId);

        if (sessionId != null && !sessionId.isEmpty()) {
            try {
                JSONObject sendBind = new JSONObject();
                sendBind.put("type", "H5_bind");
                sendBind.put("sessionId", sessionId);

                String message = sendBind.toString();
                logger.info("Sending auto-bind message: " + message);
                webSocket.send(message);

            } catch (JSONException e) {
                logger.severe("Error creating bind message", e);
            }
        }
    }

    /**
     * Get callback auth info from storage.
     *
     * @return JSONObject with code and data (address if authenticated)
     */
    private JSONObject callbackAuthInfo() {
        try {
            String address = StorageUtils.getAddress();
            String sessionId = StorageUtils.getSessionId();

            JSONObject authInfo = new JSONObject();
            authInfo.put("code", 0);

            JSONObject data = new JSONObject();
            if (address != null && !address.isEmpty() && sessionId != null && !sessionId.isEmpty()) {
                data.put("address", address);
            }
            authInfo.put("data", data);

            return authInfo;

        } catch (JSONException e) {
            logger.severe("Error creating auth info", e);
            try {
                JSONObject errorInfo = new JSONObject();
                errorInfo.put("code", -1);
                errorInfo.put("data", new JSONObject());
                return errorInfo;
            } catch (JSONException ex) {
                return new JSONObject();
            }
        }
    }

    /**
     * Schedule reconnection attempt.
     */
    private void scheduleReconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(RECONNECT_DELAY_MS);
                if (!closing) {
                    logger.info("Attempting reconnection...");
                    connect();
                }
            } catch (InterruptedException e) {
                logger.warning("Reconnect interrupted", e);
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Disconnect the WebSocket.
     * <p>
     * Closes the connection and prevents auto-reconnection.
     * </p>
     */
    public void disconnect() {
        logger.info("Disconnecting WebSocket");
        closing = true;
        connected = false;

        if (webSocket != null) {
            webSocket.close(1000, "Normal closure");
            webSocket = null;
        }

        // Clear promise pool
        promisePool.clear();
    }

    /**
     * Check if WebSocket is connected.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Get the WebSocket URL.
     *
     * @return the URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Get the number of pending promises.
     *
     * @return promise pool size
     */
    public int getPendingPromises() {
        return promisePool.size();
    }
}

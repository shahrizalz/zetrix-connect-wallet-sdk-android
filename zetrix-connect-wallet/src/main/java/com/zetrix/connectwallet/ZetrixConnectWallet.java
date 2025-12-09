package com.zetrix.connectwallet;

import android.content.Context;

import com.zetrix.connectwallet.blockchain.ChainSDK;
import com.zetrix.connectwallet.callbacks.MessageCallback;
import com.zetrix.connectwallet.callbacks.WebSocketCallback;
import com.zetrix.connectwallet.constants.ZetrixConstants;
import com.zetrix.connectwallet.core.WalletSocket;
import com.zetrix.connectwallet.helpers.SessionHelper;
import com.zetrix.connectwallet.helpers.SocketDataBuilder;
import com.zetrix.connectwallet.helpers.ValidationHelper;
import com.zetrix.connectwallet.utils.CryptoUtils;
import com.zetrix.connectwallet.utils.DeviceUtils;
import com.zetrix.connectwallet.utils.StorageUtils;
import com.zetrix.connectwallet.utils.ZetrixLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Zetrix Connect Wallet SDK for Android.
 * <p>
 * Main entry point for integrating Zetrix wallet connectivity into Android applications.
 * This SDK enables applications to connect to Zetrix wallet apps (Zetrix Wallet, PIXA, MyID, MUMA)
 * for authentication, message signing, and transaction operations.
 * </p>
 * <p>
 * Features:
 * - Connect to external wallet applications
 * - Request user authentication
 * - Sign messages and binary data
 * - Send blockchain transactions
 * - Verify credentials (VC) and get presentations (VP)
 * - Secure session management
 * </p>
 * <p>
 * Ported from Flutter SDK's wallet_connect.dart.
 * </p>
 * <p>
 * Usage:
 * <pre>
 * // Initialize SDK
 * ZetrixConnectWallet sdk = new ZetrixConnectWallet.Builder(context)
 *     .setAppType("myapp")
 *     .setTestnet(true)
 *     .build();
 *
 * // Initialize storage
 * sdk.initialize();
 *
 * // Connect to wallet
 * sdk.connect(new WebSocketCallback() {
 *     public void onConnected(JSONObject authInfo) {
 *         // Connection established
 *     }
 *     // ... other callbacks
 * });
 *
 * // Request authentication
 * sdk.auth(new AuthCallback() {
 *     public void onSuccess(String address) {
 *         // User authenticated
 *     }
 *     public void onError(String error) {
 *         // Handle error
 *     }
 * });
 * </pre>
 * </p>
 */
public class ZetrixConnectWallet {

    private static final ZetrixLogger logger = ZetrixLogger.getLogger("ZetrixConnectWallet");

    private final Context context;
    private final String appType;
    private final boolean testnet;
    private final String bridgeUrl;

    private WalletSocket walletSocket;
    private ChainSDK chainSDK;
    private boolean connected;

    /**
     * Private constructor. Use Builder to create instances.
     */
    private ZetrixConnectWallet(Builder builder) {
        this.context = builder.context.getApplicationContext();
        this.appType = builder.appType;
        this.testnet = builder.testnet;
        this.bridgeUrl = builder.bridgeUrl != null
                ? builder.bridgeUrl
                : (testnet
                        ? ZetrixConstants.WebSocket.WSS_TESTNET
                        : ZetrixConstants.WebSocket.WSS_MAINNET);
        this.connected = false;
    }

    /**
     * Initialize the SDK.
     * <p>
     * Must be called before using any SDK methods.
     * Initializes secure storage and other components.
     * </p>
     */
    public void initialize() {
        // Initialize storage
        StorageUtils.initialize(context);

        // Initialize logger
        ZetrixLogger.initialize(ZetrixLogger.Level.INFO, true);

        // Create ChainSDK
        chainSDK = new ChainSDK();

        logger.info("ZetrixConnectWallet initialized");
    }

    /**
     * Connect to the WebSocket server.
     *
     * @param callback callback for connection events
     */
    public void connect(WebSocketCallback callback) {
        String wsUrl = bridgeUrl + "/api/websocket/server";
        logger.info("Connecting to WebSocket: " + wsUrl);

        walletSocket = new WalletSocket(wsUrl, callback);
        walletSocket.connect();
        connected = true;
    }

    /**
     * Request authentication from wallet.
     * <p>
     * Opens the wallet app and requests the user to authenticate.
     * Returns the wallet address upon successful authentication.
     * </p>
     *
     * @param callback callback for authentication result
     */
    public void auth(AuthCallback callback) {
        if (!ensureConnected(callback)) return;

        try {
            String sessionId = SessionHelper.createSessionId();
            String appName = DeviceUtils.getAppName(context);
            boolean isMobile = DeviceUtils.isMobile();

            JSONObject bindData = new JSONObject();
            bindData.put("type", "H5_" + ZetrixConstants.Operations.BIND);
            bindData.put("sessionId", sessionId);
            bindData.put("source", isMobile ? ZetrixConstants.TypeInfo.MOBILE_SOURCE : "");

            walletSocket.h5Bind(bindData, new MessageCallback() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        int code = response.optInt("code", -1);
                        if (code == 0) {
                            String resSessionId = response.optString("sessionId", sessionId);
                            JSONObject data = response.optJSONObject("data");
                            if (data != null) {
                                String address = data.optString("address");
                                StorageUtils.setAuthData(resSessionId, address);
                                logger.info("Authentication successful: " + address);
                                callback.onSuccess(address, resSessionId);
                            } else {
                                callback.onError("No data in response");
                            }
                        } else {
                            String message = response.optString("message", "Authentication failed");
                            logger.warning("Auth error: " + message);
                            callback.onError(message);
                        }
                    } catch (Exception e) {
                        logger.severe("Error processing auth response", e);
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }

                @Override
                public void onError(Exception error) {
                    logger.severe("Auth request failed", error);
                    callback.onError(error.getMessage());
                }
            });

            // NOTE: In production, you would launch the wallet app here via deep link
            // For now, the wallet needs to be opened manually by the user

        } catch (JSONException e) {
            logger.severe("Error creating auth request", e);
            callback.onError("Error creating auth request: " + e.getMessage());
        }
    }

    /**
     * Request wallet to sign a message.
     *
     * @param message  the message to sign
     * @param callback callback for signing result
     */
    public void signMessage(String message, SignCallback callback) {
        if (!ensureAuthenticated(callback)) return;

        Map<String, Object> param = new HashMap<>();
        param.put("message", message);

        String validationError = ValidationHelper.verifyBlobParam(param);
        if (!validationError.isEmpty()) {
            callback.onError(SessionHelper.warnIncorrectLog(validationError));
            return;
        }

        try {
            String sessionId = StorageUtils.getSessionId();
            String appName = DeviceUtils.getAppName(context);
            boolean isMobile = DeviceUtils.isMobile();

            Map<String, Object> socketParam = new HashMap<>();
            socketParam.put("type", "H5_" + ZetrixConstants.Operations.SIGN_MESSAGE);

            Map<String, Object> data = new HashMap<>();
            data.put("message", message);
            data.put("source", isMobile ? ZetrixConstants.TypeInfo.MOBILE_SOURCE : "");
            socketParam.put("data", data);

            JSONObject socketData = SocketDataBuilder.createSocketData(
                    socketParam, false, sessionId, StorageUtils.getAddress(), appName,
                    DeviceUtils.getAppPackageId(context));

            walletSocket.send("H5_" + ZetrixConstants.Operations.SIGN_MESSAGE,
                    socketData, new MessageCallback() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        int code = response.optInt("code", -1);
                        if (code == 0) {
                            JSONObject data = response.optJSONObject("data");
                            if (data != null) {
                                String address = data.optString("address");
                                String publicKey = data.optString("publicKey");
                                String signData = data.optString("signData");
                                logger.info("Message signed successfully");
                                callback.onSuccess(address, publicKey, signData);
                            } else {
                                callback.onError("No data in response");
                            }
                        } else {
                            String errorMsg = response.optString("message", "Signing failed");
                            callback.onError(errorMsg);
                        }
                    } catch (Exception e) {
                        logger.severe("Error processing sign response", e);
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }

                @Override
                public void onError(Exception error) {
                    logger.severe("Sign request failed", error);
                    callback.onError(error.getMessage());
                }
            });

        } catch (Exception e) {
            logger.severe("Error creating sign request", e);
            callback.onError("Error creating sign request: " + e.getMessage());
        }
    }

    /**
     * Request wallet to sign binary data (blob).
     *
     * @param message  the blob message to sign
     * @param callback callback for signing result
     */
    public void signBlob(String message, SignCallback callback) {
        if (!ensureAuthenticated(callback)) return;

        Map<String, Object> param = new HashMap<>();
        param.put("message", message);

        String validationError = ValidationHelper.verifyBlobParam(param);
        if (!validationError.isEmpty()) {
            callback.onError(SessionHelper.warnIncorrectLog(validationError));
            return;
        }

        try {
            String sessionId = StorageUtils.getSessionId();
            String appName = DeviceUtils.getAppName(context);
            boolean isMobile = DeviceUtils.isMobile();

            Map<String, Object> socketParam = new HashMap<>();
            socketParam.put("type", "H5_" + ZetrixConstants.Operations.SIGN_BLOB);

            Map<String, Object> data = new HashMap<>();
            data.put("message", message);
            data.put("source", isMobile ? ZetrixConstants.TypeInfo.MOBILE_SOURCE : "");
            socketParam.put("data", data);

            JSONObject socketData = SocketDataBuilder.createSocketData(
                    socketParam, false, sessionId, StorageUtils.getAddress(), appName,
                    DeviceUtils.getAppPackageId(context));

            walletSocket.send("H5_" + ZetrixConstants.Operations.SIGN_BLOB,
                    socketData, new MessageCallback() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        int code = response.optInt("code", -1);
                        if (code == 0) {
                            JSONObject data = response.optJSONObject("data");
                            if (data != null) {
                                String address = data.optString("address");
                                String publicKey = data.optString("publicKey");
                                String signData = data.optString("signData");
                                logger.info("Blob signed successfully");
                                callback.onSuccess(address, publicKey, signData);
                            } else {
                                callback.onError("No data in response");
                            }
                        } else {
                            String errorMsg = response.optString("message", "Signing failed");
                            callback.onError(errorMsg);
                        }
                    } catch (Exception e) {
                        logger.severe("Error processing sign blob response", e);
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }

                @Override
                public void onError(Exception error) {
                    logger.severe("Sign blob request failed", error);
                    callback.onError(error.getMessage());
                }
            });

        } catch (Exception e) {
            logger.severe("Error creating sign blob request", e);
            callback.onError("Error creating sign blob request: " + e.getMessage());
        }
    }

    /**
     * Send a transaction through the wallet.
     *
     * @param from     sender address
     * @param to       recipient address
     * @param amount   transaction amount
     * @param gasFee   gas fee
     * @param nonce    account nonce
     * @param callback callback for transaction result
     */
    public void sendTransaction(String from, String to, String amount, String gasFee,
                                long nonce, TransactionCallback callback) {
        if (!ensureAuthenticated(callback)) return;

        Map<String, Object> param = new HashMap<>();
        param.put("from", from);
        param.put("to", to);
        param.put("amount", amount);
        param.put("gasFee", gasFee);
        param.put("nonce", nonce);

        String validationError = ValidationHelper.verifyTransactionParam(param);
        if (!validationError.isEmpty()) {
            callback.onError(SessionHelper.warnIncorrectLog(validationError));
            return;
        }

        try {
            // Add HMAC for transaction integrity
            String hmac = CryptoUtils.hmacStr(to + "&" + amount);
            param.put("hmac", hmac);

            String sessionId = StorageUtils.getSessionId();
            String appName = DeviceUtils.getAppName(context);
            boolean isMobile = DeviceUtils.isMobile();

            Map<String, Object> socketParam = new HashMap<>();
            socketParam.put("type", "H5_" + ZetrixConstants.Operations.SEND_TRANSACTION);

            Map<String, Object> data = new HashMap<>();
            data.putAll(param);
            data.put("source", isMobile ? ZetrixConstants.TypeInfo.MOBILE_SOURCE : "");
            socketParam.put("data", data);

            JSONObject socketData = SocketDataBuilder.createSocketData(
                    socketParam, false, sessionId, StorageUtils.getAddress(), appName,
                    DeviceUtils.getAppPackageId(context));

            walletSocket.send("H5_" + ZetrixConstants.Operations.SEND_TRANSACTION,
                    socketData, new MessageCallback() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        int code = response.optInt("code", -1);
                        if (code == 0) {
                            JSONObject data = response.optJSONObject("data");
                            if (data != null) {
                                String hash = data.optString("hash");
                                logger.info("Transaction sent successfully: " + hash);
                                callback.onSuccess(hash);
                            } else {
                                callback.onError("No data in response");
                            }
                        } else {
                            String errorMsg = response.optString("message", "Transaction failed");
                            callback.onError(errorMsg);
                        }
                    } catch (Exception e) {
                        logger.severe("Error processing transaction response", e);
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }

                @Override
                public void onError(Exception error) {
                    logger.severe("Transaction request failed", error);
                    callback.onError(error.getMessage());
                }
            });

        } catch (Exception e) {
            logger.severe("Error creating transaction request", e);
            callback.onError("Error creating transaction request: " + e.getMessage());
        }
    }

    /**
     * Get account nonce from the blockchain.
     *
     * @param address  the wallet address
     * @param chainId  the chain ID ("1" for mainnet, "2" for testnet)
     * @param callback callback for nonce result
     */
    public void getNonce(String address, String chainId, NonceCallback callback) {
        chainSDK.getAccountNonce(address, chainId, new ChainSDK.NonceCallback() {
            @Override
            public void onSuccess(long nonce) {
                callback.onSuccess(nonce);
            }

            @Override
            public void onError(Exception error) {
                callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Disconnect from wallet and clear session data.
     */
    public void disconnect() {
        logger.info("Disconnecting from wallet");
        if (walletSocket != null) {
            walletSocket.disconnect();
        }
        StorageUtils.disconnectRemoveStorage();
        connected = false;
    }

    /**
     * Check if wallet is connected.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connected && walletSocket != null && walletSocket.isConnected();
    }

    /**
     * Get the current session ID.
     *
     * @return session ID or null if not authenticated
     */
    public String getSessionId() {
        return StorageUtils.getSessionId();
    }

    /**
     * Get the authenticated wallet address.
     *
     * @return wallet address or null if not authenticated
     */
    public String getAddress() {
        return StorageUtils.getAddress();
    }

    /**
     * Ensure WebSocket is connected.
     */
    private boolean ensureConnected(BaseCallback callback) {
        if (!connected || walletSocket == null) {
            callback.onError("Not connected. Call connect() first.");
            return false;
        }
        return true;
    }

    /**
     * Ensure user is authenticated.
     */
    private boolean ensureAuthenticated(BaseCallback callback) {
        if (!ensureConnected(callback)) return false;

        String sessionId = StorageUtils.getSessionId();
        String address = StorageUtils.getAddress();

        if (sessionId == null || address == null) {
            callback.onError("Not authenticated. Call auth() first.");
            return false;
        }
        return true;
    }

    // ========== Callback Interfaces ==========

    /**
     * Base callback interface.
     */
    public interface BaseCallback {
        void onError(String error);
    }

    /**
     * Authentication callback.
     */
    public interface AuthCallback extends BaseCallback {
        void onSuccess(String address, String sessionId);
    }

    /**
     * Signing callback.
     */
    public interface SignCallback extends BaseCallback {
        void onSuccess(String address, String publicKey, String signData);
    }

    /**
     * Transaction callback.
     */
    public interface TransactionCallback extends BaseCallback {
        void onSuccess(String transactionHash);
    }

    /**
     * Nonce callback.
     */
    public interface NonceCallback extends BaseCallback {
        void onSuccess(long nonce);
    }

    // ========== Builder Pattern ==========

    /**
     * Builder for ZetrixConnectWallet.
     */
    public static class Builder {
        private final Context context;
        private String appType = "zetrix";
        private boolean testnet = false;
        private String bridgeUrl = null;

        public Builder(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }
            this.context = context;
        }

        public Builder setAppType(String appType) {
            this.appType = appType;
            return this;
        }

        public Builder setTestnet(boolean testnet) {
            this.testnet = testnet;
            return this;
        }

        public Builder setBridgeUrl(String bridgeUrl) {
            this.bridgeUrl = bridgeUrl;
            return this;
        }

        public ZetrixConnectWallet build() {
            return new ZetrixConnectWallet(this);
        }
    }
}

package com.zetrix.connectwallet;

import android.content.Context;

import com.zetrix.connectwallet.blockchain.ChainSDK;
import com.zetrix.connectwallet.callbacks.MessageCallback;
import com.zetrix.connectwallet.callbacks.WebSocketCallback;
import com.zetrix.connectwallet.constants.ZetrixConstants;
import com.zetrix.connectwallet.core.WalletSocket;
import com.zetrix.connectwallet.helpers.DeepLinkHelper;
import com.zetrix.connectwallet.helpers.SessionHelper;
import com.zetrix.connectwallet.helpers.SocketDataBuilder;
import com.zetrix.connectwallet.helpers.ValidationHelper;
import com.zetrix.connectwallet.ui.QRCodeActivity;
import com.zetrix.connectwallet.utils.CryptoUtils;
import com.zetrix.connectwallet.utils.DeviceUtils;
import com.zetrix.connectwallet.utils.StorageUtils;
import com.zetrix.connectwallet.utils.ZetrixLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    private final boolean isQrcode;
    private final boolean testnet;
    private final String bridgeUrl;

    private WalletSocket walletSocket;
    private ChainSDK chainSDK;
    private boolean connected;

    /**
     * Private constructor. Use Builder to create instances.
     * <p>
     * The SDK will internally launch a QRCodeActivity when needed to display QR codes.
     * </p>
     */
    private ZetrixConnectWallet(Builder builder) {
        this.context = builder.context;
        this.appType = builder.appType;
        this.isQrcode = builder.isQrcode;
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
     * Request authentication from wallet with QR code or deep link.
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
            String appPackageId = DeviceUtils.getAppPackageId(context);
            boolean isMobile = DeviceUtils.isMobile();

            String authType = "H5_" + ZetrixConstants.Operations.BIND;

            // Send bind request for authentication
            JSONObject bindData = new JSONObject();
            bindData.put("type", authType);
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

                                // Close QR activity if it's showing
                                closeQrActivity();

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

            // Launch wallet app or show QR code
            if (isQrcode) {
                // For QR code: send H5_put to get rms token, then show QR
                logger.info("Requesting QR code data from server");

                Map<String, Object> qrParam = new HashMap<>();
                qrParam.put("sessionId", sessionId);
                qrParam.put("type", authType);

                Map<String, Object> qrData = new HashMap<>();
                qrData.put("icon", appPackageId);
                qrData.put("host", appName);
                qrData.put("type", "H5_" + ZetrixConstants.Operations.AUTH);
                qrParam.put("data", qrData);

                JSONObject qrSocketData = SocketDataBuilder.createQrSocketData(qrParam, true);

                walletSocket.send("H5_" + ZetrixConstants.Operations.SET_QR, qrSocketData, new MessageCallback() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String rms = response.optString("rms", "");
                            if (!rms.isEmpty()) {
                                // Format: "{rms}&{sessionId}&{type}"
                                String qrCodeData = rms + "&" + sessionId + "&" + authType;
                                logger.info("Showing QR code for authentication");
                                QRCodeActivity.launch(context, qrCodeData, appType);
                            } else {
                                logger.warning("No rms token in response, cannot generate QR code");
                            }
                        } catch (Exception e) {
                            logger.severe("Error processing QR response", e);
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        logger.severe("QR request failed", error);
                    }
                });
            } else {
                // Launch wallet app via deep link
                logger.info("Launching wallet app via deep link");

                // Build params for deep link (following Flutter pattern)
                Map<String, Object> deepLinkParams = new HashMap<>();
                deepLinkParams.put("linkTo", authType);
                deepLinkParams.put("type", authType);
                deepLinkParams.put("host", appName);
                deepLinkParams.put("icon", appPackageId);
                deepLinkParams.put("sessionId", sessionId);
                deepLinkParams.put("source", isMobile ? ZetrixConstants.TypeInfo.MOBILE_SOURCE : "");

                boolean launched = DeepLinkHelper.launchWalletApp(context, appType, testnet, deepLinkParams);
                if (!launched) {
                    logger.warning("Failed to launch wallet app - user may need to open it manually");
                }
            }

        } catch (JSONException e) {
            logger.severe("Error creating auth request", e);
            callback.onError("Error creating auth request: " + e.getMessage());
        }
    }

    /**
     * Request authentication from wallet using default method (deep link).
     * <p>
     * This is a convenience method that calls auth(false, callback).
     * For QR code authentication, use auth(true, callback).
     * </p>
     *
     * @param callback callback for authentication result
     */
//    public void auth(AuthCallback callback) {
//        auth(callback);
//    }

    /**
     * Authenticate and sign message in a single operation.
     * <p>
     * This is a convenience method that combines authentication (bind) and message signing.
     * The user authenticates and signs the message in one step.
     * </p>
     * <p>
     * Follows Flutter SDK pattern from wallet_connect.dart lines 224-310.
     * </p>
     *
     * @param message  the message to sign
     * @param callback callback for auth and sign result
     */
    public void authAndSignMessage(String message, AuthAndSignCallback callback) {
        if (!ensureConnected(callback)) return;

        // Validate message parameter
        if (message == null || message.isEmpty()) {
            callback.onError("Required parameter missing: message");
            return;
        }

        try {
            String sessionId = SessionHelper.createSessionId();
            String appName = DeviceUtils.getAppName(context);
            String appPackageId = DeviceUtils.getAppPackageId(context);
            boolean isMobile = DeviceUtils.isMobile();

            String authAndSignType = "H5_" + ZetrixConstants.Operations.BIND_AND_SIGN_MESSAGE;

            // Step 1: Send bind request first
            logger.info("Sending bind request for authAndSignMessage: " + sessionId);
            JSONObject bindData = new JSONObject();
            bindData.put("type", authAndSignType);
            bindData.put("sessionId", sessionId);

            walletSocket.h5Bind(bindData, new MessageCallback() {
                @Override
                public void onResponse(JSONObject response) {
                    logger.info("Bind request sent for authAndSignMessage");
                }

                @Override
                public void onError(Exception error) {
                    logger.warning("h5Bind failed for authAndSignMessage", error);
                    // Don't fail the operation, continue with sign request
                }
            });

            // Store session with empty address initially
            StorageUtils.setAuthData(sessionId, "");

            // Step 2: Send bindAndSignMessage request
            logger.info("Sending bindAndSignMessage request");
            Map<String, Object> socketParam = new HashMap<>();
            socketParam.put("type", authAndSignType);
            socketParam.put("linkTo", authAndSignType);
            socketParam.put("host", appName);
            socketParam.put("icon", appPackageId);
            socketParam.put("sessionId", sessionId);
            socketParam.put("source", isMobile ? ZetrixConstants.TypeInfo.MOBILE_SOURCE : "");
            socketParam.put("message", message);

            JSONObject socketData = new JSONObject(socketParam);

            walletSocket.send("H5_" + ZetrixConstants.Operations.BIND_AND_SIGN_MESSAGE,
                    socketData, new MessageCallback() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        int code = response.optInt("code", -1);
                        if (code == 0) {
                            String resSessionId = response.optString("sessionId", sessionId);
                            JSONObject data = response.optJSONObject("data");
                            if (data != null) {
                                String address = data.optString("address");
                                String publicKey = data.optString("publicKey");
                                String signData = data.optString("signData");

                                // Store auth data
                                StorageUtils.setAuthData(resSessionId, address);
                                logger.info("AuthAndSignMessage successful: " + address);

                                // Close QR activity if it's showing
                                closeQrActivity();

                                callback.onSuccess(resSessionId, address, publicKey, signData);
                            } else {
                                callback.onError("No data in response");
                            }
                        } else {
                            String errorMsg = response.optString("message", "AuthAndSignMessage failed");
                            callback.onError(errorMsg);
                        }
                    } catch (Exception e) {
                        logger.severe("Error processing authAndSignMessage response", e);
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }

                @Override
                public void onError(Exception error) {
                    logger.severe("AuthAndSignMessage request failed", error);
                    callback.onError(error.getMessage());
                }
            });

            // Step 3: Launch wallet app or show QR code
            if (isQrcode) {
                // For QR code: send H5_put to get rms token, then show QR
                logger.info("Requesting QR code data for authAndSignMessage");

                Map<String, Object> qrParam = new HashMap<>();
                qrParam.put("sessionId", sessionId);
                qrParam.put("type", authAndSignType);

                Map<String, Object> qrData = new HashMap<>();
                qrData.put("icon", appPackageId);
                qrData.put("host", appName);
                qrData.put("message", message);
                qrData.put("type", authAndSignType);
                qrParam.put("data", qrData);

                JSONObject qrSocketData = SocketDataBuilder.createQrSocketData(qrParam, true);

                walletSocket.send("H5_" + ZetrixConstants.Operations.SET_QR, qrSocketData, new MessageCallback() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String rms = response.optString("rms", "");
                            if (!rms.isEmpty()) {
                                // Format: "{rms}&{sessionId}&{type}"
                                String qrCodeData = rms + "&" + sessionId + "&" + authAndSignType;
                                logger.info("Showing QR code for authAndSignMessage");
                                QRCodeActivity.launch(context, qrCodeData, appType);
                            } else {
                                logger.warning("No rms token in response, cannot generate QR code");
                            }
                        } catch (Exception e) {
                            logger.severe("Error processing QR response for authAndSignMessage", e);
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        logger.severe("QR request failed for authAndSignMessage", error);
                    }
                });
            } else {
                // Launch wallet app via deep link
                logger.info("Launching wallet app via deep link for authAndSignMessage");

                Map<String, Object> deepLinkParams = new HashMap<>();
                deepLinkParams.put("linkTo", authAndSignType);
                deepLinkParams.put("type", authAndSignType);
                deepLinkParams.put("host", appName);
                deepLinkParams.put("icon", appPackageId);
                deepLinkParams.put("sessionId", sessionId);
                deepLinkParams.put("source", isMobile ? ZetrixConstants.TypeInfo.MOBILE_SOURCE : "");
                deepLinkParams.put("message", message);

                boolean launched = DeepLinkHelper.launchWalletApp(context, appType, testnet, deepLinkParams);
                if (!launched) {
                    logger.warning("Failed to launch wallet app for authAndSignMessage");
                }
            }

        } catch (JSONException e) {
            logger.severe("Error creating authAndSignMessage request", e);
            callback.onError("Error creating request: " + e.getMessage());
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

            // Launch wallet app via deep link if not using QR code (following Flutter pattern)
            if (!isQrcode) {
                Map<String, Object> urlObj = new HashMap<>();
                urlObj.put("linkTo", "H5_" + ZetrixConstants.Operations.SIGN_MESSAGE);
                urlObj.put("sessionId", sessionId);
                urlObj.put("host", appName);
                urlObj.put("icon", "");

                DeepLinkHelper.launchWalletApp(context, appType, testnet, urlObj);
            }

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

            // Launch wallet app via deep link if not using QR code (following Flutter pattern)
            if (!isQrcode) {
                Map<String, Object> urlObj = new HashMap<>();
                urlObj.put("linkTo", "H5_" + ZetrixConstants.Operations.SIGN_BLOB);
                urlObj.put("sessionId", sessionId);
                urlObj.put("host", appName);
                urlObj.put("icon", "");

                DeepLinkHelper.launchWalletApp(context, appType, testnet, urlObj);
            }

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

            // Launch wallet app via deep link if not using QR code (following Flutter pattern)
            if (!isQrcode) {
                Map<String, Object> urlObj = new HashMap<>();
                urlObj.put("linkTo", "H5_" + ZetrixConstants.Operations.SEND_TRANSACTION);
                urlObj.put("sessionId", sessionId);
                urlObj.put("host", appName);
                urlObj.put("icon", "");
                urlObj.put("tag", to);  // Include "to" address as tag (Flutter line 490)

                DeepLinkHelper.launchWalletApp(context, appType, testnet, urlObj);
            }

        } catch (Exception e) {
            logger.severe("Error creating transaction request", e);
            callback.onError("Error creating transaction request: " + e.getMessage());
        }
    }

    /**
     * Request wallet to verify a Verifiable Credential (VC).
     * <p>
     * Verifiable Credentials are digital credentials that can be cryptographically verified.
     * This method requests the wallet to verify a VC against a specific template.
     * </p>
     * <p>
     * Follows Flutter SDK pattern from wallet_connect.dart lines 501-580.
     * </p>
     *
     * @param templateId the VC template ID to verify against
     * @param callback   callback for verification result
     */
    public void verifyVC(String templateId, VCCallback callback) {
        if (!ensureAuthenticated(callback)) return;

        // Validate templateId parameter
        if (templateId == null || templateId.isEmpty()) {
            callback.onError("Required parameter missing: templateId");
            return;
        }

        try {
            String sessionId = StorageUtils.getSessionId();
            String address = StorageUtils.getAddress();
            String appName = DeviceUtils.getAppName(context);
            boolean isMobile = DeviceUtils.isMobile();

            String vcType = "H5_" + ZetrixConstants.Operations.VERIFY_VC;

            // Build socket data
            Map<String, Object> socketParam = new HashMap<>();
            socketParam.put("type", vcType);
            socketParam.put("sessionId", sessionId);

            Map<String, Object> data = new HashMap<>();
            data.put("host", "");
            data.put("icon", "");
            data.put("address", address);
            data.put("templateId", templateId);
            data.put("source", isMobile ? ZetrixConstants.TypeInfo.MOBILE_SOURCE : "");
            socketParam.put("data", data);

            JSONObject socketData = new JSONObject(socketParam);

            logger.info("Sending VerifyVC request");
            walletSocket.send("H5_" + ZetrixConstants.Operations.VERIFY_VC,
                    socketData, new MessageCallback() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        int code = response.optInt("code", -1);
                        if (code == 0) {
                            JSONObject responseData = response.optJSONObject("data");
                            if (responseData != null) {
                                String status = responseData.optString("status");
                                String details = responseData.optString("details");
                                logger.info("VerifyVC successful: " + status);
                                callback.onSuccess(status, details);
                            } else {
                                callback.onError("No data in response");
                            }
                        } else {
                            String errorMsg = response.optString("message", "VerifyVC failed");
                            logger.warning("VerifyVC rejected: " + errorMsg);
                            callback.onError(errorMsg);
                        }
                    } catch (Exception e) {
                        logger.severe("Error processing verifyVC response", e);
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }

                @Override
                public void onError(Exception error) {
                    logger.severe("VerifyVC request failed", error);
                    callback.onError(error.getMessage());
                }
            });

            // Launch wallet app via deep link if not using QR code
            if (!isQrcode) {
                Map<String, Object> urlObj = new HashMap<>();
                urlObj.put("linkTo", vcType);
                urlObj.put("type", vcType);
                urlObj.put("sessionId", sessionId);
                urlObj.put("host", appName);
                urlObj.put("icon", "");
                urlObj.put("address", address);
                urlObj.put("templateId", templateId);

                DeepLinkHelper.launchWalletApp(context, appType, testnet, urlObj);
            }

        } catch (Exception e) {
            logger.severe("Error creating verifyVC request", e);
            callback.onError("Error creating request: " + e.getMessage());
        }
    }

    /**
     * Request wallet to get a Verifiable Presentation (VP).
     * <p>
     * Verifiable Presentations are collections of verifiable credentials that can be
     * cryptographically verified. This method requests the wallet to create a VP
     * based on a template and specified attributes.
     * </p>
     * <p>
     * Follows Flutter SDK pattern from wallet_connect.dart lines 582-671.
     * </p>
     *
     * @param templateId the VP template ID
     * @param attributes list of attributes to include in the VP
     * @param callback   callback for VP result
     */
    public void getVP(String templateId, List<String> attributes, VPCallback callback) {
        if (!ensureAuthenticated(callback)) return;

        // Validate templateId parameter
        if (templateId == null || templateId.isEmpty()) {
            callback.onError("Required parameter missing: templateId");
            return;
        }

        // Validate attributes parameter
        if (attributes == null || attributes.isEmpty()) {
            callback.onError("Required parameter missing or invalid: attributes (must be a non-empty list)");
            return;
        }

        try {
            String sessionId = StorageUtils.getSessionId();
            String address = StorageUtils.getAddress();
            String appName = DeviceUtils.getAppName(context);
            boolean isMobile = DeviceUtils.isMobile();

            String vpType = "H5_" + ZetrixConstants.Operations.GET_VP;

            // Build socket data
            Map<String, Object> socketParam = new HashMap<>();
            socketParam.put("type", vpType);
            socketParam.put("sessionId", sessionId);

            Map<String, Object> data = new HashMap<>();
            data.put("host", "");
            data.put("icon", "");
            data.put("address", address);
            data.put("templateId", templateId);
            data.put("attributes", attributes);
            data.put("source", isMobile ? ZetrixConstants.TypeInfo.MOBILE_SOURCE : "");
            socketParam.put("data", data);

            JSONObject socketData = new JSONObject(socketParam);

            logger.info("Sending GetVP request");
            walletSocket.send("H5_" + ZetrixConstants.Operations.GET_VP,
                    socketData, new MessageCallback() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        int code = response.optInt("code", -1);
                        if (code == 0) {
                            JSONObject responseData = response.optJSONObject("data");
                            if (responseData != null) {
                                String uuid = responseData.optString("uuid");
                                logger.info("GetVP successful: " + uuid);
                                callback.onSuccess(uuid);
                            } else {
                                callback.onError("No data in response");
                            }
                        } else {
                            String errorMsg = response.optString("message", "GetVP failed");
                            logger.warning("GetVP rejected: " + errorMsg);
                            callback.onError(errorMsg);
                        }
                    } catch (Exception e) {
                        logger.severe("Error processing getVP response", e);
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }

                @Override
                public void onError(Exception error) {
                    logger.severe("GetVP request failed", error);
                    callback.onError(error.getMessage());
                }
            });

            // Launch wallet app via deep link if not using QR code
            if (!isQrcode) {
                Map<String, Object> urlObj = new HashMap<>();
                urlObj.put("linkTo", vpType);
                urlObj.put("type", vpType);
                urlObj.put("sessionId", sessionId);
                urlObj.put("host", appName);
                urlObj.put("icon", "");
                urlObj.put("address", address);
                urlObj.put("templateId", templateId);
                urlObj.put("attributes", attributes);

                DeepLinkHelper.launchWalletApp(context, appType, testnet, urlObj);
            }

        } catch (Exception e) {
            logger.severe("Error creating getVP request", e);
            callback.onError("Error creating request: " + e.getMessage());
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
     * Close the current QR code activity if it's showing.
     * <p>
     * This is called automatically when authentication succeeds.
     * Sends a broadcast to close any open QRCodeActivity instances.
     * </p>
     */
    private void closeQrActivity() {
        try {
            QRCodeActivity.closeAll(context);
            logger.info("QR code activity close broadcast sent");
        } catch (Exception e) {
            logger.warning("Error closing QR activity: " + e.getMessage());
        }
    }

    /**
     * Disconnect from wallet and clear session data.
     * <p>
     * This method clears stored authentication data (sessionId, address).
     * Use this when you want to fully disconnect and clear all session data.
     * </p>
     * <p>
     * If you only want to close the WebSocket connection without clearing data,
     * use {@link #closeConnect()} instead.
     * </p>
     */
    public void disconnect() {
        logger.info("Disconnecting from wallet and clearing storage");
        StorageUtils.disconnectRemoveStorage();
    }

    /**
     * Close the WebSocket connection without clearing session data.
     * <p>
     * This method only closes the WebSocket connection but preserves
     * stored authentication data (sessionId, address).
     * </p>
     * <p>
     * Use this when you want to temporarily close the connection but
     * keep the session data for reconnection later.
     * </p>
     * <p>
     * If you want to fully disconnect and clear all data, use {@link #disconnect()} instead.
     * </p>
     * <p>
     * Follows Flutter SDK pattern from wallet_connect.dart line 678-680.
     * </p>
     */
    public void closeConnect() {
        logger.info("Closing WebSocket connection");
        if (walletSocket != null) {
            walletSocket.disconnect();
        }
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
     * Authentication and signing callback.
     * <p>
     * Used for authAndSignMessage() which returns both auth and sign data.
     * </p>
     */
    public interface AuthAndSignCallback extends BaseCallback {
        void onSuccess(String sessionId, String address, String publicKey, String signData);
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

    /**
     * Verifiable Credential (VC) verification callback.
     * <p>
     * Used for verifyVC() which returns verification status and details.
     * </p>
     */
    public interface VCCallback extends BaseCallback {
        void onSuccess(String status, String details);
    }

    /**
     * Verifiable Presentation (VP) callback.
     * <p>
     * Used for getVP() which returns a VP UUID.
     * </p>
     */
    public interface VPCallback extends BaseCallback {
        void onSuccess(String uuid);
    }

    // ========== Result Classes for CompletableFuture API ==========

    /**
     * Result class for authentication operations.
     */
    public static class AuthResult {
        public final String address;
        public final String sessionId;

        public AuthResult(String address, String sessionId) {
            this.address = address;
            this.sessionId = sessionId;
        }
    }

    /**
     * Result class for signing operations.
     */
    public static class SignResult {
        public final String address;
        public final String publicKey;
        public final String signData;

        public SignResult(String address, String publicKey, String signData) {
            this.address = address;
            this.publicKey = publicKey;
            this.signData = signData;
        }
    }

    /**
     * Result class for authentication and signing operations.
     */
    public static class AuthAndSignResult {
        public final String sessionId;
        public final String address;
        public final String publicKey;
        public final String signData;

        public AuthAndSignResult(String sessionId, String address, String publicKey, String signData) {
            this.sessionId = sessionId;
            this.address = address;
            this.publicKey = publicKey;
            this.signData = signData;
        }
    }

    /**
     * Result class for VC verification operations.
     */
    public static class VCResult {
        public final String status;
        public final String details;

        public VCResult(String status, String details) {
            this.status = status;
            this.details = details;
        }
    }

    // ========== CompletableFuture API ==========

    /**
     * Connect to the WebSocket server (CompletableFuture version).
     * <p>
     * Returns a CompletableFuture that completes when the connection is established.
     * </p>
     *
     * @return CompletableFuture that completes with authInfo on success
     */
    public CompletableFuture<JSONObject> connectAsync() {
        CompletableFuture<JSONObject> future = new CompletableFuture<>();

        connect(new WebSocketCallback() {
            @Override
            public void onConnected(JSONObject authInfo) {
                future.complete(authInfo);
            }

            @Override
            public void onMessage(JSONObject message) {
                // No-op for this future
            }

            @Override
            public void onClosed(int code, String reason) {
                future.completeExceptionally(new Exception("Connection closed: " + reason));
            }

            @Override
            public void onError(Exception error) {
                future.completeExceptionally(error);
            }
        });

        return future;
    }

    /**
     * Request authentication from wallet (CompletableFuture version).
     *
     * @return CompletableFuture that completes with AuthResult on success
     */
    public CompletableFuture<AuthResult> authAsync() {
        CompletableFuture<AuthResult> future = new CompletableFuture<>();

        auth(new AuthCallback() {
            @Override
            public void onSuccess(String address, String sessionId) {
                future.complete(new AuthResult(address, sessionId));
            }

            @Override
            public void onError(String error) {
                future.completeExceptionally(new Exception(error));
            }
        });

        return future;
    }

    /**
     * Authenticate and sign message in a single operation (CompletableFuture version).
     *
     * @param message the message to sign
     * @return CompletableFuture that completes with AuthAndSignResult on success
     */
    public CompletableFuture<AuthAndSignResult> authAndSignMessageAsync(String message) {
        CompletableFuture<AuthAndSignResult> future = new CompletableFuture<>();

        authAndSignMessage(message, new AuthAndSignCallback() {
            @Override
            public void onSuccess(String sessionId, String address, String publicKey, String signData) {
                future.complete(new AuthAndSignResult(sessionId, address, publicKey, signData));
            }

            @Override
            public void onError(String error) {
                future.completeExceptionally(new Exception(error));
            }
        });

        return future;
    }

    /**
     * Request wallet to sign a message (CompletableFuture version).
     *
     * @param message the message to sign
     * @return CompletableFuture that completes with SignResult on success
     */
    public CompletableFuture<SignResult> signMessageAsync(String message) {
        CompletableFuture<SignResult> future = new CompletableFuture<>();

        signMessage(message, new SignCallback() {
            @Override
            public void onSuccess(String address, String publicKey, String signData) {
                future.complete(new SignResult(address, publicKey, signData));
            }

            @Override
            public void onError(String error) {
                future.completeExceptionally(new Exception(error));
            }
        });

        return future;
    }

    /**
     * Request wallet to sign binary data (CompletableFuture version).
     *
     * @param message the blob message to sign
     * @return CompletableFuture that completes with SignResult on success
     */
    public CompletableFuture<SignResult> signBlobAsync(String message) {
        CompletableFuture<SignResult> future = new CompletableFuture<>();

        signBlob(message, new SignCallback() {
            @Override
            public void onSuccess(String address, String publicKey, String signData) {
                future.complete(new SignResult(address, publicKey, signData));
            }

            @Override
            public void onError(String error) {
                future.completeExceptionally(new Exception(error));
            }
        });

        return future;
    }

    /**
     * Send a transaction through the wallet (CompletableFuture version).
     *
     * @param from     sender address
     * @param to       recipient address
     * @param amount   transaction amount
     * @param gasFee   gas fee
     * @param nonce    account nonce
     * @return CompletableFuture that completes with transaction hash on success
     */
    public CompletableFuture<String> sendTransactionAsync(String from, String to, String amount,
                                                           String gasFee, long nonce) {
        CompletableFuture<String> future = new CompletableFuture<>();

        sendTransaction(from, to, amount, gasFee, nonce, new TransactionCallback() {
            @Override
            public void onSuccess(String transactionHash) {
                future.complete(transactionHash);
            }

            @Override
            public void onError(String error) {
                future.completeExceptionally(new Exception(error));
            }
        });

        return future;
    }

    /**
     * Get account nonce from the blockchain (CompletableFuture version).
     *
     * @param address  the wallet address
     * @param chainId  the chain ID ("1" for mainnet, "2" for testnet)
     * @return CompletableFuture that completes with nonce value on success
     */
    public CompletableFuture<Long> getNonceAsync(String address, String chainId) {
        CompletableFuture<Long> future = new CompletableFuture<>();

        getNonce(address, chainId, new NonceCallback() {
            @Override
            public void onSuccess(long nonce) {
                future.complete(nonce);
            }

            @Override
            public void onError(String error) {
                future.completeExceptionally(new Exception(error));
            }
        });

        return future;
    }

    /**
     * Request wallet to verify a Verifiable Credential (CompletableFuture version).
     *
     * @param templateId the VC template ID to verify against
     * @return CompletableFuture that completes with VCResult on success
     */
    public CompletableFuture<VCResult> verifyVCAsync(String templateId) {
        CompletableFuture<VCResult> future = new CompletableFuture<>();

        verifyVC(templateId, new VCCallback() {
            @Override
            public void onSuccess(String status, String details) {
                future.complete(new VCResult(status, details));
            }

            @Override
            public void onError(String error) {
                future.completeExceptionally(new Exception(error));
            }
        });

        return future;
    }

    /**
     * Request wallet to get a Verifiable Presentation (CompletableFuture version).
     *
     * @param templateId the VP template ID
     * @param attributes list of attributes to include in the VP
     * @return CompletableFuture that completes with VP UUID on success
     */
    public CompletableFuture<String> getVPAsync(String templateId, List<String> attributes) {
        CompletableFuture<String> future = new CompletableFuture<>();

        getVP(templateId, attributes, new VPCallback() {
            @Override
            public void onSuccess(String uuid) {
                future.complete(uuid);
            }

            @Override
            public void onError(String error) {
                future.completeExceptionally(new Exception(error));
            }
        });

        return future;
    }

    // ========== Builder Pattern ==========

    /**
     * Builder for ZetrixConnectWallet.
     * <p>
     * The SDK internally launches a QRCodeActivity when needed to display QR codes.
     * </p>
     */
    public static class Builder {
        private final Context context;
        private String appType = "zetrix";
        private boolean isQrcode = false;
        private boolean testnet = false;
        private String bridgeUrl = null;

        /**
         * Create a new Builder.
         *
         * @param context the context
         * @throws IllegalArgumentException if context is null
         */
        public Builder(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }
            // Store application context internally
            this.context = context.getApplicationContext();
        }

        public Builder setAppType(String appType) {
            this.appType = appType;
            return this;
        }

        public Builder setQrcode(boolean isQrcode) {
            this.isQrcode = isQrcode;
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

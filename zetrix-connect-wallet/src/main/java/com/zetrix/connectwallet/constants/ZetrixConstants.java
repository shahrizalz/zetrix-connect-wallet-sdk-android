package com.zetrix.connectwallet.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Constants for Zetrix Connect Wallet SDK.
 * <p>
 * This class provides static constant values for use throughout the Android SDK.
 * Ported from Flutter SDK's zetrix_constants.dart.
 * </p>
 */
public final class ZetrixConstants {

    /**
     * WebSocket URLs for Zetrix Connect Wallet servers.
     */
    public static final class WebSocket {
        /** WebSocket URL for mainnet */
        public static final String WSS_MAINNET = "wss://wscw.zetrix.com";

        /** WebSocket URL for testnet */
        public static final String WSS_TESTNET = "wss://test-wscw.zetrix.com";

        private WebSocket() {
            throw new AssertionError("Cannot instantiate WebSocket constants class");
        }
    }

    /**
     * SDK operation types.
     */
    public static final class Operations {
        /** Bind operation - connect to wallet */
        public static final String BIND = "bind";

        /** Authentication operation */
        public static final String AUTH = "auth";

        /** Sign message operation */
        public static final String SIGN_MESSAGE = "signMessage";

        /** Sign blob (binary data) operation */
        public static final String SIGN_BLOB = "signBlob";

        /** Send transaction operation */
        public static final String SEND_TRANSACTION = "sendTransaction";

        /** Set QR code operation */
        public static final String SET_QR = "put";

        /** Combined bind and sign message operation */
        public static final String BIND_AND_SIGN_MESSAGE = "bindAndSignMessage";

        /** Verify verifiable credential operation */
        public static final String VERIFY_VC = "verifyVC";

        /** Get verifiable presentation operation */
        public static final String GET_VP = "getVP";

        private Operations() {
            throw new AssertionError("Cannot instantiate Operations constants class");
        }
    }

    /**
     * Result information with error codes and messages.
     */
    public static final class ResultInfo {
        /** Result code for WebSocket not supported */
        public static final int CODE_NO_USE_SOCKET = 90001;
        public static final String MSG_NO_USE_SOCKET = "Your device does not support the WebSocket communication protocol";

        /** Result code for local storage not supported */
        public static final int CODE_NO_USE_LOCAL_STORAGE = 90002;
        public static final String MSG_NO_USE_LOCAL_STORAGE = "Your device does not support local storage";

        /** Result code for account not activated */
        public static final int CODE_NONCE_NOT_ACTIVATED = 10010;
        public static final String MSG_NONCE_NOT_ACTIVATED = "Account not activated";

        /** Result code for not authorized */
        public static final int CODE_NONCE_NOT_AUTH = 10011;
        public static final String MSG_NONCE_NOT_AUTH = "Unexecuted authorization";

        /** Result code for operation cancelled */
        public static final int CODE_CLOSE_NOTICE = 1;
        public static final String MSG_CLOSE_NOTICE = "Cancelled";

        /** Result code for parameter format error */
        public static final int CODE_H5_PARAMETERS_MUST_OBJECT = 2000;
        public static final String MSG_H5_PARAMETERS_MUST_OBJECT = "Parameter format error, parameter format is object";

        /** Result code for missing required parameters */
        public static final int CODE_H5_REQUIRED_PARAMETERS_ERROR = 2001;
        public static final String MSG_H5_REQUIRED_PARAMETERS_ERROR = "Missing required parameters";

        private ResultInfo() {
            throw new AssertionError("Cannot instantiate ResultInfo constants class");
        }

        /**
         * Get result message for a given code.
         *
         * @param code the result code
         * @return the message corresponding to the code, or "Unknown error" if not found
         */
        public static String getMessageForCode(int code) {
            switch (code) {
                case CODE_NO_USE_SOCKET:
                    return MSG_NO_USE_SOCKET;
                case CODE_NO_USE_LOCAL_STORAGE:
                    return MSG_NO_USE_LOCAL_STORAGE;
                case CODE_NONCE_NOT_ACTIVATED:
                    return MSG_NONCE_NOT_ACTIVATED;
                case CODE_NONCE_NOT_AUTH:
                    return MSG_NONCE_NOT_AUTH;
                case CODE_CLOSE_NOTICE:
                    return MSG_CLOSE_NOTICE;
                case CODE_H5_PARAMETERS_MUST_OBJECT:
                    return MSG_H5_PARAMETERS_MUST_OBJECT;
                case CODE_H5_REQUIRED_PARAMETERS_ERROR:
                    return MSG_H5_REQUIRED_PARAMETERS_ERROR;
                default:
                    return "Unknown error";
            }
        }
    }

    /**
     * Type information constants.
     */
    public static final class TypeInfo {
        /** Storage name for session data */
        public static final String STORAGE_NAME = "zetrixWalletConnect";

        /** Mobile source identifier */
        public static final String MOBILE_SOURCE = "mobile";

        private TypeInfo() {
            throw new AssertionError("Cannot instantiate TypeInfo constants class");
        }
    }

    /**
     * Deep link host parameters for different wallet applications and platforms.
     * These are used to launch wallet apps via Android Intents.
     */
    public static final class HostParam {
        // Zetrix Wallet
        /** Zetrix Wallet Android deep link (mainnet) */
        public static final String ANDROID_ZETRIX = "zetrixnew://zetrix.com/app/flutter";

        /** Zetrix Wallet iOS deep link (mainnet) */
        public static final String IOS_ZETRIX = "zetrixnew://zetrix.com/app/flutter";

        /** Zetrix Wallet Android deep link (testnet) */
        public static final String ANDROID_ZETRIX_TESTNET = "zetrixnew-uat://zetrix.com/app/flutter";

        /** Zetrix Wallet iOS deep link (testnet) */
        public static final String IOS_ZETRIX_TESTNET = "zetrixnew-uat://zetrix.com/app/flutter";

        // PIXA
        /** PIXA Android deep link */
        public static final String ANDROID_PIXA = "pixa://pixa.com/app/flutter";

        /** PIXA iOS deep link */
        public static final String IOS_PIXA = "pixa://pixa.com/app/flutter";

        // MyID
        /** MyID Android deep link (mainnet) */
        public static final String ANDROID_MYID = "myid://myid.com/app/flutter";

        /** MyID iOS deep link (mainnet) */
        public static final String IOS_MYID = "myid://myid.com/app/flutter";

        /** MyID Android deep link (testnet) */
        public static final String ANDROID_MYID_TESTNET = "myid-uat://myid.com/app/flutter";

        /** MyID iOS deep link (testnet) */
        public static final String IOS_MYID_TESTNET = "myid-uat://myid.com/app/flutter";

        // MUMA
        /** MUMA Android deep link (mainnet) */
        public static final String ANDROID_MUMA = "muma://muma.com/app/flutter";

        /** MUMA iOS deep link (mainnet) */
        public static final String IOS_MUMA = "muma://muma.com/app/flutter";

        /** MUMA Android deep link (testnet) */
        public static final String ANDROID_MUMA_TESTNET = "muma-uat://muma.com/app/flutter";

        /** MUMA iOS deep link (testnet) */
        public static final String IOS_MUMA_TESTNET = "muma-uat://muma.com/app/flutter";

        private HostParam() {
            throw new AssertionError("Cannot instantiate HostParam constants class");
        }

        /**
         * Get the appropriate deep link for a wallet app on Android.
         *
         * @param walletApp the wallet app name (zetrix, pixa, myid, muma)
         * @param testnet   whether to use testnet URLs
         * @return the deep link URL for the wallet app
         */
        public static String getAndroidDeepLink(String walletApp, boolean testnet) {
            if (walletApp == null) {
                return ANDROID_ZETRIX;
            }

            switch (walletApp.toLowerCase()) {
                case "zetrix":
                    return testnet ? ANDROID_ZETRIX_TESTNET : ANDROID_ZETRIX;
                case "pixa":
                    return ANDROID_PIXA;
                case "myid":
                    return testnet ? ANDROID_MYID_TESTNET : ANDROID_MYID;
                case "muma":
                    return testnet ? ANDROID_MUMA_TESTNET : ANDROID_MUMA;
                default:
                    return ANDROID_ZETRIX;
            }
        }
    }

    /**
     * System error messages.
     */
    public static final class SystemError {
        /** Abnormal operation error message */
        public static final String ABNORMAL_OPERATION = "Abnormal operation";

        /** iOS support error message (not applicable for Android SDK) */
        public static final String IOS_SUPPORT_ERROR = "Sorry, iOS is not currently supported";

        private SystemError() {
            throw new AssertionError("Cannot instantiate SystemError constants class");
        }
    }

    /**
     * Wallet application display names.
     */
    public static final class AppName {
        /** Zetrix Wallet display name */
        public static final String ZETRIX = "Zetrix Wallet";

        /** PIXA display name */
        public static final String PIXA = "PIXA";

        /** MyID display name */
        public static final String MYID = "MyID";

        /** MUMA display name */
        public static final String MUMA = "MUMA";

        private AppName() {
            throw new AssertionError("Cannot instantiate AppName constants class");
        }

        /**
         * Get display name for a wallet app.
         *
         * @param walletApp the wallet app identifier
         * @return the display name
         */
        public static String getDisplayName(String walletApp) {
            if (walletApp == null) {
                return ZETRIX;
            }

            switch (walletApp.toLowerCase()) {
                case "zetrix":
                    return ZETRIX;
                case "pixa":
                    return PIXA;
                case "myid":
                    return MYID;
                case "muma":
                    return MUMA;
                default:
                    return ZETRIX;
            }
        }
    }

    /**
     * Default error code for general errors.
     */
    public static final int ERROR_CODE = -1;

    // Prevent instantiation
    private ZetrixConstants() {
        throw new AssertionError("Cannot instantiate ZetrixConstants class");
    }
}

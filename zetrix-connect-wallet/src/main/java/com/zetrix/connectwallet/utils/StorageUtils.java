package com.zetrix.connectwallet.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Utility class for secure storage operations.
 * <p>
 * Provides encrypted key-value storage for sensitive data like session IDs and wallet addresses.
 * Uses Android Keystore and EncryptedSharedPreferences to ensure data is encrypted at rest.
 * </p>
 * <p>
 * Ported from Flutter SDK's storage_utils.dart (replaces flutter_secure_storage).
 * </p>
 * <p>
 * Usage:
 * <pre>
 * // Initialize first (typically in Application onCreate or SDK init)
 * StorageUtils.initialize(context);
 *
 * // Store auth data
 * StorageUtils.setAuthData("session-123", "zetrix-address-xyz");
 *
 * // Retrieve session ID
 * String sessionId = StorageUtils.getSessionId();
 *
 * // Clear on disconnect
 * StorageUtils.disconnectRemoveStorage();
 * </pre>
 * </p>
 */
public final class StorageUtils {

    private static final String PREFERENCES_NAME = "zetrix_secure_storage";
    private static final String KEY_SESSION_ID = "sessionId";
    private static final String KEY_ADDRESS = "address";

    private static final ZetrixLogger logger = ZetrixLogger.getLogger("StorageUtils");

    private static SharedPreferences securePreferences;
    private static boolean initialized = false;
    private static final Object lock = new Object();

    // Prevent instantiation
    private StorageUtils() {
        throw new AssertionError("Cannot instantiate StorageUtils class");
    }

    /**
     * Initialize the secure storage system.
     * <p>
     * Must be called before using any storage methods. Typically called once
     * during application initialization or SDK setup.
     * </p>
     * <p>
     * This method creates an EncryptedSharedPreferences instance backed by
     * Android Keystore for secure encryption.
     * </p>
     *
     * @param context the application context (will use application context internally)
     * @throws RuntimeException if initialization fails due to security or I/O errors
     */
    public static void initialize(Context context) {
        if (initialized) {
            return;
        }

        synchronized (lock) {
            if (initialized) {
                return;
            }

            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }

            try {
                // Create or retrieve the Master Key for encryption
                MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                // Create EncryptedSharedPreferences
                securePreferences = EncryptedSharedPreferences.create(
                        context.getApplicationContext(),
                        PREFERENCES_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );

                initialized = true;
                logger.info("Secure storage initialized successfully");
            } catch (GeneralSecurityException | IOException e) {
                logger.severe("Failed to initialize secure storage", e);
                throw new RuntimeException("Failed to initialize secure storage", e);
            }
        }
    }

    /**
     * Ensure storage is initialized.
     * <p>
     * Internal helper method that throws if storage hasn't been initialized.
     * </p>
     *
     * @throws IllegalStateException if storage is not initialized
     */
    private static void ensureInitialized() {
        if (!initialized || securePreferences == null) {
            throw new IllegalStateException(
                    "StorageUtils not initialized. Call initialize(context) first.");
        }
    }

    /**
     * Get the stored session ID.
     *
     * @return the session ID, or null if not found
     */
    public static String getSessionId() {
        ensureInitialized();
        try {
            return securePreferences.getString(KEY_SESSION_ID, null);
        } catch (Exception e) {
            logger.warning("Error retrieving session ID", e);
            return null;
        }
    }

    /**
     * Get the stored wallet address.
     *
     * @return the wallet address, or null if not found
     */
    public static String getAddress() {
        ensureInitialized();
        try {
            return securePreferences.getString(KEY_ADDRESS, null);
        } catch (Exception e) {
            logger.warning("Error retrieving address", e);
            return null;
        }
    }

    /**
     * Store authentication data (session ID and wallet address).
     * <p>
     * This method stores both the session ID and wallet address atomically.
     * Used after successful wallet authentication.
     * </p>
     *
     * @param sessionId the session ID to store
     * @param address   the wallet address to store
     * @return true if data was stored successfully, false otherwise
     */
    public static boolean setAuthData(String sessionId, String address) {
        ensureInitialized();
        try {
            SharedPreferences.Editor editor = securePreferences.edit();
            editor.putString(KEY_SESSION_ID, sessionId);
            editor.putString(KEY_ADDRESS, address);
            boolean success = editor.commit();

            if (success) {
                logger.info("Auth data stored successfully");
            } else {
                logger.warning("Failed to store auth data");
            }

            return success;
        } catch (Exception e) {
            logger.severe("Error storing auth data", e);
            return false;
        }
    }

    /**
     * Remove stored authentication data on disconnect.
     * <p>
     * Clears both session ID and wallet address from secure storage.
     * Called when the user disconnects from the wallet.
     * </p>
     *
     * @return true if data was removed successfully, false otherwise
     */
    public static boolean disconnectRemoveStorage() {
        ensureInitialized();
        try {
            SharedPreferences.Editor editor = securePreferences.edit();
            editor.remove(KEY_SESSION_ID);
            editor.remove(KEY_ADDRESS);
            boolean success = editor.commit();

            if (success) {
                logger.info("Auth data removed successfully");
            } else {
                logger.warning("Failed to remove auth data");
            }

            return success;
        } catch (Exception e) {
            logger.severe("Error removing auth data", e);
            return false;
        }
    }

    /**
     * Store a generic key-value pair in secure storage.
     * <p>
     * Provides general-purpose encrypted storage for any string data.
     * </p>
     *
     * @param key   the key to store under
     * @param value the value to store
     * @return true if data was stored successfully, false otherwise
     */
    public static boolean setLocalStorage(String key, String value) {
        ensureInitialized();

        if (key == null || key.isEmpty()) {
            logger.warning("Cannot store with null or empty key");
            return false;
        }

        try {
            SharedPreferences.Editor editor = securePreferences.edit();
            editor.putString(key, value);
            boolean success = editor.commit();

            if (!success) {
                logger.warning("Failed to store value for key: " + key);
            }

            return success;
        } catch (Exception e) {
            logger.severe("Error storing key-value pair: " + key, e);
            return false;
        }
    }

    /**
     * Retrieve a generic value from secure storage.
     *
     * @param key the key to retrieve
     * @return the stored value, or null if not found
     */
    public static String getLocalStorage(String key) {
        ensureInitialized();

        if (key == null || key.isEmpty()) {
            logger.warning("Cannot retrieve with null or empty key");
            return null;
        }

        try {
            return securePreferences.getString(key, null);
        } catch (Exception e) {
            logger.warning("Error retrieving value for key: " + key, e);
            return null;
        }
    }

    /**
     * Remove a generic key-value pair from secure storage.
     *
     * @param key the key to remove
     * @return true if the key was removed successfully, false otherwise
     */
    public static boolean removeLocalStorage(String key) {
        ensureInitialized();

        if (key == null || key.isEmpty()) {
            logger.warning("Cannot remove with null or empty key");
            return false;
        }

        try {
            SharedPreferences.Editor editor = securePreferences.edit();
            editor.remove(key);
            return editor.commit();
        } catch (Exception e) {
            logger.severe("Error removing key: " + key, e);
            return false;
        }
    }

    /**
     * Clear all stored data from secure storage.
     * <p>
     * WARNING: This removes ALL encrypted data, not just session/address.
     * Use with caution.
     * </p>
     *
     * @return true if all data was cleared successfully, false otherwise
     */
    public static boolean clearAll() {
        ensureInitialized();
        try {
            boolean success = securePreferences.edit().clear().commit();

            if (success) {
                logger.info("All secure storage cleared");
            } else {
                logger.warning("Failed to clear secure storage");
            }

            return success;
        } catch (Exception e) {
            logger.severe("Error clearing secure storage", e);
            return false;
        }
    }

    /**
     * Check if a key exists in secure storage.
     *
     * @param key the key to check
     * @return true if the key exists, false otherwise
     */
    public static boolean contains(String key) {
        ensureInitialized();

        if (key == null || key.isEmpty()) {
            return false;
        }

        try {
            return securePreferences.contains(key);
        } catch (Exception e) {
            logger.warning("Error checking key existence: " + key, e);
            return false;
        }
    }

    /**
     * Check if storage is initialized.
     *
     * @return true if storage is ready to use, false otherwise
     */
    public static boolean isInitialized() {
        return initialized && securePreferences != null;
    }
}

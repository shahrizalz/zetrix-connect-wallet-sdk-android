package com.zetrix.connectwallet.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Facade for cryptographic string operations.
 * <p>
 * Provides utility methods for cryptographic hashing operations.
 * Ported from Flutter SDK's crypto_utils.dart (originally from cryptoStr.js).
 * </p>
 */
public final class CryptoUtils {

    // Prevent instantiation
    private CryptoUtils() {
        throw new AssertionError("Cannot instantiate CryptoUtils class");
    }

    /**
     * Returns the SHA256 hash of the message as a hex string.
     * <p>
     * Computes the SHA-256 hash of the input message and returns it
     * as a lowercase hexadecimal string.
     * </p>
     *
     * @param message the message to hash (can be null or empty)
     * @return the SHA256 hash as a hex string, or empty string if input is null/empty
     * @throws RuntimeException if SHA-256 algorithm is not available
     */
    public static String hmacStr(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        try {
            // Get SHA-256 MessageDigest instance
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Compute hash
            byte[] hashBytes = digest.digest(message.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Converts byte array to hexadecimal string.
     * <p>
     * Helper method to convert hash bytes to lowercase hex representation.
     * </p>
     *
     * @param bytes the byte array to convert
     * @return lowercase hexadecimal string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

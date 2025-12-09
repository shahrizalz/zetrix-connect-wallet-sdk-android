package com.zetrix.connectwallet.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * QRCodeGenerator utility for generating QR codes containing connection data.
 * <p>
 * Generates QR codes that wallet apps can scan to establish connection
 * with the dApp via the WebSocket bridge server.
 * </p>
 * <p>
 * QR code data format from server: "{rms}&{sessionId}&{type}"
 * </p>
 */
public class QRCodeGenerator {

    private static final ZetrixLogger logger = ZetrixLogger.getLogger("QRCodeGenerator");
    private static final int DEFAULT_QR_SIZE = 512; // 512x512 pixels

    /**
     * Generate QR code bitmap from QR data string.
     * <p>
     * This is the primary method for generating QR codes.
     * Data format: "{rms}&{sessionId}&{type}"
     * </p>
     *
     * @param qrData the QR data string from server
     * @return Bitmap containing the QR code, or null on error
     */
    public static Bitmap generateQRCode(String qrData) {
        return generateQRCode(qrData, DEFAULT_QR_SIZE);
    }

    /**
     * Generate QR code bitmap from QR data string with custom size.
     *
     * @param qrData the QR data string from server
     * @param size   QR code size in pixels (width and height)
     * @return Bitmap containing the QR code, or null on error
     */
    public static Bitmap generateQRCode(String qrData, int size) {
        try {
            logger.info("Generating QR code with data: " + qrData);

            // Configure QR code writer
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1); // Minimal margin

            // Generate QR code matrix
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE, size, size, hints);

            // Convert to bitmap
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            logger.info("QR code generated successfully");
            return bitmap;

        } catch (WriterException e) {
            logger.severe("Error encoding QR code", e);
            return null;
        }
    }

    /**
     * Generate QR code bitmap from connection data (legacy method).
     * <p>
     * NOTE: This method is kept for backward compatibility but is not
     * the recommended way to generate QR codes. Use generateQRCode(String qrData)
     * with the server-provided data instead.
     * </p>
     *
     * @param sessionId the session ID
     * @param bridgeUrl the WebSocket bridge URL
     * @param appType   the app type (e.g., "zetrix")
     * @return Bitmap containing the QR code, or null on error
     */
    @Deprecated
    public static Bitmap generateQRCode(String sessionId, String bridgeUrl, String appType) {
        return generateQRCode(sessionId, bridgeUrl, appType, DEFAULT_QR_SIZE);
    }

    /**
     * Generate QR code bitmap from connection data with custom size.
     *
     * @param sessionId the session ID
     * @param bridgeUrl the WebSocket bridge URL
     * @param appType   the app type (e.g., "zetrix")
     * @param size      QR code size in pixels (width and height)
     * @return Bitmap containing the QR code, or null on error
     */
    public static Bitmap generateQRCode(String sessionId, String bridgeUrl, String appType, int size) {
        try {
            // Create JSON payload
            JSONObject qrData = new JSONObject();
            qrData.put("sessionId", sessionId);
            qrData.put("bridgeUrl", bridgeUrl);
            qrData.put("appType", appType);

            String qrContent = qrData.toString();
            logger.info("Generating QR code with data: " + qrContent);

            // Configure QR code writer
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1); // Minimal margin

            // Generate QR code matrix
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, size, size, hints);

            // Convert to bitmap
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            logger.info("QR code generated successfully");
            return bitmap;

        } catch (JSONException e) {
            logger.severe("Error creating QR data JSON", e);
            return null;
        } catch (WriterException e) {
            logger.severe("Error encoding QR code", e);
            return null;
        }
    }

    /**
     * Generate QR code from connection data map.
     *
     * @param connectionData map containing sessionId, bridgeUrl, appType
     * @return Bitmap containing the QR code, or null on error
     */
    public static Bitmap generateQRCode(Map<String, String> connectionData) {
        return generateQRCode(connectionData, DEFAULT_QR_SIZE);
    }

    /**
     * Generate QR code from connection data map with custom size.
     *
     * @param connectionData map containing sessionId, bridgeUrl, appType
     * @param size           QR code size in pixels
     * @return Bitmap containing the QR code, or null on error
     */
    public static Bitmap generateQRCode(Map<String, String> connectionData, int size) {
        String sessionId = connectionData.get("sessionId");
        String bridgeUrl = connectionData.get("bridgeUrl");
        String appType = connectionData.get("appType");

        if (sessionId == null || bridgeUrl == null || appType == null) {
            logger.warning("Missing required connection data for QR code generation");
            return null;
        }

        return generateQRCode(sessionId, bridgeUrl, appType, size);
    }
}

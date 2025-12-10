package com.zetrix.connectwallet.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zetrix.connectwallet.utils.QRCodeGenerator;
import com.zetrix.connectwallet.utils.ZetrixLogger;

/**
 * QRCodeDialog helper for displaying QR codes in a dialog.
 * <p>
 * Shows a QR code that wallet apps can scan to establish connection.
 * Includes instructions and a close button.
 * </p>
 * <p>
 * QR code data format: "{rms}&{sessionId}&{type}"
 * </p>
 */
public class QRCodeDialog {

    private static final ZetrixLogger logger = ZetrixLogger.getLogger("QRCodeDialog");
    private static final int QR_SIZE_DP = 300;
    private static final int PADDING_DP = 24;

    /**
     * Show QR code dialog with QR data string.
     * <p>
     * QR data format from server: "{rms}&{sessionId}&{type}"
     * </p>
     *
     * @param context        the context (Activity or Fragment context)
     * @param qrData         the QR data string from server (rms&sessionId&type)
     * @param appType        the app type (e.g., "zetrix", "pixa")
     * @param dismissCallback optional callback when dialog is dismissed
     * @return AlertDialog instance, or null on error
     */
    public static AlertDialog show(Context context, String qrData, String appType, Runnable dismissCallback) {
        // Ensure dialog is shown on the UI thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            // We're on a background thread, post to main thread
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> showOnUiThread(context, qrData, appType, dismissCallback));
            return null; // Can't return dialog from background thread
        } else {
            // We're already on the main thread
            return showOnUiThread(context, qrData, appType, dismissCallback);
        }
    }

    /**
     * Internal method to show dialog on UI thread.
     * This method must be called from the main/UI thread.
     */
    private static AlertDialog showOnUiThread(Context context, String qrData, String appType, Runnable dismissCallback) {
        try {
            // Generate QR code from data string
            Bitmap qrBitmap = QRCodeGenerator.generateQRCode(qrData);
            if (qrBitmap == null) {
                logger.severe("Failed to generate QR code");
                return null;
            }

            // Create layout
            LinearLayout layout = createDialogLayout(context, qrBitmap, appType);

            // Build dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Scan QR Code");
            builder.setView(layout);
            builder.setPositiveButton("Close", (dialog, which) -> {
                dialog.dismiss();
                if (dismissCallback != null) {
                    dismissCallback.run();
                }
            });
            builder.setCancelable(true);
            builder.setOnCancelListener(dialog -> {
                if (dismissCallback != null) {
                    dismissCallback.run();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();

            logger.info("QR code dialog displayed");
            return dialog;

        } catch (Exception e) {
            logger.severe("Error showing QR code dialog", e);
            return null;
        }
    }

    /**
     * Show QR code dialog without dismiss callback.
     *
     * @param context the context
     * @param qrData  the QR data string from server (rms&sessionId&type)
     * @param appType the app type
     * @return AlertDialog instance, or null on error
     */
    public static AlertDialog show(Context context, String qrData, String appType) {
        return show(context, qrData, appType, null);
    }

    /**
     * Create the dialog layout with QR code and instructions.
     *
     * @param context  the context
     * @param qrBitmap the QR code bitmap
     * @param appType  the app type for display text
     * @return LinearLayout containing the dialog content
     */
    private static LinearLayout createDialogLayout(Context context, Bitmap qrBitmap, String appType) {
        float density = context.getResources().getDisplayMetrics().density;
        int qrSizePx = (int) (QR_SIZE_DP * density);
        int paddingPx = (int) (PADDING_DP * density);

        // Main layout
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        layout.setGravity(Gravity.CENTER);

        // Get wallet app name
        String walletName = getWalletAppName(appType);

        // Instructions text
        TextView instructions = new TextView(context);
        instructions.setText("Open your " + walletName + " wallet app and scan this QR code to connect");
        instructions.setTextSize(14);
        instructions.setGravity(Gravity.CENTER);
        instructions.setPadding(0, 0, 0, paddingPx);
        layout.addView(instructions);

        // QR code image
        ImageView qrImageView = new ImageView(context);
        qrImageView.setImageBitmap(qrBitmap);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(qrSizePx, qrSizePx);
        imageParams.gravity = Gravity.CENTER;
        qrImageView.setLayoutParams(imageParams);
        layout.addView(qrImageView);

        // Additional info text
        TextView info = new TextView(context);
        info.setText("Waiting for wallet app to scan...");
        info.setTextSize(12);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, paddingPx, 0, 0);
        info.setAlpha(0.7f);
        layout.addView(info);

        return layout;
    }

    /**
     * Get wallet app display name from app type.
     *
     * @param appType the app type
     * @return display name
     */
    private static String getWalletAppName(String appType) {
        if (appType == null) {
            return "Zetrix";
        }
        switch (appType.toLowerCase()) {
            case "pixa":
                return "PIXA";
            case "myid":
                return "MyID";
            case "muma":
                return "MUMA";
            case "zetrix":
            default:
                return "Zetrix";
        }
    }
}

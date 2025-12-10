package com.zetrix.connectwallet.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zetrix.connectwallet.utils.QRCodeGenerator;
import com.zetrix.connectwallet.utils.ZetrixLogger;

/**
 * QRCodeActivity for displaying QR codes in a standalone activity.
 * <p>
 * This activity is launched by the SDK internally to show QR codes.
 * Developers don't need to manage this activity - it's handled automatically.
 * </p>
 * <p>
 * The activity listens for a broadcast to close itself when authentication succeeds.
 * </p>
 */
public class QRCodeActivity extends Activity {

    private static final ZetrixLogger logger = ZetrixLogger.getLogger("QRCodeActivity");
    private static final int QR_SIZE_DP = 300;
    private static final int PADDING_DP = 24;

    public static final String EXTRA_QR_DATA = "qr_data";
    public static final String EXTRA_APP_TYPE = "app_type";
    public static final String ACTION_CLOSE_QR = "com.zetrix.connectwallet.CLOSE_QR";

    private BroadcastReceiver closeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get QR data from intent
        String qrData = getIntent().getStringExtra(EXTRA_QR_DATA);
        String appType = getIntent().getStringExtra(EXTRA_APP_TYPE);

        if (qrData == null || qrData.isEmpty()) {
            logger.severe("QRCodeActivity started without QR data");
            finish();
            return;
        }

        // Generate QR code
        Bitmap qrBitmap = QRCodeGenerator.generateQRCode(qrData);
        if (qrBitmap == null) {
            logger.severe("Failed to generate QR code");
            finish();
            return;
        }

        // Create and set layout
        View layout = createLayout(qrBitmap, appType);
        setContentView(layout);

        // Register broadcast receiver to close this activity
        closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                logger.info("Received close broadcast, finishing activity");
                finish();
            }
        };

        IntentFilter filter = new IntentFilter(ACTION_CLOSE_QR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(closeReceiver, filter);
        }

        logger.info("QRCodeActivity created and displayed");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (closeReceiver != null) {
            try {
                unregisterReceiver(closeReceiver);
            } catch (Exception e) {
                logger.warning("Error unregistering receiver: " + e.getMessage());
            }
        }
    }

    /**
     * Create the activity layout with QR code and instructions.
     */
    private View createLayout(Bitmap qrBitmap, String appType) {
        float density = getResources().getDisplayMetrics().density;
        int qrSizePx = (int) (QR_SIZE_DP * density);
        int paddingPx = (int) (PADDING_DP * density);

        // Main layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        layout.setGravity(Gravity.CENTER);
        layout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Title
        TextView title = new TextView(this);
        title.setText("Scan QR Code");
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, paddingPx);
        layout.addView(title);

        // Get wallet app name
        String walletName = getWalletAppName(appType);

        // Instructions text
        TextView instructions = new TextView(this);
        instructions.setText("Open your " + walletName + " wallet app and scan this QR code to connect");
        instructions.setTextSize(14);
        instructions.setGravity(Gravity.CENTER);
        instructions.setPadding(0, 0, 0, paddingPx);
        layout.addView(instructions);

        // QR code image
        ImageView qrImageView = new ImageView(this);
        qrImageView.setImageBitmap(qrBitmap);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(qrSizePx, qrSizePx);
        imageParams.gravity = Gravity.CENTER;
        qrImageView.setLayoutParams(imageParams);
        layout.addView(qrImageView);

        // Additional info text
        TextView info = new TextView(this);
        info.setText("Waiting for wallet app to scan...");
        info.setTextSize(12);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, paddingPx, 0, paddingPx);
        info.setAlpha(0.7f);
        layout.addView(info);

        // Close button
        Button closeButton = new Button(this);
        closeButton.setText("Close");
        closeButton.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        buttonParams.gravity = Gravity.CENTER;
        closeButton.setLayoutParams(buttonParams);
        layout.addView(closeButton);

        return layout;
    }

    /**
     * Get wallet app display name from app type.
     */
    private String getWalletAppName(String appType) {
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

    /**
     * Helper method to launch QRCodeActivity from anywhere with Application context.
     */
    public static void launch(Context context, String qrData, String appType) {
        Intent intent = new Intent(context, QRCodeActivity.class);
        intent.putExtra(EXTRA_QR_DATA, qrData);
        intent.putExtra(EXTRA_APP_TYPE, appType);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        logger.getLogger("QRCodeActivity").info("Launching QRCodeActivity");
    }

    /**
     * Helper method to close all QRCodeActivity instances via broadcast.
     */
    public static void closeAll(Context context) {
        Intent intent = new Intent(ACTION_CLOSE_QR);
        context.sendBroadcast(intent);
        logger.getLogger("QRCodeActivity").info("Sent broadcast to close QRCodeActivity");
    }
}

package com.zetrix.connectwallet.helpers;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.zetrix.connectwallet.constants.ZetrixConstants;
import com.zetrix.connectwallet.utils.ZetrixLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DeepLinkHelper for launching wallet apps via deep links.
 * <p>
 * Constructs deep link URIs with connection data and launches
 * the appropriate wallet app to establish connection.
 * </p>
 * <p>
 * Supports multiple wallet apps: Zetrix Wallet, PIXA, MyID, MUMA.
 * Falls back to app store if wallet app is not installed.
 * </p>
 * <p>
 * Ported from Flutter SDK's link_to.dart.
 * </p>
 */
public class DeepLinkHelper {

    private static final ZetrixLogger logger = ZetrixLogger.getLogger("DeepLinkHelper");

    // Play Store package names
    private static final String PACKAGE_ZETRIX = "com.zetrix.wallet";
    private static final String PACKAGE_PIXA = "com.pixa.wallet";
    private static final String PACKAGE_MYID = "com.myid.wallet";
    private static final String PACKAGE_MUMA = "com.muma.wallet";

    /**
     * Launch wallet app via deep link with connection parameters.
     * <p>
     * Follows Flutter SDK pattern: builds URL as {host}?{query_params}
     * </p>
     *
     * @param context   the context (Activity context)
     * @param appType   the app type (zetrix, pixa, myid, muma)
     * @param testnet   whether to use testnet URLs
     * @param params    the connection parameters as a map
     * @return true if launch successful, false otherwise
     */
    public static boolean launchWalletApp(Context context, String appType, boolean testnet, Map<String, Object> params) {
        try {
            // Get host URL from constants
            String host = ZetrixConstants.HostParam.getAndroidDeepLink(appType, testnet);
            if (host == null || host.isEmpty()) {
                logger.severe("No deep link host found for app type: " + appType);
                return false;
            }

            // Build deep link URL: host?query_params
            String deepLink = buildDeepLinkUrl(host, params);
            logger.info("Launching wallet app with deep link: " + deepLink);

            // Create intent
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(deepLink));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Try to launch
            context.startActivity(intent);
            logger.info("Wallet app launched successfully");
            return true;

        } catch (ActivityNotFoundException e) {
            logger.warning("Wallet app not installed: " + appType);
            // Fallback to app store
            return openAppStore(context, appType);

        } catch (Exception e) {
            logger.severe("Error launching wallet app", e);
            return false;
        }
    }

    /**
     * Launch wallet app with sessionId and bridgeUrl (legacy method).
     * <p>
     * NOTE: This method defaults to mainnet. For testnet support,
     * use launchWalletApp(context, appType, testnet, params) instead.
     * </p>
     *
     * @param context   the context
     * @param sessionId the session ID
     * @param bridgeUrl the WebSocket bridge URL
     * @param appType   the app type
     * @return true if launch successful, false otherwise
     * @deprecated Use {@link #launchWalletApp(Context, String, boolean, Map)} instead
     */
    @Deprecated
    public static boolean launchWalletApp(Context context, String sessionId, String bridgeUrl, String appType) {
        // Build params map
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("sessionId", sessionId);
        params.put("bridgeUrl", bridgeUrl);

        // Default to mainnet - caller should use the proper method with testnet flag
        return launchWalletApp(context, appType, false, params);
    }

    /**
     * Build deep link URL from host and parameters.
     * <p>
     * Follows Flutter SDK pattern: {host}?{query_params}
     * where query_params are key=value pairs joined with &
     * </p>
     *
     * @param host   the host URL (deep link prefix)
     * @param params the parameters map
     * @return complete deep link URL
     */
    private static String buildDeepLinkUrl(String host, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return host;
        }

        // Convert params to query string
        String queryString = convertMapToQueryString(params);

        // Build final URL
        return host + "?" + queryString;
    }

    /**
     * Convert parameter map to query string.
     * <p>
     * Follows Flutter SDK convertObj() method pattern.
     * </p>
     *
     * @param params the parameters map
     * @return query string (key=value&key=value...)
     */
    private static String convertMapToQueryString(Map<String, Object> params) {
        List<String> parts = new ArrayList<>();

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof List) {
                // Handle array values (add multiple key=value pairs)
                List<?> list = (List<?>) value;
                for (Object item : list) {
                    parts.add(key + "=" + item.toString());
                }
            } else {
                // Handle single values
                parts.add(key + "=" + value.toString());
            }
        }

        return String.join("&", parts);
    }

    /**
     * Get package name for app type.
     *
     * @param appType the app type
     * @return package name
     */
    private static String getPackageForAppType(String appType) {
        if (appType == null) {
            return PACKAGE_ZETRIX; // default
        }

        switch (appType.toLowerCase()) {
            case "pixa":
                return PACKAGE_PIXA;
            case "myid":
                return PACKAGE_MYID;
            case "muma":
                return PACKAGE_MUMA;
            case "zetrix":
            default:
                return PACKAGE_ZETRIX;
        }
    }

    /**
     * Open app store to install wallet app.
     *
     * @param context the context
     * @param appType the app type
     * @return true if store opened successfully, false otherwise
     */
    private static boolean openAppStore(Context context, String appType) {
        try {
            String packageName = getPackageForAppType(appType);
            logger.info("Opening app store for package: " + packageName);

            // Try Google Play Store
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            return true;

        } catch (ActivityNotFoundException e) {
            logger.warning("Play Store not available, trying browser");

            try {
                // Fallback to browser
                String packageName = getPackageForAppType(appType);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);

                return true;

            } catch (Exception ex) {
                logger.severe("Error opening app store", ex);
                return false;
            }
        }
    }

    /**
     * Check if wallet app is installed.
     *
     * @param context the context
     * @param appType the app type
     * @return true if installed, false otherwise
     */
    public static boolean isWalletAppInstalled(Context context, String appType) {
        try {
            String packageName = getPackageForAppType(appType);
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

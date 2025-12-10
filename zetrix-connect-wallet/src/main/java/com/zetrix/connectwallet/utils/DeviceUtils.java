package com.zetrix.connectwallet.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for device detection and information operations.
 * <p>
 * Provides methods to retrieve device information, app metadata,
 * and platform details for Android devices.
 * </p>
 * <p>
 * Ported from Flutter SDK's device_utils.dart.
 * </p>
 */
public final class DeviceUtils {

    private static final ZetrixLogger logger = ZetrixLogger.getLogger("DeviceUtils");

    // Prevent instantiation
    private DeviceUtils() {
        throw new AssertionError("Cannot instantiate DeviceUtils class");
    }

    /**
     * Check if the current platform is mobile.
     * <p>
     * For Android SDK, this always returns true since it only runs on Android devices.
     * </p>
     *
     * @return always true for Android
     */
    public static boolean isMobile() {
        // On Android, we're always on a mobile platform
        return true;
    }

    /**
     * Get device information including model, platform, and OS version.
     * <p>
     * Returns a map containing:
     * - model: Device model name (e.g., "Pixel 6")
     * - platform: Always "Android" for this SDK
     * - version: Android OS version (e.g., "13")
     * </p>
     *
     * @return map of device information
     */
    public static Map<String, String> getDeviceInfo() {
        Map<String, String> info = new HashMap<>();

        try {
            // Get device model
            info.put("model", Build.MODEL);

            // Platform is always Android
            info.put("platform", "Android");

            // Get Android version
            info.put("version", Build.VERSION.RELEASE);

            // Additional useful info
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("brand", Build.BRAND);
            info.put("sdkInt", String.valueOf(Build.VERSION.SDK_INT));
        } catch (Exception e) {
            logger.warning("Error getting device info: " + e.getMessage());
            info.put("model", "Unknown");
            info.put("platform", "Android");
            info.put("version", "Unknown");
        }

        return info;
    }

    /**
     * Get application name (user-visible app name).
     * <p>
     * Retrieves the application label from the AndroidManifest.xml.
     * This is the name displayed to users in the app launcher.
     * </p>
     *
     * @param context the application context
     * @return the app name, or "Unknown App" if retrieval fails
     */
    public static String getAppName(Context context) {
        if (context == null) {
            logger.warning("Context is null, cannot get app name");
            return "Unknown App";
        }

        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = context.getApplicationInfo();
            return packageManager.getApplicationLabel(applicationInfo).toString();
        } catch (Exception e) {
            logger.warning("Error getting app name: " + e.getMessage());
            return "Unknown App";
        }
    }

    /**
     * Get application package ID (package name).
     * <p>
     * Returns the unique package identifier (e.g., "com.example.app").
     * This is the applicationId defined in build.gradle.
     * </p>
     *
     * @param context the application context
     * @return the package name, or "unknown.package" if retrieval fails
     */
    public static String getAppPackageId(Context context) {
        if (context == null) {
            logger.warning("Context is null, cannot get package ID");
            return "unknown.package";
        }

        try {
            return context.getPackageName();
        } catch (Exception e) {
            logger.warning("Error getting app package ID: " + e.getMessage());
            return "unknown.package";
        }
    }

    /**
     * Get complete application information.
     * <p>
     * Returns a map containing:
     * - appName: User-visible application name
     * - packageName: Package identifier
     * - version: Version name (e.g., "1.0.0")
     * - buildNumber: Version code (e.g., "1")
     * </p>
     *
     * @param context the application context
     * @return map of application information
     */
    public static Map<String, String> getAppInfo(Context context) {
        Map<String, String> info = new HashMap<>();

        if (context == null) {
            logger.warning("Context is null, cannot get app info");
            info.put("appName", "Unknown");
            info.put("packageName", "unknown.package");
            info.put("version", "Unknown");
            info.put("buildNumber", "Unknown");
            return info;
        }

        try {
            PackageManager packageManager = context.getPackageManager();
            String packageName = context.getPackageName();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            ApplicationInfo applicationInfo = context.getApplicationInfo();

            // Get app name
            String appName = packageManager.getApplicationLabel(applicationInfo).toString();
            info.put("appName", appName);

            // Get package name
            info.put("packageName", packageName);

            // Get version name
            info.put("version", packageInfo.versionName != null ? packageInfo.versionName : "Unknown");

            // Get version code (build number)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.put("buildNumber", String.valueOf(packageInfo.getLongVersionCode()));
            } else {
                //noinspection deprecation
                info.put("buildNumber", String.valueOf(packageInfo.versionCode));
            }
        } catch (Exception e) {
            logger.warning("Error getting app info: " + e.getMessage());
            info.put("appName", "Unknown");
            info.put("packageName", "unknown.package");
            info.put("version", "Unknown");
            info.put("buildNumber", "Unknown");
        }

        return info;
    }

    /**
     * Get device platform name.
     * <p>
     * For Android SDK, this always returns "Android".
     * </p>
     *
     * @return always "Android" for this SDK
     */
    public static String getDevicePlatform() {
        return "Android";
    }

    /**
     * Get device model name.
     *
     * @return the device model (e.g., "Pixel 6")
     */
    public static String getDeviceModel() {
        return Build.MODEL;
    }

    /**
     * Get device manufacturer name.
     *
     * @return the device manufacturer (e.g., "Google")
     */
    public static String getManufacturer() {
        return Build.MANUFACTURER;
    }

    /**
     * Get Android OS version.
     *
     * @return the Android version (e.g., "13")
     */
    public static String getOsVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * Get Android SDK version (API level).
     *
     * @return the SDK version as an integer (e.g., 33 for Android 13)
     */
    public static int getSdkInt() {
        return Build.VERSION.SDK_INT;
    }
}

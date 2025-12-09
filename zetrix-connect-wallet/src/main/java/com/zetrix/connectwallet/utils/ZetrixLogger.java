package com.zetrix.connectwallet.utils;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized logging utility for Zetrix Connect Wallet SDK.
 * <p>
 * Provides structured logging with different severity levels using Android's Log system.
 * Supports per-class logger instances and global log level configuration.
 * </p>
 * <p>
 * Ported from Flutter SDK's logger.dart.
 * </p>
 * <p>
 * Usage:
 * <pre>
 * // Initialize logging (optional, done automatically on first use)
 * ZetrixLogger.initialize(ZetrixLogger.Level.DEBUG, true);
 *
 * // Get a logger for your class
 * ZetrixLogger logger = ZetrixLogger.getLogger("MyClass");
 * logger.info("Initialized successfully");
 * logger.warning("Connection timeout");
 * logger.error("Failed to authenticate", exception);
 *
 * // Or use static convenience methods
 * ZetrixLogger.info("MyClass", "Quick log message");
 * </pre>
 * </p>
 */
public class ZetrixLogger {

    /**
     * Logging levels (ordered by severity).
     */
    public enum Level {
        /** All logs enabled (most verbose) */
        ALL(0),
        /** Fine/debug level logs */
        FINE(1),
        /** Informational logs */
        INFO(2),
        /** Warning logs */
        WARNING(3),
        /** Severe/error logs */
        SEVERE(4),
        /** All logs disabled */
        OFF(5);

        private final int value;

        Level(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private static final String TAG_PREFIX = "Zetrix";
    private static final Map<String, ZetrixLogger> loggers = new HashMap<>();
    private static boolean initialized = false;
    private static Level currentLevel = Level.INFO;
    private static boolean loggingEnabled = true;

    private final String name;
    private final String tag;

    /**
     * Private constructor. Use {@link #getLogger(String)} to obtain instances.
     *
     * @param name the logger name (typically class name)
     */
    private ZetrixLogger(String name) {
        this.name = name;
        this.tag = TAG_PREFIX + ":" + name;
    }

    /**
     * Initialize the logging system.
     * <p>
     * Call this once at SDK initialization to configure logging behavior.
     * If not called explicitly, default settings will be used (Level.INFO, enabled).
     * </p>
     *
     * @param level          the minimum log level to output (default: Level.INFO)
     * @param enableLogging  whether to enable logging (default: true)
     */
    public static void initialize(Level level, boolean enableLogging) {
        currentLevel = level;
        loggingEnabled = enableLogging;
        initialized = true;
    }

    /**
     * Initialize with default settings (Level.INFO, enabled).
     */
    public static void initialize() {
        initialize(Level.INFO, true);
    }

    /**
     * Get or create a logger for a specific class.
     * <p>
     * Loggers are cached and reused for the same name.
     * </p>
     *
     * @param name the logger name (typically the class name)
     * @return a ZetrixLogger instance for the specified name
     */
    public static ZetrixLogger getLogger(String name) {
        if (!initialized) {
            initialize();
        }

        synchronized (loggers) {
            return loggers.computeIfAbsent(name, ZetrixLogger::new);
        }
    }

    /**
     * Set the global log level.
     * <p>
     * Only logs at or above this level will be output.
     * </p>
     *
     * @param level the minimum log level
     */
    public static void setLevel(Level level) {
        currentLevel = level;
    }

    /**
     * Enable or disable all logging.
     *
     * @param enabled true to enable logging, false to disable
     */
    public static void setEnabled(boolean enabled) {
        loggingEnabled = enabled;
    }

    /**
     * Check if logging is enabled for a specific level.
     *
     * @param level the level to check
     * @return true if the level should be logged
     */
    private boolean isLoggable(Level level) {
        if (!loggingEnabled || currentLevel == Level.OFF) {
            return false;
        }
        return level.getValue() >= currentLevel.getValue();
    }

    /**
     * Log a fine/debug level message.
     *
     * @param message the log message
     */
    public void fine(String message) {
        if (isLoggable(Level.FINE)) {
            Log.d(tag, message);
        }
    }

    /**
     * Log an informational message.
     *
     * @param message the log message
     */
    public void info(String message) {
        if (isLoggable(Level.INFO)) {
            Log.i(tag, message);
        }
    }

    /**
     * Log a warning message.
     *
     * @param message the log message
     */
    public void warning(String message) {
        if (isLoggable(Level.WARNING)) {
            Log.w(tag, message);
        }
    }

    /**
     * Log a warning message with an exception.
     *
     * @param message the log message
     * @param error   the exception/error
     */
    public void warning(String message, Throwable error) {
        if (isLoggable(Level.WARNING)) {
            Log.w(tag, message, error);
        }
    }

    /**
     * Log a severe/error message.
     *
     * @param message the log message
     */
    public void severe(String message) {
        if (isLoggable(Level.SEVERE)) {
            Log.e(tag, message);
        }
    }

    /**
     * Log a severe/error message with an exception.
     *
     * @param message the log message
     * @param error   the exception/error
     */
    public void severe(String message, Throwable error) {
        if (isLoggable(Level.SEVERE)) {
            Log.e(tag, message, error);
        }
    }

    // Static convenience methods

    /**
     * Convenience method for debug logs.
     *
     * @param className the class name
     * @param message   the log message
     */
    public static void debug(String className, String message) {
        getLogger(className).fine(message);
    }

    /**
     * Convenience method for info logs.
     *
     * @param className the class name
     * @param message   the log message
     */
    public static void info(String className, String message) {
        getLogger(className).info(message);
    }

    /**
     * Convenience method for warning logs.
     *
     * @param className the class name
     * @param message   the log message
     */
    public static void warning(String className, String message) {
        getLogger(className).warning(message);
    }

    /**
     * Convenience method for warning logs with exception.
     *
     * @param className the class name
     * @param message   the log message
     * @param error     the exception/error
     */
    public static void warning(String className, String message, Throwable error) {
        getLogger(className).warning(message, error);
    }

    /**
     * Convenience method for error logs.
     *
     * @param className the class name
     * @param message   the log message
     */
    public static void error(String className, String message) {
        getLogger(className).severe(message);
    }

    /**
     * Convenience method for error logs with exception.
     *
     * @param className the class name
     * @param message   the log message
     * @param error     the exception/error
     */
    public static void error(String className, String message, Throwable error) {
        getLogger(className).severe(message, error);
    }

    /**
     * Get the logger name.
     *
     * @return the logger name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the log tag used with Android Log.
     *
     * @return the log tag
     */
    public String getTag() {
        return tag;
    }
}

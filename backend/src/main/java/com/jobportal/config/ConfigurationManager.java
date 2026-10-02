package com.jobportal.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Singleton pattern: one shared, read-only view of application.properties.
 *
 * Uses the "initialization-on-demand holder" idiom: the JVM loads {@link Holder} lazily and
 * class initialization is guaranteed thread-safe, so no synchronized/volatile is needed.
 * (Exercise: compare this with double-checked locking and an enum singleton.)
 */
public final class ConfigurationManager {

    private static final String RESOURCE = "application.properties";

    private final Properties properties = new Properties();

    private ConfigurationManager() {
        try (InputStream in = ConfigurationManager.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load " + RESOURCE, e);
        }
    }

    private static final class Holder {
        private static final ConfigurationManager INSTANCE = new ConfigurationManager();
    }

    public static ConfigurationManager getInstance() {
        return Holder.INSTANCE;
    }

    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        return value == null ? defaultValue : Integer.parseInt(value.trim());
    }

    public long getLong(String key, long defaultValue) {
        String value = properties.getProperty(key);
        return value == null ? defaultValue : Long.parseLong(value.trim());
    }
}

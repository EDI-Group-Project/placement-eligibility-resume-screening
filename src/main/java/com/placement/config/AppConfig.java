package com.placement.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {
    private static final Properties PROPS = load();

    private AppConfig() {}

    private static Properties load() {
        Properties p = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null) {
                throw new IllegalStateException("application.properties not found on the classpath");
            }
            p.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load application.properties", e);
        }
        return p;
    }

    private static String envOrProperty(String envName, String propertyName) {
        String env = System.getenv(envName);
        return env != null && !env.isBlank() ? env : PROPS.getProperty(propertyName);
    }

    public static String dbUrl() {
        return envOrProperty("DB_URL", "db.url");
    }

    public static String dbUser() {
        return envOrProperty("DB_USER", "db.user");
    }

    public static String dbPassword() {
        return envOrProperty("DB_PASSWORD", "db.password");
    }

    public static String serverHost() {
        return envOrProperty("SERVER_HOST", "server.host");
    }

    public static int serverPort() {
        return Integer.parseInt(envOrProperty("SERVER_PORT", "server.port"));
    }

    public static int serverThreads() {
        return Integer.parseInt(envOrProperty("SERVER_THREADS", "server.threads"));
    }

    public static int clientTimeoutMs() {
        return Integer.parseInt(envOrProperty("CLIENT_TIMEOUT_MS", "server.client-timeout-ms"));
    }
}

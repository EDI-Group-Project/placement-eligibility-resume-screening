package com.placement.database;

import com.placement.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                AppConfig.dbUrl(),
                AppConfig.dbUser(),
                AppConfig.dbPassword()
        );
    }

    public static void verifyConnection() throws SQLException {
        try (Connection connection = getConnection()) {
            if (!connection.isValid(5)) {
                throw new SQLException("Database connection is not valid.");
            }
        }
    }
}

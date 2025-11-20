package org.veenix.timer.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String DB_URL = "jdbc:sqlite:timer.db";
    private static DatabaseConnection instance;
    private Connection connection;
    private final String dbUrl;
    private boolean initialized = false;

    private DatabaseConnection() {
        this(DB_URL);
    }

    public DatabaseConnection(String dbUrl) {
        this.dbUrl = dbUrl;
        // Ensure SQLite driver is registered (critical for native image)
        SqliteDriverInitializer.initialize();
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(dbUrl);
                if (!initialized) {
                    initializeDatabase();
                    initialized = true;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database connection", e);
        }
        return connection;
    }

    private void initializeDatabase() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(SqlQueries.CREATE_ACTIVITY_TABLE);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to close database connection", e);
        }
    }
}

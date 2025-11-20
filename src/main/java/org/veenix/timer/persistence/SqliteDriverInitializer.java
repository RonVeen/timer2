package org.veenix.timer.persistence;

import org.sqlite.JDBC;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Ensures SQLite JDBC driver is properly registered for native image builds.
 * This class must be initialized at runtime for GraalVM native image.
 */
public class SqliteDriverInitializer {

    private static volatile boolean initialized = false;

    /**
     * Call this method to ensure the SQLite driver is registered.
     * This is automatically invoked when DatabaseConnection is first used.
     */
    public static synchronized void initialize() {
        if (!initialized) {
            try {
                // Explicitly register the SQLite JDBC driver
                DriverManager.registerDriver(new JDBC());
                initialized = true;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to register SQLite JDBC driver", e);
            }
        }
    }
}

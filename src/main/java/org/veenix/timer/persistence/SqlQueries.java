package org.veenix.timer.persistence;

public final class SqlQueries {

    private SqlQueries() {
        // Utility class
    }

    // Table creation
    public static final String CREATE_ACTIVITY_TABLE = """
        CREATE TABLE IF NOT EXISTS activity (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            start_time TEXT NOT NULL,
            end_time TEXT,
            activity_type TEXT NOT NULL,
            status TEXT NOT NULL,
            description TEXT
        )
        """;

    // CRUD operations
    public static final String INSERT_ACTIVITY = """
        INSERT INTO activity (start_time, end_time, activity_type, status, description)
        VALUES (?, ?, ?, ?, ?)
        """;

    public static final String UPDATE_ACTIVITY = """
        UPDATE activity
        SET start_time = ?, end_time = ?, activity_type = ?, status = ?, description = ?
        WHERE id = ?
        """;

    public static final String DELETE_ACTIVITY = """
        DELETE FROM activity WHERE id = ?
        """;

    public static final String SELECT_ACTIVITY_BY_ID = """
        SELECT id, start_time, end_time, activity_type, status, description
        FROM activity
        WHERE id = ?
        """;

    public static final String SELECT_ALL_ACTIVITIES = """
        SELECT id, start_time, end_time, activity_type, status, description
        FROM activity
        ORDER BY start_time ASC
        """;

    public static final String SELECT_ACTIVITIES_BY_STATUS = """
        SELECT id, start_time, end_time, activity_type, status, description
        FROM activity
        WHERE status = ?
        ORDER BY start_time DESC
        """;

    public static final String SELECT_ACTIVITIES_BY_TYPE = """
        SELECT id, start_time, end_time, activity_type, status, description
        FROM activity
        WHERE activity_type = ?
        ORDER BY start_time DESC
        """;

    public static final String SELECT_ACTIVITIES_BY_START_TIME = """
        SELECT id, start_time, end_time, activity_type, status, description
        FROM activity
        WHERE DATE(start_time) = DATE(?)
        ORDER BY start_time ASC
        """;

    public static final String SELECT_ACTIVITIES_BY_DATE_RANGE = """
        SELECT id, start_time, end_time, activity_type, status, description
        FROM activity
        WHERE DATE(start_time) >= DATE(?) AND DATE(start_time) <= DATE(?)
        ORDER BY start_time ASC
        """;

    public static final String UPDATE_ACTIVITIES_STATUS_BY_STATUS = """
        UPDATE activity
        SET status = ?, end_time = ?
        WHERE status = ?
        """;
}

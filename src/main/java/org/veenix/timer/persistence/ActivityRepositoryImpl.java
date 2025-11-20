package org.veenix.timer.persistence;

import org.veenix.timer.model.Activity;
import org.veenix.timer.model.ActivityStatus;
import org.veenix.timer.model.ActivityType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ActivityRepositoryImpl implements ActivityRepository {

    private final DatabaseConnection dbConnection;

    public ActivityRepositoryImpl(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Activity save(Activity activity) {
        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(
                SqlQueries.INSERT_ACTIVITY, Statement.RETURN_GENERATED_KEYS)) {

            setActivityParameters(stmt, activity);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating activity failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    return Activity.builder()
                            .id(id)
                            .startTime(activity.startTime())
                            .endTime(activity.endTime())
                            .activityType(activity.activityType())
                            .status(activity.status())
                            .description(activity.description())
                            .build();
                } else {
                    throw new SQLException("Creating activity failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save activity", e);
        }
    }

    @Override
    public Activity update(Activity activity) {
        if (activity.id() == null) {
            throw new IllegalArgumentException("Activity ID cannot be null for update");
        }

        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(
                SqlQueries.UPDATE_ACTIVITY)) {

            setActivityParameters(stmt, activity);
            stmt.setLong(6, activity.id());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating activity failed, no rows affected.");
            }

            return activity;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update activity", e);
        }
    }

    @Override
    public void delete(Long id) {
        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(
                SqlQueries.DELETE_ACTIVITY)) {

            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete activity", e);
        }
    }

    @Override
    public Optional<Activity> findById(Long id) {
        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(
                SqlQueries.SELECT_ACTIVITY_BY_ID)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToActivity(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find activity by id", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Activity> findAll() {
        List<Activity> activities = new ArrayList<>();

        try (Statement stmt = dbConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(SqlQueries.SELECT_ALL_ACTIVITIES)) {

            while (rs.next()) {
                activities.add(mapResultSetToActivity(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all activities", e);
        }

        return activities;
    }

    @Override
    public List<Activity> findByStatus(ActivityStatus status) {
        List<Activity> activities = new ArrayList<>();

        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(
                SqlQueries.SELECT_ACTIVITIES_BY_STATUS)) {

            stmt.setString(1, status.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    activities.add(mapResultSetToActivity(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find activities by status", e);
        }

        return activities;
    }

    @Override
    public List<Activity> findByType(ActivityType type) {
        List<Activity> activities = new ArrayList<>();

        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(
                SqlQueries.SELECT_ACTIVITIES_BY_TYPE)) {

            stmt.setString(1, type.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    activities.add(mapResultSetToActivity(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find activities by type", e);
        }

        return activities;
    }

    @Override
    public List<Activity> findByStartTime(LocalDateTime startTime) {
        List<Activity> activities = new ArrayList<>();

        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(
                SqlQueries.SELECT_ACTIVITIES_BY_START_TIME)) {

            stmt.setString(1, startTime.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    activities.add(mapResultSetToActivity(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find activities by start time", e);
        }

        return activities;
    }

    @Override
    public List<Activity> findByDateRange(LocalDateTime from, LocalDateTime to) {
        List<Activity> activities = new ArrayList<>();

        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(
                SqlQueries.SELECT_ACTIVITIES_BY_DATE_RANGE)) {

            stmt.setString(1, from.toString());
            stmt.setString(2, to.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    activities.add(mapResultSetToActivity(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find activities by date range", e);
        }

        return activities;
    }

    @Override
    public void updateStatusByStatus(ActivityStatus currentStatus, ActivityStatus newStatus, LocalDateTime endTime) {
        try (PreparedStatement stmt = dbConnection.getConnection().prepareStatement(
                SqlQueries.UPDATE_ACTIVITIES_STATUS_BY_STATUS)) {

            stmt.setString(1, newStatus.name());
            stmt.setString(2, endTime != null ? endTime.toString() : null);
            stmt.setString(3, currentStatus.name());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update activities status", e);
        }
    }

    private void setActivityParameters(PreparedStatement stmt, Activity activity) throws SQLException {
        stmt.setString(1, activity.startTime() != null ? activity.startTime().toString() : null);
        stmt.setString(2, activity.endTime() != null ? activity.endTime().toString() : null);
        stmt.setString(3, activity.activityType() != null ? activity.activityType().name() : null);
        stmt.setString(4, activity.status() != null ? activity.status().name() : null);
        stmt.setString(5, activity.description());
    }

    private Activity mapResultSetToActivity(ResultSet rs) throws SQLException {
        return Activity.builder()
                .id(rs.getLong("id"))
                .startTime(rs.getString("start_time") != null ? LocalDateTime.parse(rs.getString("start_time")) : null)
                .endTime(rs.getString("end_time") != null ? LocalDateTime.parse(rs.getString("end_time")) : null)
                .activityType(rs.getString("activity_type") != null ? ActivityType.valueOf(rs.getString("activity_type")) : null)
                .status(rs.getString("status") != null ? ActivityStatus.valueOf(rs.getString("status")) : null)
                .description(rs.getString("description"))
                .build();
    }
}

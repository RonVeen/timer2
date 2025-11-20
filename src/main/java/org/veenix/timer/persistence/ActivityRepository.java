package org.veenix.timer.persistence;

import org.veenix.timer.model.Activity;
import org.veenix.timer.model.ActivityStatus;
import org.veenix.timer.model.ActivityType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ActivityRepository {

    Activity save(Activity activity);

    Activity update(Activity activity);

    void delete(Long id);

    Optional<Activity> findById(Long id);

    List<Activity> findAll();

    List<Activity> findByStatus(ActivityStatus status);

    List<Activity> findByType(ActivityType type);

    List<Activity> findByStartTime(LocalDateTime startTime);

    List<Activity> findByDateRange(LocalDateTime from, LocalDateTime to);

    void updateStatusByStatus(ActivityStatus currentStatus, ActivityStatus newStatus, LocalDateTime endTime);
}

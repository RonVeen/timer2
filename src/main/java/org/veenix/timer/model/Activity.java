package org.veenix.timer.model;

import java.time.LocalDateTime;

public record Activity(
    Long id,
    LocalDateTime startTime,
    LocalDateTime endTime,
    ActivityType activityType,
    ActivityStatus status,
    String description
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private ActivityType activityType;
        private ActivityStatus status;
        private String description;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder activityType(ActivityType activityType) {
            this.activityType = activityType;
            return this;
        }

        public Builder status(ActivityStatus status) {
            this.status = status;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Activity build() {
            return new Activity(id, startTime, endTime, activityType, status, description);
        }
    }
}

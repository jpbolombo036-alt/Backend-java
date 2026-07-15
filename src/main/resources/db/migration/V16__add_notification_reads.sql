CREATE TABLE user_notification_reads (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT uk_notification_read UNIQUE (notification_id, user_id),
    CONSTRAINT fk_notification_read_notification FOREIGN KEY (notification_id) REFERENCES system_notifications (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_read_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_notification_read_user ON user_notification_reads (user_id);

CREATE TABLE approval_step_checkpoint (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    step_name VARCHAR(64) NOT NULL,
    state_json JSON NOT NULL,
    completed_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_approval_step_checkpoint (application_id, step_name),
    KEY idx_approval_step_application (application_id, completed_at)
);

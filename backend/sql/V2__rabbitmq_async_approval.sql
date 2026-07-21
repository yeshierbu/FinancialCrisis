-- 已有 financial_crisis 数据库的增量升级脚本：RabbitMQ Outbox、消费幂等与申请锁。
USE financial_crisis;

CREATE TABLE IF NOT EXISTS approval_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    application_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    routing_key VARCHAR(128) NOT NULL,
    payload_json JSON NOT NULL,
    publish_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME DEFAULT NULL,
    published_at DATETIME DEFAULT NULL,
    last_error VARCHAR(1000) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_approval_outbox_event (event_id),
    KEY idx_approval_outbox_pending (publish_status, next_retry_at, created_at),
    KEY idx_approval_outbox_application (application_id)
);

CREATE TABLE IF NOT EXISTS approval_message_consume_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    application_id BIGINT NOT NULL,
    consume_status VARCHAR(16) NOT NULL,
    consumer_name VARCHAR(64) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    started_at DATETIME DEFAULT NULL,
    completed_at DATETIME DEFAULT NULL,
    last_error VARCHAR(1000) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_consume_event (event_id),
    KEY idx_consume_application (application_id),
    KEY idx_consume_status (consume_status, updated_at)
);

CREATE TABLE IF NOT EXISTS approval_execution_lock (
    application_id BIGINT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    locked_by VARCHAR(64) NOT NULL,
    locked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    KEY idx_approval_lock_expire (expires_at)
);

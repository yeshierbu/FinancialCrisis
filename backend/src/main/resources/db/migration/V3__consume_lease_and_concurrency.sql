ALTER TABLE approval_message_consume_log
    ADD COLUMN claim_token VARCHAR(64) NULL,
    ADD COLUMN lease_until DATETIME NULL,
    ADD COLUMN attempt_no INT NOT NULL DEFAULT 0,
    ADD KEY idx_consume_reclaim (consume_status, lease_until);

ALTER TABLE loan_application ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE loan_application ADD COLUMN owner_username VARCHAR(64) NULL;
UPDATE loan_application SET owner_username='user' WHERE owner_username IS NULL;
ALTER TABLE loan_application MODIFY owner_username VARCHAR(64) NOT NULL;
CREATE INDEX idx_loan_owner_created ON loan_application(owner_username, created_at);
ALTER TABLE approval_decision ADD UNIQUE KEY uk_approval_decision_application (application_id);
ALTER TABLE manual_review_ticket ADD UNIQUE KEY uk_manual_review_application (application_id);

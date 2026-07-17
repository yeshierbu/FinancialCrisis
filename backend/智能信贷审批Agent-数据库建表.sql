-- 智能信贷审批系统 MySQL 8 初始化脚本
-- 用于新库首次初始化；生产环境请使用 Flyway/Liquibase 管理版本，且不要保留演示密码。
CREATE DATABASE IF NOT EXISTS financial_crisis
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE financial_crisis;

CREATE TABLE IF NOT EXISTS system_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_code VARCHAR(32) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    role_description VARCHAR(255) DEFAULT NULL COMMENT '角色说明',
    role_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_system_role_code (role_code)
) COMMENT='系统角色表';

INSERT INTO system_role (role_code, role_name, role_description, role_status) VALUES
    ('ADMIN', '系统管理员', '管理人工复核、政策知识库和审计数据', 'ACTIVE'),
    ('USER', '贷款客户', '提交贷款申请、材料并查询本人申请', 'ACTIVE'),
    ('REVIEWER', '信贷复核员', '处理 Agent 转入的人工复核工单', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    role_description = VALUES(role_description),
    role_status = VALUES(role_status);

CREATE TABLE system_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '账号ID',
    username VARCHAR(64) NOT NULL COMMENT '登录账号',
    password_hash CHAR(64) NOT NULL COMMENT '密码SHA-256摘要，生产环境建议升级为BCrypt或Argon2',
    role_code VARCHAR(32) NOT NULL COMMENT '角色编码：ADMIN-管理员，USER-客户端用户',
    display_name VARCHAR(64) NOT NULL COMMENT '显示名称',
    account_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态：ACTIVE-启用，DISABLED-停用，LOCKED-锁定',
    last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_system_account_username (username),
    KEY idx_system_account_role_status (role_code, account_status)
) COMMENT='系统登录账号表';

INSERT INTO system_account (
    username,
    password_hash,
    role_code,
    display_name,
    account_status
) VALUES
    ('admin', SHA2('admin123', 256), 'ADMIN', '系统管理员', 'ACTIVE'),
    ('user', SHA2('user123', 256), 'USER', '贷款客户', 'ACTIVE'),
    ('reviewer', SHA2('reviewer123', 256), 'REVIEWER', '信贷复核员', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    role_code = VALUES(role_code),
    display_name = VALUES(display_name),
    account_status = VALUES(account_status);

-- 精确名单必须放在关系数据库中查询，不使用向量相似度判断身份证、手机号等是否命中。
CREATE TABLE risk_blacklist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '名单ID',
    subject_type VARCHAR(32) NOT NULL COMMENT 'ID_CARD/MOBILE/DEVICE/BANK_CARD',
    subject_hash CHAR(64) NOT NULL COMMENT '规范化值的SHA-256摘要，不保存敏感信息明文',
    risk_level VARCHAR(16) NOT NULL DEFAULT 'HIGH' COMMENT '风险等级',
    reason_code VARCHAR(64) NOT NULL COMMENT '原因码',
    source VARCHAR(64) NOT NULL COMMENT '名单来源',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    effective_from DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生效时间',
    effective_to DATETIME DEFAULT NULL COMMENT '失效时间',
    created_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_blacklist_subject (subject_type, subject_hash, source),
    KEY idx_blacklist_lookup (subject_type, subject_hash, status, effective_from, effective_to)
) COMMENT='精确风险黑名单';

-- MySQL 保存政策主数据和同步状态，政策分片向量本身存放在 Qdrant。
CREATE TABLE policy_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '政策主键',
    document_id VARCHAR(64) NOT NULL COMMENT '政策业务ID',
    title VARCHAR(255) NOT NULL COMMENT '政策标题',
    version VARCHAR(32) NOT NULL COMMENT '政策版本',
    product_code VARCHAR(64) NOT NULL COMMENT '适用产品',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/EXPIRED',
    effective_from DATE NOT NULL COMMENT '生效日期',
    effective_to DATE DEFAULT NULL COMMENT '失效日期',
    source_url VARCHAR(512) DEFAULT NULL COMMENT '原始文件地址',
    content_hash CHAR(64) DEFAULT NULL COMMENT '政策全文SHA-256',
    qdrant_collection VARCHAR(128) DEFAULT 'credit_policy_chunks' COMMENT 'Qdrant集合名',
    vector_sync_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SYNCED/FAILED',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_policy_version (document_id, version),
    KEY idx_policy_effective (product_code, status, effective_from, effective_to)
) COMMENT='政策文档主数据';

CREATE TABLE loan_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请单ID',
    application_no VARCHAR(64) NOT NULL UNIQUE COMMENT '申请单编号',
    product_code VARCHAR(64) NOT NULL COMMENT '贷款产品编码',
    applicant_name VARCHAR(64) NOT NULL COMMENT '申请人姓名',
    id_card_no VARCHAR(32) NOT NULL COMMENT '身份证号',
    mobile VARCHAR(20) NOT NULL COMMENT '手机号',
    loan_amount DECIMAL(18,2) NOT NULL COMMENT '申请金额',
    loan_term INT NOT NULL COMMENT '申请期数',
    employment_type VARCHAR(32) DEFAULT NULL COMMENT '就业类型',
    company_name VARCHAR(128) DEFAULT NULL COMMENT '工作单位',
    work_years INT DEFAULT NULL COMMENT '工作年限',
    status VARCHAR(32) NOT NULL COMMENT '审批状态',
    current_step VARCHAR(64) DEFAULT NULL COMMENT '当前处理步骤',
    channel_code VARCHAR(32) DEFAULT NULL COMMENT '申请渠道',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='贷款申请主表';

CREATE TABLE applicant_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    gender VARCHAR(16) DEFAULT NULL COMMENT '性别',
    birth_date DATE DEFAULT NULL COMMENT '出生日期',
    address VARCHAR(255) DEFAULT NULL COMMENT '住址',
    marital_status VARCHAR(16) DEFAULT NULL COMMENT '婚姻状态',
    education_level VARCHAR(32) DEFAULT NULL COMMENT '学历',
    occupation VARCHAR(64) DEFAULT NULL COMMENT '职业',
    employer_name VARCHAR(128) DEFAULT NULL COMMENT '雇主名称',
    annual_income DECIMAL(18,2) DEFAULT NULL COMMENT '年收入',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_application_id (application_id)
) COMMENT='申请人画像表';

CREATE TABLE uploaded_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    document_type VARCHAR(32) NOT NULL COMMENT '文件类型',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_url VARCHAR(512) NOT NULL COMMENT '文件存储地址',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小',
    file_hash VARCHAR(128) DEFAULT NULL COMMENT '文件HASH',
    ocr_status VARCHAR(32) DEFAULT 'PENDING' COMMENT 'OCR状态',
    parse_result_json JSON DEFAULT NULL COMMENT '解析结果',
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    KEY idx_application_id (application_id)
) COMMENT='上传材料表';

CREATE TABLE credit_assessment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    credit_score INT DEFAULT NULL COMMENT '征信评分',
    total_liability DECIMAL(18,2) DEFAULT NULL COMMENT '总负债',
    overdue_count INT DEFAULT 0 COMMENT '逾期次数',
    credit_card_utilization DECIMAL(10,4) DEFAULT NULL COMMENT '信用卡使用率',
    loan_account_count INT DEFAULT 0 COMMENT '贷款账户数',
    external_report_no VARCHAR(64) DEFAULT NULL COMMENT '外部征信报告编号',
    assessment_time DATETIME DEFAULT NULL COMMENT '评估时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_application_id (application_id)
) COMMENT='征信评估结果表';

CREATE TABLE fraud_risk_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    risk_level VARCHAR(16) NOT NULL COMMENT '风险等级',
    risk_score DECIMAL(10,2) DEFAULT NULL COMMENT '风险分',
    risk_tags_json JSON DEFAULT NULL COMMENT '风险标签列表',
    rule_hits_json JSON DEFAULT NULL COMMENT '命中规则列表',
    suggested_action VARCHAR(32) DEFAULT NULL COMMENT '建议动作',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_application_id (application_id)
) COMMENT='反欺诈风控结果表';

CREATE TABLE repayment_capacity_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    stable_monthly_income DECIMAL(18,2) DEFAULT NULL COMMENT '稳定月收入',
    monthly_debt_payment DECIMAL(18,2) DEFAULT NULL COMMENT '月债务支出',
    dti DECIMAL(10,4) DEFAULT NULL COMMENT '负债收入比',
    foir DECIMAL(10,4) DEFAULT NULL COMMENT '固定支出占收入比',
    disposable_income DECIMAL(18,2) DEFAULT NULL COMMENT '可支配收入',
    income_stability_score DECIMAL(10,2) DEFAULT NULL COMMENT '收入稳定性评分',
    max_affordable_emi DECIMAL(18,2) DEFAULT NULL COMMENT '最大可承受月供',
    recommended_credit_limit DECIMAL(18,2) DEFAULT NULL COMMENT '建议授信额度',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_application_id (application_id)
) COMMENT='偿债能力结果表';

CREATE TABLE approval_decision (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    decision_result VARCHAR(32) NOT NULL COMMENT '决策结果',
    approved_amount DECIMAL(18,2) DEFAULT NULL COMMENT '审批额度',
    interest_rate DECIMAL(10,4) DEFAULT NULL COMMENT '审批利率',
    loan_term INT DEFAULT NULL COMMENT '审批期数',
    reject_reason_code VARCHAR(64) DEFAULT NULL COMMENT '拒贷原因码',
    decision_explanation TEXT COMMENT '决策解释',
    policy_references_json JSON DEFAULT NULL COMMENT '条款引用',
    decided_by VARCHAR(64) DEFAULT NULL COMMENT '决策主体',
    decided_at DATETIME DEFAULT NULL COMMENT '决策时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_application_id (application_id)
) COMMENT='审批决策结果表';

CREATE TABLE manual_review_ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '工单ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    ticket_no VARCHAR(64) NOT NULL UNIQUE COMMENT '工单编号',
    review_status VARCHAR(32) NOT NULL COMMENT '复核状态',
    assigned_to VARCHAR(64) DEFAULT NULL COMMENT '分配审核人',
    trigger_reason VARCHAR(255) DEFAULT NULL COMMENT '触发原因',
    risk_summary TEXT COMMENT '风险摘要',
    review_comment TEXT COMMENT '复核意见',
    reviewed_at DATETIME DEFAULT NULL COMMENT '复核时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_application_id (application_id)
) COMMENT='人工复核工单表';

CREATE TABLE approval_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报告ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    report_type VARCHAR(32) NOT NULL COMMENT '报告类型',
    report_url VARCHAR(512) NOT NULL COMMENT '报告地址',
    report_version VARCHAR(32) DEFAULT NULL COMMENT '报告版本',
    generated_by VARCHAR(64) DEFAULT NULL COMMENT '生成主体',
    generated_at DATETIME DEFAULT NULL COMMENT '生成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_application_id (application_id)
) COMMENT='审批报告表';

CREATE TABLE agent_task_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    agent_name VARCHAR(64) NOT NULL COMMENT 'Agent名称',
    task_name VARCHAR(128) DEFAULT NULL COMMENT '任务名称',
    input_summary TEXT COMMENT '输入摘要',
    output_summary TEXT COMMENT '输出摘要',
    status VARCHAR(32) NOT NULL COMMENT '执行状态',
    duration_ms BIGINT DEFAULT NULL COMMENT '耗时毫秒',
    error_message TEXT COMMENT '错误信息',
    started_at DATETIME DEFAULT NULL COMMENT '开始时间',
    finished_at DATETIME DEFAULT NULL COMMENT '结束时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_application_id (application_id)
) COMMENT='Agent任务日志表';

CREATE TABLE tool_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    agent_name VARCHAR(64) DEFAULT NULL COMMENT '调用Agent',
    tool_name VARCHAR(128) NOT NULL COMMENT '工具名称',
    request_payload JSON DEFAULT NULL COMMENT '请求参数',
    response_payload JSON DEFAULT NULL COMMENT '响应参数',
    call_status VARCHAR(32) NOT NULL COMMENT '调用状态',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    duration_ms BIGINT DEFAULT NULL COMMENT '耗时毫秒',
    error_message TEXT COMMENT '错误信息',
    called_at DATETIME DEFAULT NULL COMMENT '调用时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_application_id (application_id)
) COMMENT='工具调用日志表';

CREATE TABLE llm_trace_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    agent_name VARCHAR(64) DEFAULT NULL COMMENT 'Agent名称',
    model_name VARCHAR(64) NOT NULL COMMENT '模型名称',
    prompt_summary TEXT COMMENT 'Prompt摘要',
    response_summary TEXT COMMENT '响应摘要',
    input_tokens INT DEFAULT 0 COMMENT '输入Token数',
    output_tokens INT DEFAULT 0 COMMENT '输出Token数',
    duration_ms BIGINT DEFAULT NULL COMMENT '耗时毫秒',
    trace_status VARCHAR(32) NOT NULL COMMENT '状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_application_id (application_id)
) COMMENT='LLM调用轨迹表';

CREATE TABLE policy_hit_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    policy_id VARCHAR(64) NOT NULL COMMENT '政策ID',
    clause_id VARCHAR(64) DEFAULT NULL COMMENT '条款ID',
    policy_title VARCHAR(255) DEFAULT NULL COMMENT '政策标题',
    clause_text TEXT COMMENT '条款内容摘要',
    hit_source VARCHAR(64) DEFAULT NULL COMMENT '命中来源',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_application_id (application_id)
) COMMENT='政策命中记录表';

CREATE TABLE state_transition_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    from_status VARCHAR(32) DEFAULT NULL COMMENT '原状态',
    to_status VARCHAR(32) NOT NULL COMMENT '目标状态',
    trigger_event VARCHAR(64) NOT NULL COMMENT '触发事件',
    operator_type VARCHAR(32) DEFAULT NULL COMMENT '操作主体类型',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作主体名称',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_application_id (application_id)
) COMMENT='状态流转日志表';

-- 申请与待发送事件在同一 MySQL 事务中落库，避免数据成功但 RabbitMQ 消息丢失。
CREATE TABLE approval_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    application_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    routing_key VARCHAR(128) NOT NULL,
    payload_json JSON NOT NULL,
    publish_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PUBLISHING/RETRY/PUBLISHED/CONSUMED',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME DEFAULT NULL,
    published_at DATETIME DEFAULT NULL,
    last_error VARCHAR(1000) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_approval_outbox_event (event_id),
    KEY idx_approval_outbox_pending (publish_status, next_retry_at, created_at),
    KEY idx_approval_outbox_application (application_id)
) COMMENT='审批事件Outbox';

CREATE TABLE approval_message_consume_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    application_id BIGINT NOT NULL,
    consume_status VARCHAR(16) NOT NULL COMMENT 'PROCESSING/RETRY/FAILED/COMPLETED',
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
) COMMENT='RabbitMQ审批消息幂等日志';

-- 不同 eventId 也不允许并发执行同一申请，超时锁由下一个消费者清理。
CREATE TABLE approval_execution_lock (
    application_id BIGINT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    locked_by VARCHAR(64) NOT NULL,
    locked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    KEY idx_approval_lock_expire (expires_at)
) COMMENT='审批申请执行锁';

-- 一次审批运行。为后续异步执行、断点恢复和多轮 Agent 返工预留持久化事实层。
CREATE TABLE approval_run (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '运行ID',
    run_no VARCHAR(64) NOT NULL COMMENT '运行编号',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    phase VARCHAR(32) NOT NULL COMMENT 'INTAKE/ANALYSIS/DECISION/COMPLETE',
    flow_status VARCHAR(32) NOT NULL COMMENT '当前工作流状态',
    revision_count INT NOT NULL DEFAULT 0 COMMENT 'RiskWorker返工次数',
    run_status VARCHAR(16) NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/SUCCESS/FAILED/MANUAL',
    current_agent VARCHAR(64) DEFAULT NULL COMMENT '当前Agent',
    error_message TEXT DEFAULT NULL COMMENT '失败原因',
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_approval_run_no (run_no),
    KEY idx_approval_run_application (application_id, created_at),
    KEY idx_approval_run_status (run_status, updated_at)
) COMMENT='审批运行实例';

-- RiskReport、ReviewReport、DecisionProposal 使用追加版本，Agent 不能覆盖其他 Agent 的工件。
CREATE TABLE agent_artifact (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '工件ID',
    run_id BIGINT NOT NULL COMMENT '运行ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    artifact_type VARCHAR(32) NOT NULL COMMENT 'RISK_REPORT/REVIEW_REPORT/DECISION_PROPOSAL',
    artifact_version INT NOT NULL COMMENT '工件版本',
    agent_name VARCHAR(64) NOT NULL COMMENT '创建Agent',
    content_json JSON NOT NULL COMMENT '结构化工件内容',
    based_on_artifact_ids JSON DEFAULT NULL COMMENT '依赖的上游工件ID',
    model_name VARCHAR(64) DEFAULT NULL,
    prompt_version VARCHAR(32) DEFAULT NULL,
    content_hash CHAR(64) DEFAULT NULL COMMENT '内容摘要，防止审计数据被静默修改',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_agent_artifact_version (run_id, artifact_type, artifact_version),
    KEY idx_agent_artifact_application (application_id, created_at)
) COMMENT='Agent结构化共享工件';

CREATE TABLE agent_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '反馈ID',
    run_id BIGINT NOT NULL COMMENT '运行ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    author_agent VARCHAR(64) NOT NULL COMMENT '反馈Agent',
    target_agent VARCHAR(64) NOT NULL COMMENT '目标Agent',
    target_artifact_id BIGINT NOT NULL COMMENT '被审查工件ID',
    feedback_type VARCHAR(32) NOT NULL COMMENT 'CONFLICT/MISSING_EVIDENCE/REVISION',
    feedback_json JSON NOT NULL COMMENT '结构化问题和返工指令',
    requested_action VARCHAR(32) NOT NULL COMMENT 'ACCEPT/REVISE/MANUAL_REVIEW',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_agent_feedback_target (run_id, target_agent, created_at)
) COMMENT='Agent间结构化反馈';

CREATE TABLE agent_checkpoint (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '检查点ID',
    run_id BIGINT NOT NULL COMMENT '运行ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    agent_name VARCHAR(64) NOT NULL COMMENT 'Agent名称',
    step_name VARCHAR(64) NOT NULL COMMENT '步骤名称',
    iteration_no INT NOT NULL DEFAULT 1 COMMENT '迭代次数',
    checkpoint_status VARCHAR(16) NOT NULL COMMENT 'SUCCESS/FAILED',
    state_json JSON DEFAULT NULL COMMENT '恢复所需的最小状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_agent_checkpoint_step (run_id, agent_name, step_name, iteration_no),
    KEY idx_agent_checkpoint_run (run_id, created_at)
) COMMENT='Agent断点恢复记录';

CREATE TABLE policy_retrieval_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '检索日志ID',
    run_id BIGINT DEFAULT NULL COMMENT '运行ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    agent_name VARCHAR(64) NOT NULL COMMENT '调用Agent',
    query_text VARCHAR(1000) NOT NULL COMMENT '脱敏后的检索问题',
    product_code VARCHAR(64) DEFAULT NULL,
    qdrant_collection VARCHAR(128) NOT NULL,
    result_chunks_json JSON DEFAULT NULL COMMENT '命中的chunkId和分数',
    duration_ms BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_policy_retrieval_application (application_id, created_at)
) COMMENT='Qdrant政策检索审计日志';

-- 可选演示黑名单：仅使用哈希值，测试时可通过 SHA2('原始值', 256) 精确查询。
INSERT INTO risk_blacklist
    (subject_type, subject_hash, risk_level, reason_code, source, status, created_by)
VALUES
    ('ID_CARD', SHA2('310101199001019999', 256), 'HIGH', 'KNOWN_FRAUD_IDENTITY', 'DEMO', 'ACTIVE', 'admin'),
    ('MOBILE', SHA2('13900000000', 256), 'HIGH', 'KNOWN_FRAUD_MOBILE', 'DEMO', 'ACTIVE', 'admin')
ON DUPLICATE KEY UPDATE
    risk_level = VALUES(risk_level),
    reason_code = VALUES(reason_code),
    status = VALUES(status);

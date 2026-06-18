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

# Financial Crisis 后端改进方案

本文档记录两项后续重点改造：人工复核列表查询优化，以及将同步审批流程改造成基于消息队列的异步状态机。文档用于指导后续实现，不代表当前代码已经完成这些改造。

## 1. 改造目标与优先顺序

| 优先级 | 改造项 | 主要收益 |
| --- | --- | --- |
| P1 | 人工复核列表改为数据库分页查询 | 消除 N+1 查询，避免全表加载，稳定列表响应时间 |
| P1 | 审批流程异步化 | 缩短接口响应时间，隔离 OCR/LLM 慢调用，提高吞吐和故障恢复能力 |
| P1 | 审批状态机、幂等和并发控制 | 防止重复消费、重复审批和非法状态跳转 |
| P2 | 监控、重试、死信和补偿机制 | 提升异步链路的可观测性与可运维性 |

建议先完成列表查询优化，再实施异步审批。异步改造涉及接口语义、数据库、消息队列、前端交互和运维配置，改动范围更大。

---

## 2. 人工复核列表查询优化

### 2.1 当前问题

当前 `ManualReviewServiceImpl.queryPendingReviews` 的处理方式是：

1. 查询所有待复核工单。
2. 对每张工单分别查询贷款申请。
3. 对每张工单分别查询反欺诈结果。
4. 在 Java 内存中按风险等级、产品编码过滤。
5. 最后通过 `skip/limit` 做内存分页。

假设一页展示 10 条数据，实际可能执行 1 次工单查询、10 次申请查询和 10 次风控查询；而且在分页前已经把全部待处理工单加载进内存。数据量增长后会出现 N+1 查询、数据库连接往返过多、内存占用增长和响应时间不稳定等问题。

### 2.2 目标方案

由数据库一次完成关联、过滤、排序和分页：

```text
Controller
  → ManualReviewService
    → ManualReviewTicketMapper.selectPendingPage(query)
      → manual_review_ticket
        JOIN loan_application
        LEFT JOIN fraud_risk_result
```

建议 SQL 结构：

```sql
SELECT
    t.application_id,
    t.ticket_no,
    COALESCE(f.risk_level, 'MEDIUM') AS risk_level,
    a.product_code,
    a.applicant_name,
    t.review_status,
    t.assigned_to,
    t.created_at
FROM manual_review_ticket t
JOIN loan_application a
    ON a.id = t.application_id
LEFT JOIN fraud_risk_result f
    ON f.application_id = t.application_id
WHERE t.review_status = 'PENDING'
  AND (#{riskLevel} IS NULL OR f.risk_level = #{riskLevel})
  AND (#{productCode} IS NULL OR a.product_code = #{productCode})
ORDER BY t.created_at ASC, t.id ASC
LIMIT #{pageSize} OFFSET #{offset};
```

同时执行一条条件一致的 `COUNT(*)` SQL，返回总数。若后期数据量很大，可把深分页替换成基于 `(created_at, id)` 的游标分页。

### 2.3 接口返回结构

当前接口只返回列表，建议统一为分页响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "pageNo": 1,
    "pageSize": 10,
    "total": 125,
    "items": []
  }
}
```

可以新增通用 `PageResponse<T>`，避免每个列表接口重复定义分页字段。

### 2.4 代码改动清单

- 新增 Mapper 查询参数对象，包含 `riskLevel`、`productCode`、`pageSize` 和 `offset`。
- 新增面向列表展示的查询 DTO，避免用多个实体在 Service 层拼装。
- 在 `ManualReviewTicketMapper` 中新增 `selectPendingPage` 和 `countPending`。
- 在对应 Mapper XML 中使用 JOIN、动态条件和数据库分页。
- `ApprovalStore` 提供分页查询门面，不再向业务层返回全部工单。
- `ManualReviewServiceImpl` 删除 Stream 过滤、逐条查询和内存分页。
- Controller 返回 `PageResponse<ManualReviewPendingResponse>`。
- 前端根据 `total`、`pageNo` 和 `pageSize` 渲染分页器。

### 2.5 数据库约束与索引

建议至少补充：

```sql
ALTER TABLE manual_review_ticket
    ADD INDEX idx_review_pending_page
        (review_status, created_at, id);

ALTER TABLE loan_application
    ADD INDEX idx_loan_product
        (product_code, id);

ALTER TABLE fraud_risk_result
    ADD UNIQUE KEY uk_fraud_application
        (application_id);
```

索引最终应结合 `EXPLAIN` 和真实数据分布确认，不要只根据字段存在与否机械添加。

### 2.6 验收标准

- 列表查询不再逐条调用 `getApplicationOrThrow` 和 `findFraudResult`。
- 一次列表请求只执行分页查询和总数查询，或使用一条带窗口统计的查询。
- 风险等级、产品编码筛选在数据库中完成。
- 排序稳定，同一时间的记录通过 `id` 二次排序。
- `pageSize` 设置上限，例如最大 100。
- 使用至少 1 万条工单数据验证查询计划和响应时间。
- 增加 Mapper 集成测试，覆盖无条件、风险等级、产品编码、组合筛选和边界页码。

---

## 3. 同步审批改为异步状态机

### 3.1 当前问题

创建申请、上传材料或提交补件后，HTTP 请求会同步调用完整审批编排。该链路包含 OCR、规则分析和 LLM 等外部或慢速操作，容易导致：

- 接口等待时间过长或被网关超时中断。
- Web 请求线程和数据库连接被长期占用。
- 用户重试导致审批流程被重复触发。
- 服务重启后难以从中间步骤恢复。
- 单个外部服务故障影响整个接口吞吐。
- 整个编排处于长事务中，事务边界过大。

### 3.2 队列选型建议

RabbitMQ 和 Kafka 都能实现该方案，但适用侧重点不同：

| 维度 | RabbitMQ | Kafka |
| --- | --- | --- |
| 典型用途 | 业务任务队列、路由、延迟重试 | 高吞吐事件流、事件回放、数据管道 |
| 项目初期复杂度 | 较低 | 较高 |
| 消费模型 | 任务完成后确认 | 基于分区和 offset |
| 延迟/死信路由 | 使用体验较直接 | 通常需要重试 Topic 设计 |
| 事件长期保留与回放 | 较弱 | 较强 |

对当前项目，第一阶段推荐 RabbitMQ：审批任务量通常不以超高吞吐为首要目标，更需要明确的任务确认、重试和死信处理。如果后续要建设统一业务事件平台、长期回放审批事件或接入实时数据分析，再考虑 Kafka。

以下设计对两者通用；实现时只选择一种消息中间件，避免同时维护两套链路。

### 3.3 目标架构

```text
客户端提交申请/材料
        ↓
短事务：保存业务数据 + 写审批任务/Outbox
        ↓
HTTP 202 Accepted
        ↓
Outbox 发布器 → RabbitMQ/Kafka
        ↓
审批消费者按 applicationId/runId 消费
        ↓
材料检查 → 风控 → 偿债分析 → LLM 复核 → 合规决策
        ↓
每一步使用独立短事务保存结果和状态日志
        ↓
成功进入 APPROVED/REJECTED，异常重试耗尽后进入 MANUAL_REVIEW
```

不建议在业务事务提交前直接发送消息。数据库写入成功但消息发送失败，或消息发送成功但数据库回滚，都会产生不一致。推荐使用 Transactional Outbox：业务数据和 Outbox 事件在同一数据库事务中落库，再由独立发布器可靠投递。

### 3.4 状态机设计

保留现有业务状态，同时增加明确的运行状态。建议至少定义合法迁移：

```text
SUBMITTED
  → DOCUMENT_PENDING
  → OCR_PARSING

OCR_PARSING
  → DOCUMENT_PENDING
  → RISK_ANALYZING
  → MANUAL_REVIEW

RISK_ANALYZING
  → DECISION_PENDING
  → MANUAL_REVIEW

DECISION_PENDING
  → APPROVED
  → REJECTED
  → MANUAL_REVIEW
```

状态更新必须使用条件更新，不能只在 Java 中先读取再覆盖：

```sql
UPDATE loan_application
SET status = #{toStatus},
    current_step = #{currentStep},
    version = version + 1,
    updated_at = NOW()
WHERE id = #{applicationId}
  AND status = #{expectedStatus}
  AND version = #{version};
```

受影响行数为 0 时，说明消息重复、状态已被其他消费者推进或发生并发冲突。消费者应把它识别为幂等命中或状态冲突，而不是继续覆盖。

### 3.5 消息结构

消息只携带定位和控制字段，不携带完整身份证、OCR 原文等敏感数据：

```json
{
  "eventId": "uuid",
  "eventType": "APPROVAL_REQUESTED",
  "applicationId": 10001,
  "runId": "uuid",
  "trigger": "APPLICATION_CREATED",
  "occurredAt": "2026-07-16T10:00:00+08:00",
  "schemaVersion": 1,
  "traceId": "uuid"
}
```

消费者根据 `applicationId` 从数据库加载最新数据。`eventId` 用于消息去重，`runId` 标识一次审批运行，`schemaVersion` 用于兼容后续消息格式升级。

### 3.6 建议新增的数据表

审批任务表用于查看运行状态、重试次数和错误原因：

```sql
CREATE TABLE approval_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    run_id VARCHAR(64) NOT NULL,
    job_status VARCHAR(32) NOT NULL,
    current_stage VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME DEFAULT NULL,
    last_error_code VARCHAR(64) DEFAULT NULL,
    last_error_message VARCHAR(512) DEFAULT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_approval_job_run_id (run_id),
    KEY idx_approval_job_application (application_id, created_at),
    KEY idx_approval_job_retry (job_status, next_retry_at)
);
```

Outbox 表用于可靠发布消息：

```sql
CREATE TABLE outbox_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload_json JSON NOT NULL,
    publish_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at DATETIME DEFAULT NULL,
    UNIQUE KEY uk_outbox_event_id (event_id),
    KEY idx_outbox_publish (publish_status, next_retry_at, id)
);
```

还需增加消费去重记录，或让 `approval_job.run_id`、阶段结果表唯一约束承担幂等保护。仅依赖消息中间件的“恰好一次”语义不够，业务消费者仍应按至少一次投递设计。

### 3.7 接口调整

创建申请成功后返回 `202 Accepted`，响应中明确表示任务已接收而非审批已完成：

```http
HTTP/1.1 202 Accepted
Location: /api/loan/applications/10001/status
```

```json
{
  "code": 0,
  "message": "accepted",
  "data": {
    "applicationId": 10001,
    "applicationNo": "APP20260716...",
    "runId": "uuid",
    "status": "SUBMITTED",
    "statusUrl": "/api/loan/applications/10001/status"
  }
}
```

材料上传和补件成功后也可返回 202。文件格式错误、申请不存在、当前状态禁止补件等同步校验失败，仍应立即返回相应的 4xx。

状态查询接口建议返回：

- 当前申请状态和当前步骤。
- 当前审批运行 `runId` 与运行状态。
- 是否为终态。
- 最近更新时间。
- 可安全展示的失败原因或补件要求。
- 状态时间线。

前端收到 202 后跳转到状态页，建议前 30 秒每 2 秒轮询一次，随后降低频率；进入 `APPROVED`、`REJECTED`、`MANUAL_REVIEW` 或 `DOCUMENT_PENDING` 后停止轮询。后续可以升级为 SSE 或 WebSocket，但第一阶段不必同时引入。

### 3.8 消费、重试和死信策略

- 消费成功并提交数据库事务后再确认消息。
- 网络超时、限流、临时数据库异常属于可重试错误。
- 参数非法、状态非法、消息格式不兼容属于不可重试错误。
- 使用指数退避，例如 30 秒、2 分钟、10 分钟，最多 3 次。
- 重试耗尽后进入死信队列，并把申请安全地转为 `MANUAL_REVIEW`。
- OCR/LLM 调用必须设置连接超时和读取超时。
- 自动重试前确认第三方调用是否幂等；必要时携带业务请求号。
- 死信消息应支持管理端查询和人工重新投递，但重新投递仍需经过幂等检查。

### 3.9 事务边界

异步化后不再给整个 `startApprovalFlow` 添加一个大事务。建议每个阶段分成三段：

1. 短事务读取并校验当前状态，登记阶段开始。
2. 在事务外调用 OCR、LLM 或其他外部服务。
3. 短事务保存结果、状态变更和状态日志。

异常日志不能依赖随后会回滚的业务事务。可以使用独立事务记录失败信息，或者由消费者捕获异常后更新 `approval_job`。

### 3.10 分阶段实施步骤

#### 第一阶段：建立异步骨架

- 选择 RabbitMQ 或 Kafka，并补充依赖和环境配置。
- 新增 `approval_job`、`outbox_event` 和数据库迁移脚本。
- 创建申请时只保存申请、任务和 Outbox 事件。
- Controller 返回 202 和状态查询地址。
- 实现 Outbox 发布器和一个审批消费者。

#### 第二阶段：拆分审批步骤

- 把当前编排中的材料、风控、偿债、LLM 和决策拆成可独立执行的阶段。
- 每阶段使用短事务和条件状态更新。
- 增加 `runId`、`eventId` 和唯一约束，实现重复消息安全消费。
- 明确各阶段的可重试错误和不可重试错误。

#### 第三阶段：故障治理

- 配置退避重试、死信队列和人工补偿入口。
- 添加队列积压、失败率、阶段耗时和死信数量监控。
- 支持服务重启后继续处理未完成任务。
- 做消费者并发、乱序、重复消息和消息丢失演练。

#### 第四阶段：前端适配

- 创建、上传和补件接口按 202 处理。
- 状态页实现轮询和退避。
- 明确展示排队中、处理中、等待补件、人工复核和终态。
- 避免用户因页面无响应重复提交。

### 3.11 验收标准

- 创建申请接口不等待 OCR 或 LLM，正常情况下快速返回 202。
- 消息重复投递不会产生重复决策、重复工单或非法状态覆盖。
- 消费者重启后可以继续处理未完成任务。
- OCR/LLM 暂时失败会按策略重试，重试耗尽后安全转人工复核。
- 状态主表和状态日志保持一致。
- 外部调用不处于长数据库事务中。
- 能通过 `applicationId/runId/traceId` 定位完整审批链路。
- 队列积压、失败率、重试次数和死信数量均可监控。
- 前端能够通过状态接口完整展示审批进度，并在终态停止轮询。

---

## 4. 测试计划

### 人工复核列表

- 验证分页 SQL 没有 N+1 查询。
- 验证风险等级、产品编码及组合筛选。
- 验证默认风险等级和 `LEFT JOIN` 场景。
- 验证第一页、最后一页、空结果和超过最大 `pageSize`。
- 使用 MySQL Testcontainers 检查真实 SQL、索引和 JSON/枚举映射。

### 异步审批

- 正常消息能够推进到审批终态。
- 同一 `eventId` 重复消费只执行一次业务效果。
- 同一申请并发消息不会越过状态机。
- 消费过程中服务退出，消息能够重新投递并安全恢复。
- OCR/LLM 超时能够重试，重试耗尽后进入人工复核和死信队列。
- Outbox 发布失败后能够补发，不丢失审批任务。
- 数据库事务回滚时不会发布不存在的业务事件。
- Controller 正确返回 202、`Location` 和状态查询信息。

## 5. 实施注意事项

- 不要在一次提交中同时完成列表优化、消息队列、认证授权和全部数据库重构，建议拆成可独立回滚的变更。
- 数据库变更应通过 Flyway 或 Liquibase 管理，不再直接修改一份总建表 SQL。
- 消息体和日志不得包含完整身份证号、手机号、图片 Base64 或 OCR 原文。
- RabbitMQ/Kafka 不能代替业务幂等、数据库约束和状态条件更新。
- 异步系统应接受短时间的最终一致性，接口和前端文案不能暗示 202 等于审批成功。

## 6. 推荐交付拆分

| 变更 | 内容 |
| --- | --- |
| PR 1 | 人工复核 JOIN 查询、数据库分页、索引和测试 |
| PR 2 | `approval_job`、Outbox 表和迁移脚本 |
| PR 3 | 消息发布器、审批消费者、202 接口响应 |
| PR 4 | 审批阶段短事务、状态机、幂等和并发控制 |
| PR 5 | 重试、死信、监控和故障恢复测试 |
| PR 6 | 前端状态轮询与异步交互适配 |

按以上顺序实施，可以先获得查询性能收益，再逐步替换同步审批链路，并确保每一步都有清晰的测试和回滚边界。

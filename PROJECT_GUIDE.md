# Financial Crisis 项目导读

> 目标：帮助第一次接触本项目的人，在较短时间内理解它解决什么问题、一次申请如何流转、代码应按什么顺序阅读，以及哪些部分仍属于演示实现。

## 1. 一句话认识项目

这是一个基于 **Spring Boot + MyBatis + LangChain4j** 的智能信贷审批后端。系统接收贷款申请和材料后，先通过确定性工具产生材料、反欺诈和偿债事实，再由 Risk、Review、Decision 三个 LLM Worker 协作完成分析、复核和决策，最后通过 PolicyGuard 校验安全边界。

它最值得学习的是以下三点：

1. 多个职责单一的 LLM Worker 如何通过结构化工件共享结论并由编排器串成完整流程。
2. 如何让 LLM 负责主要分析和决策，同时用确定性工具及 PolicyGuard 保护金融安全边界。
3. 如何通过状态日志、Agent 日志、工具日志和决策记录保留可审计证据。

## 2. 业务主流程

```text
客户端提交申请
  ↓
创建 LoanApplication（SUBMITTED）
  ↓
AgentOrchestrationServiceImpl 启动同步审批
  ↓
DocumentIntakeAgent：检查材料并调用 OCR
  ├─ 材料不足 → DOCUMENT_PENDING，等待补件
  └─ 材料齐全
       ↓
FraudRiskAgent：生成确定性反欺诈工具结果
       ↓
RepaymentCapacityAgent：生成确定性偿债测算结果
       ↓
RiskWorker：综合工具事实与 Qdrant 政策证据
       ↓
ReviewWorker：独立检查证据、冲突，必要时触发一次返工
       ↓
DecisionWorker：生成最终审批建议
       ↓
PolicyGuard：校验黑名单、DTI、额度、证据和置信度边界
  ├─ APPROVED
  ├─ REJECTED
  └─ MANUAL_REVIEW
```

关键原则：**LLM 负责主要分析、复核和决策，确定性代码只负责事实工具、流程路由和安全护栏**。LLM 未配置、调用失败或返回非法 JSON 时，系统不会用本地模拟结果冒充 Agent 输出，而是转人工复核。

## 3. 代码分层地图

| 层次 | 目录/文件 | 职责 |
| --- | --- | --- |
| 启动与配置 | `FinancialCrisisApplication.java`、`application.yml`、`LangChain4jConfig.java` | 启动 Spring Boot，配置数据库、OCR、LLM |
| Web 接口 | `controller/` | 接收贷款申请、材料上传、状态/报告查询及人工复核操作 |
| DTO | `dto/request/`、`dto/response/` | 定义接口输入输出，隔离接口模型和数据库实体 |
| 业务服务 | `service/impl/` | 处理申请、材料、报告、人工复核和审计业务 |
| 审批编排 | `AgentOrchestrationServiceImpl.java` | 项目的流程大脑：决定 Agent 顺序、推进状态、异常转人工 |
| Agent | `agent/worker/` | Risk、Review、Decision 三个独立 LLM Worker |
| 工具与护栏 | `agent/tool/`、`agent/guard/` | Qdrant 政策检索工具与最终安全边界 |
| 协作上下文 | `agent/collaboration/` | 在 Agent 之间传递结构化发现和案件上下文 |
| 持久化门面 | `store/ApprovalStore.java` | 统一组合多个 Mapper，维护数据和状态日志 |
| 数据访问 | `mapper/`、`resources/mapper/` | MyBatis 接口和 SQL 映射 |
| 领域模型 | `domain/entity/`、`domain/enums/` | 数据库实体、申请状态、风险等级和决策结果 |
| 数据库 | `智能信贷审批Agent-数据库建表.sql` | 表结构、演示账号及审计相关数据设计 |

典型后端调用链如下：

```text
Controller → Service → AgentOrchestrationService → Agent → ApprovalStore → Mapper → MySQL
```

## 4. 推荐学习文件顺序

### 阶段一：建立全局认识（约 30 分钟）

1. `README.md`：了解用途、技术栈、环境变量和启动方式。
2. `pom.xml`：确认 Java 17、Spring Boot、MyBatis、LangChain4j 等依赖。
3. `src/main/resources/application.yml`：理解数据库、OCR 和 LLM 的配置入口。
4. `src/main/java/com/erbu/financialcrisis/FinancialCrisisApplication.java`：确认应用启动入口。
5. `src/main/java/com/erbu/financialcrisis/domain/enums/ApplicationStatus.java`：先掌握状态机词汇。
6. `DecisionResult.java`、`RiskLevel.java`、`DocumentType.java`：掌握审批核心枚举。

完成标志：能说清系统有哪些状态、可能输出哪些审批结论、依赖哪些外部服务。

### 阶段二：沿一次申请读通后端主链路（约 1～2 小时）

7. `controller/LoanApplicationController.java`：找到创建申请入口。
8. `dto/request/CreateLoanApplicationRequest.java`：查看创建申请接口接收哪些字段及其校验规则。
9. `service/impl/LoanApplicationServiceImpl.java`：理解申请落库后为何立即启动审批。
10. `service/impl/AgentOrchestrationServiceImpl.java`：重点精读 `startApprovalFlow`，这是全项目最核心文件。
11. 按实际执行顺序阅读 Agent：
    - `agent/DocumentIntakeAgent.java`
    - `agent/FraudRiskAgent.java`
    - `agent/RepaymentCapacityAgent.java`
    - `agent/worker/RiskWorker.java`
    - `agent/worker/ReviewWorker.java`
    - `agent/worker/DecisionWorker.java`
    - `agent/guard/PolicyGuard.java`
12. `agent/collaboration/ApprovalCaseContext.java` 和 `AgentFinding.java`：理解 Agent 之间传递什么，而不是只看调用顺序。

阅读 Agent 时建议为每个类回答四个问题：输入是什么、输出是什么、核心阈值是什么、什么情况会转人工或拒绝。

### 阶段三：理解数据如何保存和审计（约 1 小时）

13. `store/ApprovalStore.java`：先看持久化总入口，尤其是 `changeStatus` 如何同时更新主表并写状态日志。
14. `domain/entity/LoanApplication.java`、`FraudRiskResult.java`、`RepaymentCapacityResult.java`、`ApprovalDecision.java`、`ManualReviewTicket.java`。
15. `智能信贷审批Agent-数据库建表.sql`：把实体对应到申请、结果、工单、状态日志、Agent 日志和工具日志等表。
16. 任选一组 Mapper 对照阅读，例如 `LoanApplicationMapper.java` + `resources/mapper/LoanApplicationMapper.xml`，掌握项目的数据访问方式即可，不必一开始逐个看完。
17. `service/impl/AuditTimelineServiceImpl.java`：理解审计信息如何被聚合展示。

### 阶段四：理解材料、补件和人工兜底（约 45 分钟）

18. `controller/DocumentController.java` → `service/impl/DocumentServiceImpl.java` → `BaiduQianfanOcrService.java`。
19. `controller/AdminReviewController.java` → `service/impl/ManualReviewServiceImpl.java`。
20. `controller/ApprovalReportController.java` → `service/impl/ApprovalReportServiceImpl.java`。

完成标志：能够解释材料不足、OCR/LLM 异常、中风险案件分别如何收口。

### 阶段五：系统梳理后端接口（约 45 分钟）

21. `controller/LoanApplicationController.java`：申请创建、列表、详情和状态接口。
22. `controller/DocumentController.java`：材料上传和补件接口。
23. `controller/ApprovalReportController.java`：审批报告查询接口。
24. `controller/AdminReviewController.java`：待复核列表、详情、人工通过和拒绝接口。
25. `controller/AuditController.java`：审计时间线接口。
26. `common/Result.java`：统一响应结构。
27. `config/GlobalExceptionHandler.java` 和 `common/BusinessException.java`：异常如何转换为接口响应。

### 阶段六：用测试验证理解（约 30 分钟）

28. `PolicyGuardTests.java`：重点看 LLM 建议不能绕过的安全边界。
29. `BaiduQianfanOcrServiceTests.java`：了解 OCR 服务的测试方式。
30. `FinancialCrisisApplicationTests.java`：查看应用上下文的基础验证。

## 5. 建议的动手学习路径

1. 创建数据库并执行 `智能信贷审批Agent-数据库建表.sql`。
2. 复制 `.env.example` 中需要的配置到本机环境，至少正确设置数据库连接。
3. 启动后端，访问 `/actuator/health` 确认服务正常。
4. 使用 Postman、Apifox 或 `curl` 调用 `POST /api/loan/applications` 创建申请。
5. 在数据库中同时观察 `loan_application` 和 `state_transition_log` 的变化。
6. 调用材料上传接口后，继续观察 `agent_task_log`、`fraud_risk_result`、`repayment_capacity_result` 和 `approval_decision`。
7. 暂时不配置 DeepSeek Key，再提交一笔申请，验证严格模式是否转人工复核。
8. 调用 `/api/admin/reviews` 和 `/api/admin/audit` 下的接口，验证人工复核与审计时间线。
9. 修改一个低风险阈值并补充测试，观察最终决策变化；这是理解规则边界最快的练习。

常用启动命令：

```bash
# 后端（项目根目录）
mvn spring-boot:run

# 后端测试
mvn test
```

## 6. 需要特别注意的设计与现状

- **审批是同步执行的。** 创建申请的 HTTP 请求会直接进入多 Agent 流程；真实系统通常会改成消息队列或任务编排，并加入幂等、重试和超时治理。
- **后端暂未实现认证授权。** 用户端和管理端接口都没有真正的身份认证、角色校验与数据权限控制，不能直接用于生产环境。
- **规则和收入测算是示例逻辑。** 例如偿债 Agent 会按就业类型和工作年限估算收入，不能视为真实授信模型。
- **LLM 是主要决策者但不能绕过护栏。** 模型不可用、响应不合法、证据不足或输出越界都会转人工。
- **数据库配置要本地化。** `application.yml` 中存在默认数据库连接信息，学习时应通过环境变量覆盖；真实项目不应在仓库中保留可用密码。
- **部分表暂未进入主流程。** 数据库中的账号、申请人画像、征信评估、LLM Trace 等设计比当前业务实现更完整，阅读时要区分“已落地链路”和“预留模型”。
- **文件处理是演示方案。** 当前实现不把原图写入本地数据库，实际生产还需对象存储、病毒扫描、加密、脱敏、留存和访问控制。

## 7. 二次开发从哪里下手

| 目标 | 优先阅读/修改 |
| --- | --- |
| 增加新的风险事实工具 | `FraudRiskAgent.java`、`RiskWorker.java`、对应测试 |
| 调整额度或 DTI 边界 | `RepaymentCapacityAgent.java`、`PolicyGuard.java` |
| 增加一个新 Agent | 新 Agent 类、`ApprovalCaseContext`、`AgentOrchestrationServiceImpl`、日志表 |
| 更换 LLM | `LangChain4jConfig.java`、`StructuredLlmClient.java`、`application.yml` |
| 更换 OCR | `QianfanOcrService.java`、`BaiduQianfanOcrService.java`、`DocumentIntakeAgent.java` |
| 增加接口 | 对应 `controller` → `service` → `store/mapper` |
| 做真正的登录权限 | 引入 Spring Security，增加身份认证、角色权限和申请数据隔离 |
| 改成异步审批 | 拆分 `createApplication` 与 `startApprovalFlow`，引入队列/任务状态、幂等和重试 |

## 8. 学完后应能回答的问题

- 为什么 `AgentOrchestrationServiceImpl` 是流程大脑，而每个 Agent 应保持职责单一？
- 材料不足与 OCR/LLM 执行异常分别会进入什么状态？
- `RiskWorker`、`ReviewWorker`、`DecisionWorker` 和 `PolicyGuard` 的职责边界有何不同？
- 为什么主表状态更新必须和 `state_transition_log` 在同一事务内完成？
- 一次最终决策能从哪些表和日志中还原依据？
- 当前实现距离真实生产信贷系统还缺哪些安全、稳定性和合规能力？

如果能不看代码完整讲清上述问题，就已经掌握了这个后端项目的主干；之后再按具体开发任务深入 DTO、Mapper 和 SQL 细节会更高效。

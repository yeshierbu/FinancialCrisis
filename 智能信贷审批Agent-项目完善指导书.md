# 智能信贷审批 Agent 项目完善指导书

## 1. 指导书目的

这份文档的目标，不是继续补“项目介绍”，而是把这个项目后续应该怎么完善，拆成一套可以直接执行的开发说明。

你现在已经有了：

- 项目计划书
- API 接口文档
- 数据库建表 SQL
- 技术注意点
- Java 项目骨架

接下来最重要的事情，是把骨架项目逐步补成一个：

- 能运行
- 能演示
- 能讲清楚
- 能继续扩展
- 能拿去面试讲架构和实现细节

这份指导书会重点回答两类问题：

1. 每个功能到底要写什么
2. 每个功能具体用什么方法实现

项目整体建议按照这条路线推进：

`先跑通主流程 -> 再补持久化 -> 再补 Agent -> 再补工具 -> 再补 RAG -> 再补工程化能力`

不要一开始同时推进：

- MySQL
- 文件上传
- OCR
- LangChain4j
- RAG
- 状态机
- MQ
- Redis
- 报表导出
- 脱敏

这样会让项目失去主线。

---

## 2. 总体开发原则

### 2.1 开发顺序原则

推荐拆成 6 个阶段：

1. 第一阶段：把申请和审批主流程跑通
2. 第二阶段：把数据库和持久化接完整
3. 第三阶段：把 Agent 与工具调用链路补齐
4. 第四阶段：把 LangChain4j 和 RAG 接入决策解释
5. 第五阶段：把状态机、日志、异常和人工复核补完整
6. 第六阶段：做缓存、异步、脱敏、监控、报表等工程增强

### 2.2 实现原则

你后续写代码时，必须遵守四个硬性原则：

#### 原则 1：先最小可运行，再逐步升级

例如：

- 先内存存储，再 MySQL
- 先同步调用，再异步编排
- 先规则判断，再接大模型
- 先关键词检索，再向量检索

#### 原则 2：每个模块都必须职责单一

例如：

- Controller 只接参数和返回响应
- Service 只做业务编排
- Mapper 只做数据访问
- Agent 只负责某一类审批分析
- Tool 只负责对外部能力做封装

#### 原则 3：结构化优先

无论是审批结果、Agent 输出、日志、工具调用结果，都尽量结构化，避免一开始就全写成自然语言。

推荐优先使用：

- DTO
- 枚举
- JSON 字段
- 状态码
- 风险标签列表

#### 原则 4：每完成一层都必须能演示

每完成一个阶段，都要满足：

- 接口能调用
- 数据能查到
- 状态能解释
- 流程能讲清楚

---

## 3. 总体模块拆分建议

整个项目建议拆成下面几类模块：

### 3.1 用户端模块

负责贷款申请的提交、资料上传、进度查询、报告查看。

建议对应功能：

- 创建贷款申请
- 上传申请材料
- 提交补充资料
- 查询申请详情
- 查询审批进度
- 获取审批报告

### 3.2 管理端模块

负责人工复核、复核结果处理、审计信息查看。

建议对应功能：

- 查询待人工复核列表
- 查询人工复核详情
- 人工复核通过
- 人工复核拒绝

### 3.3 审批编排模块

负责把多个 Agent 串成完整审批流程。

建议对应模块：

- AgentOrchestrationService
- 状态流转控制
- Agent 执行日志
- 异常降级与人工转派

### 3.4 Agent 模块

负责分步骤完成自动审批。

建议拆成：

- 信息采集 Agent
- 反欺诈风控 Agent
- 偿债能力 Agent
- 合规决策 Agent

### 3.5 工具模块

负责提供 Agent 可调用的外部能力封装。

建议拆成：

- OCR 解析工具
- 黑名单校验工具
- 征信摘要工具
- 收入分析工具
- DTI 计算工具
- 政策检索工具

### 3.6 数据与审计模块

负责落库、日志、轨迹、报告、状态历史。

建议重点表：

- `loan_application`
- `uploaded_document`
- `fraud_risk_result`
- `repayment_capacity_result`
- `approval_decision`
- `manual_review_ticket`
- `agent_task_log`
- `tool_call_log`
- `llm_trace_log`
- `state_transition_log`

---

## 4. 第一阶段：先把主流程跑通

### 4.1 阶段目标

第一阶段不要追求“智能”，只追求“流程真的能走完”。

这阶段必须做到：

- 用户能提交贷款申请
- 系统能生成申请单
- 系统能查询申请详情
- 系统能模拟走完审批流程
- 系统能返回一个审批结果

### 4.2 这一阶段建议先写哪些类

优先补全：

1. `LoanApplicationController`
2. `LoanApplicationService`
3. `LoanApplicationServiceImpl`
4. `LoanApplication` 实体
5. `AgentOrchestrationService`
6. `AgentOrchestrationServiceImpl`
7. `LoanApplicationResponse`
8. `CreateLoanApplicationRequest`

### 4.3 第一阶段推荐实现方法

第一版先用：

- `ConcurrentHashMap` 模拟数据库
- `AtomicLong` 模拟主键
- 固定规则模拟审批结果
- 同步串行方式跑完审批链路

先不要引入：

- MySQL
- Redis
- MQ
- LangChain4j
- RAG
- 文件上传

### 4.4 完成标准

- Postman 能成功调创建申请接口
- 能查到申请记录
- 能看到状态字段变化
- 能返回一份固定审批结果

---

## 5. 功能级完善说明

这一章是整份指导书的核心。下面按功能逐个说明：

- 这个功能负责什么
- 第一版写到什么程度
- 具体实现方法是什么
- 涉及哪些表和类
- 后续怎么升级

---

## 6. 功能一：创建贷款申请

### 6.1 功能目标

这是整个系统的入口。用户提交基础资料后，系统要创建一笔申请单，并启动审批流程。

### 6.2 功能职责

这个功能至少完成四件事：

1. 校验用户输入
2. 生成申请单编号
3. 保存申请主数据
4. 启动审批编排流程

### 6.3 请求字段建议

结合现有 API 文档，建议第一版使用这些字段：

- `productCode`
- `applicantName`
- `idCardNo`
- `mobile`
- `loanAmount`
- `loanTerm`
- `employmentType`
- `companyName`
- `workYears`

### 6.4 实现方法

#### 第一版实现方法

用当前骨架直接落地：

1. `Controller` 接收 `CreateLoanApplicationRequest`
2. 用 `@Valid` 做基础参数校验
3. `Service` 里创建 `LoanApplication` 实体
4. 生成 `applicationNo`
5. 设置初始状态为 `SUBMITTED`
6. 保存到内存 `STORE`
7. 调用 `agentOrchestrationService.startApprovalFlow(application)`
8. 返回申请编号、状态、当前步骤

#### 编号生成建议

第一版可以继续使用：

`APP-` + 12 位随机串

后续升级建议：

- 增加日期前缀
- 增加渠道位
- 增加幂等控制

例如：

`APP20260618WEB000001`

### 6.5 代码实现要点

建议放在：

- `LoanApplicationController#createApplication`
- `LoanApplicationServiceImpl#createApplication`

要点如下：

1. 参数校验用 `jakarta.validation`
2. 主键先用 `AtomicLong`
3. 创建时间用 `LocalDateTime.now()`
4. 初始状态建议统一用枚举 `ApplicationStatus.SUBMITTED`
5. 当前步骤可以先写成 `申请已提交`

### 6.6 数据表映射

后续接 MySQL 时，对应主表：

- `loan_application`

第一批建议映射字段：

- `id`
- `application_no`
- `product_code`
- `applicant_name`
- `id_card_no`
- `mobile`
- `loan_amount`
- `loan_term`
- `employment_type`
- `company_name`
- `work_years`
- `status`
- `current_step`
- `created_at`
- `updated_at`

### 6.7 后续升级方向

- 增加幂等提交能力
- 增加手机号和身份证唯一性规则
- 增加渠道参数
- 增加草稿保存能力
- 增加同一申请人短期多头申请拦截

---

## 7. 功能二：查询申请详情

### 7.1 功能目标

用户提交申请后，需要查看这笔申请当前的核心信息和审批状态。

### 7.2 功能职责

这个接口负责返回：

- 申请单基本信息
- 当前审批状态
- 当前处理步骤
- 关键审批结果摘要

### 7.3 实现方法

#### 第一版实现方法

1. `Controller` 接收 `applicationId`
2. `Service` 从内存 `STORE` 查询申请对象
3. 转换成 `LoanApplicationResponse`
4. 返回状态和当前步骤

#### 第二版实现方法

接数据库后改为：

1. 用 `LoanApplicationMapper` 查询主表
2. 用 `ApprovalDecisionMapper` 查询决策结果
3. 可选查询人工复核工单
4. 聚合成详情 DTO 返回

### 7.4 建议返回字段

第一版建议至少返回：

- `applicationId`
- `applicationNo`
- `productCode`
- `applicantName`
- `loanAmount`
- `loanTerm`
- `status`
- `currentStep`

第二版可以扩展：

- `decisionResult`
- `approvedAmount`
- `rejectReasonCode`
- `manualReviewFlag`
- `updatedAt`

### 7.5 异常处理建议

当申请单不存在时，不要直接返回 `null`。

建议改成：

- 抛出 `BusinessException`
- 统一返回错误码 `4002`

### 7.6 后续升级方向

- 增加脱敏展示
- 增加不同角色字段裁剪
- 增加审批摘要信息

---

## 8. 功能三：查询审批进度

### 8.1 功能目标

这个接口比“查询申请详情”更聚焦，它专门负责告诉前端：

- 现在走到哪一步了
- 历史上经过哪些状态
- 下一步是什么

### 8.2 功能职责

建议返回：

- 当前状态
- 当前步骤说明
- 最后更新时间
- 状态时间线

### 8.3 实现方法

#### 第一版实现方法

即使还没接数据库，也建议先设计状态流转记录结构。

可以先在内存里维护一个：

- `Map<Long, List<StateTransitionLog>>`

每次状态变化时：

1. 更新 `loan_application.status`
2. 更新 `loan_application.current_step`
3. 追加一条状态流转日志

#### 第二版实现方法

接数据库后，落表：

- `state_transition_log`

查询进度时：

1. 查询 `loan_application`
2. 查询 `state_transition_log`
3. 按 `created_at` 升序组装时间线

### 8.4 推荐状态枚举

建议统一定义在 `ApplicationStatus`：

- `SUBMITTED`
- `DOCUMENT_PENDING`
- `OCR_PARSING`
- `RISK_ANALYZING`
- `DECISION_PENDING`
- `MANUAL_REVIEW`
- `APPROVED`
- `REJECTED`

### 8.5 状态推进方法建议

不要在各个 Agent 里随手 `setStatus`。

建议单独封装一个状态推进方法，例如：

- `updateStatus(application, toStatus, currentStep, triggerEvent, operator)`

这个方法内部统一做：

1. 校验状态是否合法
2. 更新主表状态
3. 写状态日志
4. 更新 `updatedAt`

### 8.6 后续升级方向

- 引入 Spring StateMachine
- 增加状态回退与重试
- 区分同步状态和异步任务状态

---

## 9. 功能四：上传申请材料

### 9.1 功能目标

系统不能只接基础字段，后续还要支持身份证、流水、征信报告等材料上传。

### 9.2 功能职责

这个功能至少负责：

1. 接收用户上传的文件
2. 校验文件格式和大小
3. 保存文件元数据
4. 触发 OCR 或解析任务

### 9.3 实现方法

#### 第一版实现方法

第一版不必真的接对象存储，可以先用本地模拟：

1. `Controller` 使用 `MultipartFile`
2. 校验 `documentType`
3. 校验扩展名与大小
4. 文件先落本地临时目录或写一个假地址
5. 保存 `uploaded_document`
6. 把 `ocr_status` 置为 `PENDING`

#### 第二版实现方法

升级为：

1. 文件上传到 MinIO 或 OSS
2. 返回 `fileUrl`
3. 投递异步 OCR 任务
4. OCR 完成后回写 `parse_result_json`

### 9.4 建议支持的材料类型

- `ID_CARD_FRONT`
- `ID_CARD_BACK`
- `BANK_STATEMENT`
- `INCOME_CERTIFICATE`
- `CREDIT_REPORT`
- `HOUSEHOLD_BOOK`

### 9.5 表设计映射

对应：

- `uploaded_document`

重点字段：

- `application_id`
- `document_type`
- `file_name`
- `file_url`
- `file_size`
- `file_hash`
- `ocr_status`
- `parse_result_json`

### 9.6 关键校验点

- 文件格式白名单
- 文件大小限制
- 材料类型合法性
- 申请单状态是否允许上传

### 9.7 后续升级方向

- 接 MinIO
- 接病毒扫描
- 接文件去重
- 接 OCR 异步队列

---

## 10. 功能五：提交补充资料

### 10.1 功能目标

当自动审批发现资料不足时，需要通知用户补件，用户补件后系统继续审批。

### 10.2 功能职责

这个功能负责：

1. 接收补件说明
2. 接收补件材料列表
3. 更新申请状态
4. 重新拉起审批流程

### 10.3 实现方法

建议设计一个补件 DTO，包含：

- `remark`
- `documents`

每个 `document` 包含：

- `documentType`
- `fileUrl`

服务层处理步骤：

1. 校验申请单存在
2. 校验当前状态必须是 `DOCUMENT_PENDING`
3. 保存补件记录或材料记录
4. 状态改为 `SUBMITTED` 或 `OCR_PARSING`
5. 重新触发 `AgentOrchestrationService`

### 10.4 第一版建议

第一版可以不额外建补件表，直接复用：

- `uploaded_document`
- `state_transition_log`

如果你想写得更完整，可以新增：

- `application_supplement_record`

### 10.5 后续升级方向

- 增加补件次数限制
- 增加超时自动关闭
- 增加补件通知能力

---

## 11. 功能六：信息采集 Agent

### 11.1 功能目标

信息采集 Agent 负责把用户提交的原始资料整理成后续 Agent 可直接消费的结构化数据。

### 11.2 角色边界

这个 Agent 不负责“是否通过”，只负责“信息是否完整、字段是否提取出来、材料是否可用”。

### 11.3 建议输入

建议输入 DTO 包含：

- 申请主信息
- 已上传材料列表
- 材料类型
- OCR 解析结果

### 11.4 建议输出

- `documentComplete`
- `missingDocuments`
- `parsedIdentityInfo`
- `parsedIncomeInfo`
- `parseConfidence`
- `needSupplement`

### 11.5 实现方法

#### 第一版实现方法

先不要接真实 OCR。

可以写一个本地模拟工具：

- 身份证文件就固定返回姓名、身份证号、住址
- 银行流水就固定返回月均收入、近 6 个月收入波动
- 征信报告就固定返回评分、逾期次数

Agent 内部处理步骤建议：

1. 校验必要材料是否已上传
2. 按材料类型逐个调用解析工具
3. 汇总解析结果
4. 判断是否缺件
5. 缺件则推进到 `DOCUMENT_PENDING`
6. 不缺件则推进到 `RISK_ANALYZING`

### 11.6 对应工具建议

- `OcrParseTool`
- `DocumentCheckTool`

### 11.7 后续升级方向

- 接入真实 OCR 平台
- 做字段置信度校验
- 做证件真伪核验

---

## 12. 功能七：反欺诈风控 Agent

### 12.1 功能目标

反欺诈风控 Agent 负责识别明显的欺诈和异常风险，输出风险等级、风险标签和建议动作。

### 12.2 角色边界

这个 Agent 负责判断“有没有风险”，但不负责给出最终审批额度。

### 12.3 建议输入

- 申请主信息
- 身份证信息
- 手机号
- 工作信息
- OCR 解析结果
- 征信摘要

### 12.4 建议输出

- `riskLevel`
- `riskScore`
- `riskTags`
- `ruleHits`
- `suggestedAction`

### 12.5 第一版实现方法

先用规则模拟，不要一开始接外部反欺诈平台。

推荐第一版规则：

1. 身份证尾号命中模拟黑名单，判高风险
2. 手机号格式异常，记一个风险标签
3. 申请金额过高且工龄过短，记一个风险标签
4. 征信逾期次数过多，建议人工复核

### 12.6 建议规则实现方式

可以先用普通 Java 方法实现，不必急着上 Drools：

- `boolean hitBlacklist(String idCardNo)`
- `boolean mobileSuspicious(String mobile)`
- `boolean highAmountLowWorkYears(BigDecimal amount, Integer workYears)`

然后把命中的标签收集成：

- `List<String> riskTags`
- `List<String> ruleHits`

### 12.7 表设计映射

对应：

- `fraud_risk_result`

建议写入：

- `risk_level`
- `risk_score`
- `risk_tags_json`
- `rule_hits_json`
- `suggested_action`

### 12.8 状态推进建议

建议规则如下：

- 高风险直接 `MANUAL_REVIEW`
- 中风险继续走偿债能力分析，但保留人工复核标记
- 低风险进入下一 Agent

### 12.9 后续升级方向

- 接黑名单库
- 接设备指纹
- 接多头借贷查询
- 用规则引擎管理规则

---

## 13. 功能八：偿债能力 Agent

### 13.1 功能目标

偿债能力 Agent 负责评估申请人的收入、债务、月供承受能力，输出建议额度和风险结论。

### 13.2 角色边界

这个 Agent 负责回答：

- 用户有没有能力还款
- 建议最多给多少额度

但不直接输出最终审批结论。

### 13.3 建议输入

- 申请金额
- 申请期数
- 月收入
- 现有债务支出
- 征信中的贷款负债
- 工作稳定性信息

### 13.4 建议输出

- `stableMonthlyIncome`
- `monthlyDebtPayment`
- `dti`
- `foir`
- `disposableIncome`
- `incomeStabilityScore`
- `maxAffordableEmi`
- `recommendedCreditLimit`

### 13.5 第一版实现方法

先用固定公式即可。

建议实现下面几个方法：

1. `calculateMonthlyPayment`
2. `calculateDti`
3. `calculateDisposableIncome`
4. `estimateIncomeStabilityScore`
5. `calculateRecommendedLimit`

### 13.6 推荐计算规则

#### DTI 计算

`DTI = 月负债支出 / 稳定月收入`

#### FOIR 计算

`FOIR = 固定支出 / 月收入`

#### 可支配收入

`可支配收入 = 月收入 - 月负债支出 - 固定生活成本`

#### 建议额度

第一版可以简单写成：

`recommendedCreditLimit = maxAffordableEmi * loanTerm * 系数`

系数可以先取：

- `0.7`
- `0.8`

### 13.7 决策建议规则

可以先定义：

- `DTI > 0.7`：高风险，建议拒绝或人工复核
- `0.5 < DTI <= 0.7`：中风险，建议降额
- `DTI <= 0.5`：可继续审批

### 13.8 表设计映射

对应：

- `repayment_capacity_result`

### 13.9 后续升级方向

- 引入更多收入维度
- 引入现金流波动分析
- 引入不同产品差异化授信策略

---

## 14. 功能九：合规决策 Agent

### 14.1 功能目标

这是自动审批链路的最终汇总节点。它负责综合前面几个 Agent 的结果，生成：

- 最终审批结论
- 审批额度
- 利率或期数建议
- 拒绝原因
- 决策解释

### 14.2 角色边界

这个 Agent 是“汇总决策者”，但它不能脱离前面 Agent 的结构化结果乱做判断。

### 14.3 建议输入

- 信息采集结果
- 反欺诈结果
- 偿债能力结果
- 政策检索结果
- 产品规则配置

### 14.4 建议输出

- `decisionResult`
- `approvedAmount`
- `interestRate`
- `loanTerm`
- `rejectReasonCode`
- `decisionExplanation`
- `policyReferences`

### 14.5 第一版实现方法

第一版强烈建议先用规则，不要第一步就让大模型直接给结论。

推荐实现顺序：

1. 先判断是否缺件
2. 再判断是否命中硬性拒绝规则
3. 再判断是否需要人工复核
4. 最后给出通过/拒绝/人工复核结果

### 14.6 推荐硬规则示例

- 身份证命中黑名单：直接拒绝
- `DTI > 0.7` 且收入不稳定：拒绝
- 材料缺失：补件
- 风险标签较多但未命中拒绝线：人工复核
- 风险低且偿债能力达标：通过

### 14.7 建议实现方式

先写一个纯 Java 决策方法，例如：

- `DecisionResult decide(FraudRiskResult, RepaymentCapacityResult, DocumentResult)`

方法内部按顺序判断，避免规则互相打架。

### 14.8 表设计映射

对应：

- `approval_decision`

建议保存：

- `decision_result`
- `approved_amount`
- `interest_rate`
- `loan_term`
- `reject_reason_code`
- `decision_explanation`
- `policy_references_json`
- `decided_by`
- `decided_at`

### 14.9 后续升级方向

- 引入产品差异化策略
- 引入规则引擎
- 引入 LLM 生成解释文本

---

## 15. 功能十：审批编排服务

### 15.1 功能目标

审批编排服务负责把多个 Agent 串成一条有顺序、有状态、有失败处理的完整流程。

### 15.2 当前骨架的实现方式

当前 `AgentOrchestrationServiceImpl` 是同步串行调用：

1. `documentIntakeAgent.collectAndParse`
2. `fraudRiskAgent.evaluate`
3. `repaymentCapacityAgent.evaluate`
4. `complianceDecisionAgent.decide`

这个顺序是合理的，可以保留。

### 15.3 第一版推荐实现方法

在这个服务里重点补三件事：

1. 统一状态推进
2. 统一异常捕获
3. 统一日志记录

建议结构：

1. 启动前更新状态为 `OCR_PARSING`
2. 信息采集完成后更新状态为 `RISK_ANALYZING`
3. 风控和偿债分析完成后更新状态为 `DECISION_PENDING`
4. 决策完成后更新为 `APPROVED`、`REJECTED` 或 `MANUAL_REVIEW`

### 15.4 异常处理建议

不要让某个 Agent 抛异常后把接口线程直接打挂。

建议：

1. 对每个 Agent 单独做 `try-catch`
2. 记录失败日志
3. 可恢复异常转 `MANUAL_REVIEW`
4. 不可恢复异常抛业务异常

### 15.5 后续升级方向

- 改成异步任务编排
- 接 MQ
- 接状态机
- 支持失败重试和补偿

---

## 16. 功能十一：人工复核

### 16.1 功能目标

当自动审批无法可靠完成时，系统需要把申请转给人工审核员处理。

### 16.2 涉及功能

管理端建议至少做四个功能：

1. 查询待复核列表
2. 查询复核详情
3. 人工复核通过
4. 人工复核拒绝

### 16.3 触发人工复核的典型场景

- 材料识别不完整
- 风险分中高但不满足直接拒绝
- 黑名单命中存在歧义
- 大模型输出不稳定
- 第三方工具调用失败

### 16.4 工单表设计

建议使用：

- `manual_review_ticket`

关键字段：

- `ticket_no`
- `application_id`
- `review_status`
- `assigned_to`
- `trigger_reason`
- `risk_summary`
- `review_comment`
- `reviewed_at`

### 16.5 人工复核通过实现方法

`ManualReviewService#approve` 建议按下面步骤实现：

1. 查询申请单
2. 校验当前状态必须是 `MANUAL_REVIEW`
3. 查询或创建人工复核工单
4. 写入复核意见
5. 写入最终审批结果到 `approval_decision`
6. 更新工单状态为已通过
7. 更新申请状态为 `APPROVED`
8. 写状态流转日志

### 16.6 人工复核拒绝实现方法

`ManualReviewService#reject` 建议按下面步骤实现：

1. 校验申请状态
2. 保存拒贷原因码与复核意见
3. 更新 `approval_decision`
4. 更新工单状态
5. 更新申请状态为 `REJECTED`
6. 写状态流转日志

### 16.7 第一版建议

第一版可以不做复杂派单系统，只做：

- 待复核列表查询
- 固定审核员字段
- 简单复核意见保存

### 16.8 后续升级方向

- 自动派单
- 审核员工作台
- SLA 超时提醒
- 多级复核

---

## 17. 功能十二：审批报告生成

### 17.1 功能目标

审批完成后，系统要能输出一份可查看的审批报告，既方便业务复盘，也方便项目演示。

### 17.2 报告建议内容

建议至少包含：

- 申请单基本信息
- 风险标签
- 偿债能力结果
- 最终审批结论
- 决策解释
- 政策引用

### 17.3 第一版实现方法

第一版不必直接生成 PDF。

可以先做：

1. 返回一个 JSON 报告结构
2. 落一条 `approval_report`
3. `report_url` 先写成假地址或本地路径

### 17.4 第二版实现方法

升级为：

1. 用模板引擎拼 HTML
2. HTML 转 PDF
3. 文件上传到 MinIO
4. 返回下载地址

### 17.5 涉及表

- `approval_report`

### 17.6 后续升级方向

- 增加报告版本号
- 增加复核版报告
- 增加报告水印和脱敏

---

## 18. 功能十三：数据库接入

### 18.1 阶段目标

把当前项目从内存演示版升级成真实后端版。

### 18.2 推荐先接的表

优先接这 4 张：

1. `loan_application`
2. `approval_decision`
3. `manual_review_ticket`
4. `state_transition_log`

第二批再接：

- `uploaded_document`
- `fraud_risk_result`
- `repayment_capacity_result`
- `agent_task_log`
- `tool_call_log`

### 18.3 推荐技术方案

如果目标是 Java 后端求职，建议优先：

- `Spring Boot + MyBatis`

推荐目录：

- `mapper`
- `mapper/xml`
- `service/impl`
- `domain/entity`
- `dto`

### 18.4 具体实现方法

按下面顺序最稳：

1. 配置 `application.yml` 数据库连接
2. 写实体和表字段映射
3. 写 `LoanApplicationMapper`
4. 写 `ApprovalDecisionMapper`
5. 写 `StateTransitionLogMapper`
6. 用数据库替换内存 `STORE`
7. 给关键事务加 `@Transactional`

### 18.5 关键注意点

- Java 字段名和 SQL 字段名统一
- 枚举落库统一用 `VARCHAR`
- 时间字段统一时区和格式
- 所有状态变化都写日志

---

## 19. 功能十四：工具层设计

### 19.1 为什么一定要单独做工具层

因为 Agent 不应该直接写第三方调用细节。

正确结构应该是：

- Agent 负责决策和整理
- Tool 负责外部能力调用

这样后续替换成本最低。

### 19.2 推荐工具清单

建议至少抽出这些工具：

1. `OcrParseTool`
2. `BlacklistCheckTool`
3. `CreditSummaryTool`
4. `IncomeAnalysisTool`
5. `DtiCalculationTool`
6. `PolicyRetrieveTool`

### 19.3 第一版实现方法

先写本地模拟工具，不接第三方。

例如：

- OCR 工具固定返回 JSON
- 黑名单工具根据身份证尾号判断
- 收入分析工具根据流水字段算月均收入
- DTI 工具直接返回计算结果

### 19.4 第二版实现方法

升级为：

- 真实 HTTP 调用
- 超时控制
- 重试
- 失败降级
- 调用日志

### 19.5 工具调用日志建议

对应表：

- `tool_call_log`

每次调用至少记录：

- `tool_name`
- `request_payload`
- `response_payload`
- `call_status`
- `retry_count`
- `duration_ms`

---

## 20. 功能十五：LangChain4j 接入

### 20.1 阶段目标

把项目升级成真正的 Agent 项目，但要控制边界，不要让大模型直接接管全部决策。

### 20.2 第一批建议让 LLM 做什么

建议只做两件事：

1. 风险摘要生成
2. 决策解释生成

不要第一步就让 LLM 输出最终通过或拒绝结论。

### 20.3 实现方法

建议接入顺序：

1. 配置 `LangChain4jConfig`
2. 跑通一次简单模型调用
3. 给 `FraudRiskAgent` 生成风险摘要
4. 给 `ComplianceDecisionAgent` 生成解释文本

### 20.4 Prompt 设计原则

每个 Agent 的 Prompt 要限制边界：

- 只能基于输入字段和工具结果输出
- 缺信息时返回固定标记
- 输出固定 JSON
- 禁止自由发挥审批结论

### 20.5 轨迹日志建议

对应表：

- `llm_trace_log`

记录：

- `model_name`
- `prompt_summary`
- `response_summary`
- `input_tokens`
- `output_tokens`
- `duration_ms`

---

## 21. 功能十六：RAG 知识库接入

### 21.1 阶段目标

让审批解释和合规判断有“政策依据”，避免模型无依据发挥。

### 21.2 第一版建议

第一版不要急着上重型向量库，先做轻量版：

1. 准备几份信贷政策文本
2. 进行条款切分
3. 做本地关键词检索
4. 把命中的条款传给合规决策 Agent

### 21.3 第二版建议

主流程稳定后，再升级成：

- PGVector
- Elasticsearch
- Milvus

### 21.4 建议实现方法

第一版可以用：

- `policy_id`
- `clause_id`
- `policy_title`
- `clause_text`

做一个简单的本地条款数据集。

查询时流程：

1. 根据产品类型和风险标签提取关键词
2. 检索相关条款
3. 返回前 N 条结果
4. 决策 Agent 基于命中条款生成解释

### 21.5 命中记录建议

对应表：

- `policy_hit_record`

### 21.6 关键注意点

- 不要一次喂给模型太多条款
- 检索不到条款时必须有兜底文本
- 条款引用要带 `policy_id` 或 `clause_id`

---

## 22. 功能十七：状态机、日志和异常处理

### 22.1 阶段目标

这一阶段要把项目从“能跑”升级到“像业务系统”。

### 22.2 状态机建议

第一版不必上框架，可用：

- 状态枚举
- 状态校验方法
- 状态日志表

后续再升级成：

- `Spring StateMachine`

### 22.3 日志建议至少分三类

1. 状态流转日志
2. Agent 任务日志
3. 工具调用日志

### 22.4 Agent 日志实现方法

对应：

- `agent_task_log`

每个 Agent 执行前后记录：

- `agent_name`
- `task_name`
- `input_summary`
- `output_summary`
- `status`
- `duration_ms`
- `error_message`

### 22.5 异常处理建议

建议统一处理：

- 参数错误
- 状态非法
- 工具调用失败
- 第三方接口超时
- OCR 解析失败

做法：

1. 自定义 `BusinessException`
2. 统一 `GlobalExceptionHandler`
3. 统一错误码
4. 可恢复异常转人工复核

---

## 23. 功能十八：工程化增强

### 23.1 这一阶段做什么

当主流程稳定后，再补这些能力：

- Redis 缓存
- MQ 异步任务
- 重试与熔断
- 敏感信息脱敏
- 监控埋点
- 报表导出

### 23.2 推荐顺序

1. 先做审计日志
2. 再做脱敏
3. 再做 Redis
4. 再做 MQ
5. 最后做监控和报表

### 23.3 推荐实现方法

#### Redis

适合缓存：

- 政策条款热数据
- 申请进度查询结果
- 黑名单热点数据

#### MQ

适合异步化：

- OCR 解析
- 审批报告生成
- 大模型摘要生成
- 风险复评任务

#### 脱敏

建议对这些字段做统一脱敏：

- 姓名
- 身份证号
- 手机号
- 银行卡号

#### 监控

建议埋点：

- 接口耗时
- Agent 耗时
- 工具调用成功率
- 审批通过率
- 人工复核占比

---

## 24. 推荐编码顺序

下面是一套最适合自己按天推进的顺序。

### 第 1 天

- 补全实体字段
- 完成创建申请接口
- 完成查询申请详情接口

### 第 2 天

- 增加状态推进方法
- 完成查询审批进度接口
- 增加状态流转日志内存版

### 第 3 天

- 接 MySQL
- 写 `LoanApplicationMapper`
- 把申请主表落库

### 第 4 天

- 写 `ApprovalDecisionMapper`
- 写 `StateTransitionLogMapper`
- 替换内存存储

### 第 5 天

- 写信息采集 Agent
- 写本地模拟 OCR 工具

### 第 6 天

- 写反欺诈 Agent
- 写黑名单与规则模拟工具

### 第 7 天

- 写偿债能力 Agent
- 写 DTI 和月供计算工具

### 第 8 天

- 写合规决策 Agent
- 输出固定审批结论和解释

### 第 9 天

- 写人工复核接口
- 写复核通过和拒绝逻辑

### 第 10 天

- 接 LangChain4j
- 让 LLM 生成风险摘要

### 第 11 天

- 接简单 RAG
- 基于政策条款生成解释

### 第 12 天

- 写审批报告生成
- 补日志和异常处理

---

## 25. 每一阶段都要自查的问题

### 25.1 主流程是否真的通了

- 能不能创建申请
- 能不能查询详情
- 能不能查询进度
- 能不能拿到审批结果

### 25.2 当前实现是不是可替换

- 内存存储能不能替换成数据库
- 模拟工具能不能替换成真实接口
- 规则方法能不能替换成规则引擎

### 25.3 这一层是不是方便面试表达

- 这块职责能不能讲清楚
- 为什么这么拆层
- 为什么要做人工复核
- 为什么不能让 LLM 直接决定通过或拒绝

---

## 26. 最终建议

你后面完善这个项目时，不要追求“代码一下子全写完”，而要追求：

- 每一步都能运行
- 每一步都能解释
- 每一步都能继续扩展
- 每一步都能体现工程思路

最稳的路线就是：

`先跑通 -> 再结构化 -> 再持久化 -> 再智能化 -> 再工程化 -> 再包装面试`

如果你按这个顺序写，这个项目会同时具备三种价值：

1. 能作为完整 Java 后端项目继续开发
2. 能作为 Agent 项目讲多 Agent、RAG、工具调用和人工接管
3. 能作为简历项目讲业务流程、系统设计、工程治理和风控思路

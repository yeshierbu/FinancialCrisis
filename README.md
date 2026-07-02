# 智能信贷审批系统（Financial Crisis）

基于 `Spring Boot + Vue 3 + LangChain4j` 的智能信贷审批演示项目。项目围绕“线上贷款申请 -> 材料提交 -> Agent 自动审批 -> 状态追踪 -> 人工复核/审批报告”的主流程搭建，适合用于智能信贷、风控审批、多 Agent 编排、审计留痕等场景的课程设计、项目展示或二次开发。

> 当前版本重点是跑通端到端业务链路：前端提交申请和材料元数据，后端使用本地规则与模拟 OCR 完成自动审批编排。数据库、真实文件存储、真实 OCR、征信/反欺诈外部接口和正式鉴权仍属于后续扩展方向。

## 项目功能

### 用户端功能

- 贷款申请提交：填写申请人姓名、身份证号、手机号、贷款产品、申请金额、贷款期限、就业类型、公司名称、工作年限等信息。
- 申请材料提交：选择身份证正面、身份证反面、银行流水等材料，前端向后端提交文件名、文件大小、文件类型、模拟文件地址等元数据。
- 审批状态查询：查看申请编号、申请人、产品、金额、期限、当前状态、当前步骤和状态时间线。
- 申请记录列表：支持按申请编号、申请人、产品关键字搜索，并按状态筛选申请记录。
- 审批报告查看：申请通过或拒绝后，可查询后端生成的模拟审批报告地址。
- 使用指南、演示登录和个人中心：用于完善前端演示体验，其中登录功能当前为纯前端演示，不调用后端鉴权接口。

### 后端审批能力

- 申请单管理：创建、查询、列表、状态查询。
- 材料管理：接收材料元数据，校验文件类型和大小，驱动审批流程继续执行。
- Agent 编排：按顺序执行材料采集、反欺诈风控、偿债能力测算、合规决策等 Agent。
- 补件判断：材料不完整时进入 `DOCUMENT_PENDING` 状态，等待用户补充材料。
- 自动审批决策：根据风险规则、DTI、推荐额度等指标输出通过、拒绝或人工复核。
- 人工复核：支持管理端查询待复核工单，并执行人工通过或拒绝。
- 审计时间线：聚合状态流转日志、Agent 执行日志、工具调用日志、政策命中记录，形成可回放的审批过程。
- 审批报告：审批结束后生成模拟报告记录和 `memory://` 报告地址。

## 审批流程

```mermaid
flowchart TD
    A[用户提交贷款申请] --> B[创建申请单]
    B --> C[DocumentIntakeAgent 材料完整性校验与模拟 OCR]
    C -->|材料缺失| D[DOCUMENT_PENDING 等待补件]
    D --> E[用户补充/上传材料]
    E --> C
    C -->|材料齐全| F[FraudRiskAgent 反欺诈风险评估]
    F --> G[RepaymentCapacityAgent 偿债能力测算]
    G --> H[ComplianceDecisionAgent 合规决策]
    H -->|自动通过| I[APPROVED 审批通过]
    H -->|自动拒绝| J[REJECTED 审批拒绝]
    H -->|边界风险| K[MANUAL_REVIEW 人工复核]
    K --> L[审核员人工通过/拒绝]
```

## 技术栈

### 后端

| 技术 | 说明 |
| --- | --- |
| Java 17 | 后端开发语言 |
| Spring Boot 3.3.1 | Web 服务、配置管理、应用启动 |
| Spring Web | REST API 接口 |
| Spring Validation | 请求参数校验 |
| LangChain4j 0.35.0 | 大模型/Agent 能力扩展基础配置 |
| MyBatis Spring Boot Starter 3.0.3 | Mapper 层预留，便于后续切换数据库持久化 |
| H2 Database | 内存数据库依赖与开发配置 |
| Lombok | 简化实体、DTO 样板代码 |
| Spring Boot Actuator | 健康检查与基础监控端点 |
| Maven | 后端依赖管理和构建工具 |

### 前端

| 技术 | 说明 |
| --- | --- |
| Vue 3.5 | 前端 UI 框架 |
| Vite 6 | 前端开发服务器和构建工具 |
| Tailwind CSS 3 | 样式与页面布局 |
| lucide-vue-next | 图标组件库 |
| Fetch API | 调用后端 REST 接口 |

## 目录结构

```text
FinancialCrisis
├── pom.xml                         # 后端 Maven 配置
├── src
│   ├── main
│   │   ├── java/com/erbu/financialcrisis
│   │   │   ├── agent               # 审批 Agent：材料、风控、偿债、合规决策
│   │   │   ├── common              # 统一响应、业务异常
│   │   │   ├── config              # 全局异常处理、LangChain4j 配置
│   │   │   ├── controller          # 用户端和管理端 REST API
│   │   │   ├── domain              # 实体与枚举
│   │   │   ├── dto                 # 请求/响应 DTO
│   │   │   ├── mapper              # MyBatis Mapper 预留层
│   │   │   ├── service             # 业务接口与实现
│   │   │   └── store               # 当前版本的内存数据中心
│   │   └── resources
│   │       └── application.yml     # 后端端口、数据源、LLM 配置
│   └── test                        # 后端测试
└── frontend
    ├── package.json                # 前端依赖与脚本
    ├── vite.config.js              # Vite 配置与 /api 代理
    └── src
        ├── components              # 页面组件
        ├── services/api.js         # 后端接口封装
        ├── App.vue                 # 单页应用入口
        └── style.css               # Tailwind 与全局样式
```

## 快速启动

### 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- npm 9+

### 启动后端

在项目根目录执行：

```bash
mvn spring-boot:run
```

后端默认启动在：

```text
http://localhost:8080
```

健康检查端点：

```text
http://localhost:8080/actuator/health
```

### 启动前端

进入前端目录并安装依赖：

```bash
cd frontend
npm install
npm run dev
```

前端默认启动在：

```text
http://localhost:5173
```

开发环境下，`frontend/vite.config.js` 会把 `/api` 请求代理到 `http://localhost:8080`。

## 配置说明

后端配置文件位于 `src/main/resources/application.yml`。

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:mem:financial_crisis;MODE=MySQL;DATABASE_TO_LOWER=TRUE

llm:
  api-key: ${DEEPSEEK_API_KEY:demo-key}
  base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
  model: ${OPENAI_MODEL:gpt-4o-mini}
```

可通过环境变量覆盖大模型配置：

| 环境变量 | 说明 |
| --- | --- |
| `DEEPSEEK_API_KEY` | LLM API Key，默认 `demo-key` |
| `OPENAI_BASE_URL` | OpenAI 兼容接口地址，默认 `https://api.openai.com/v1` |
| `OPENAI_MODEL` | 模型名称，默认 `gpt-4o-mini` |

当前业务 Agent 主要使用本地规则与模拟数据，`LangChain4jConfig` 提供的是后续接入真实大模型能力的基础 Bean。

## API 概览

### 用户端接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/loan/applications` | 查询申请列表 |
| `POST` | `/api/loan/applications` | 创建贷款申请 |
| `GET` | `/api/loan/applications/{applicationId}` | 查询申请详情 |
| `GET` | `/api/loan/applications/{applicationId}/status` | 查询申请状态和状态时间线 |
| `POST` | `/api/loan/applications/{applicationId}/documents` | 上传材料元数据 |
| `POST` | `/api/loan/applications/{applicationId}/supplement` | 提交补充材料 |
| `GET` | `/api/loan/applications/{applicationId}/report` | 查询审批报告 |

### 管理端接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/admin/reviews/pending` | 查询待人工复核工单 |
| `GET` | `/api/admin/reviews/{applicationId}` | 查询人工复核详情 |
| `POST` | `/api/admin/reviews/{applicationId}/approve` | 人工复核通过 |
| `POST` | `/api/admin/reviews/{applicationId}/reject` | 人工复核拒绝 |
| `GET` | `/api/admin/audit/{applicationId}/timeline` | 查询审计时间线 |

## 核心状态说明

| 状态 | 含义 |
| --- | --- |
| `SUBMITTED` | 申请已提交 |
| `DOCUMENT_PENDING` / `MATERIAL_PENDING` | 等待补充材料 |
| `OCR_PARSING` | 材料解析中 |
| `RISK_ANALYZING` | 风险分析中 |
| `DECISION_PENDING` / `DECISIONING` | 审批决策中 |
| `MANUAL_REVIEW` | 进入人工复核 |
| `APPROVED` | 审批通过 |
| `REJECTED` | 审批拒绝 |
| `ARCHIVED` | 已归档 |

## 当前版本说明

- 当前后端核心数据保存在 `InMemoryApprovalStore` 中，应用重启后数据会清空。
- 项目中已经存在实体、枚举和 Mapper 层，为后续迁移到 MySQL/MyBatis 持久化做了结构预留。
- 文件上传当前只提交材料元数据，不上传真实文件内容。
- OCR 当前为本地模拟逻辑，材料齐全后会把文档标记为解析成功。
- 前端登录和个人中心为演示功能，尚未接入真实用户体系。
- 审批报告当前生成 `memory://approval-reports/...` 形式的模拟地址，尚未导出真实 PDF。

## 后续扩展方向

- 接入 MySQL，替换内存仓库为真实持久化存储。
- 增加数据库建表脚本、初始化数据和事务控制。
- 接入真实文件上传、对象存储和 OCR 服务。
- 接入征信、黑名单、多头借贷、设备指纹等外部风控数据源。
- 将规则逻辑抽象为规则引擎或可配置策略。
- 引入真实 LLM/RAG，用于政策条款检索、审批解释生成和人工复核辅助。
- 增加用户认证、角色权限和管理后台页面。
- 完善单元测试、接口测试和前端构建流水线。

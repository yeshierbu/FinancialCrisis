# 智能信贷审批系统（Financial Crisis）

一个使用 Java 实现的多 Agent 智能信贷审批项目。系统覆盖贷款申请、材料上传、OCR、风险分析、偿债能力评估、政策知识检索、LLM 独立复核、审批决策、确定性安全护栏以及人工审核等完整流程。

后端使用 Spring Boot 3，业务数据存储在 MySQL，政策文件通过 `text-embedding-v4` 向量化后写入 Qdrant。前端使用 Vue 3，提供客户申请端和管理审核端。

> 当前项目适合本地开发、课程设计和多 Agent 架构演示。登录功能目前是前端演示登录，不应直接用于生产环境。

## 1. 核心功能

### 客户端

- 使用演示客户账号登录。
- 创建贷款申请。
- 上传身份证、银行流水、收入证明、征信报告等材料。
- 调用百度千帆 DeepSeek-OCR 识别材料内容。
- 查询申请状态和最终审批结果。
- 查看审批报告。

### 管理端

- 查看审批总览和最终审批结果。
- 只展示已经通过、拒绝或归档的最终结果，不展示中间状态变化。
- 查看待人工审核队列。
- 点击“查看详情”查看申请信息、风险数据和偿债能力数据。
- 对人工审核案件执行通过或拒绝。
- 填写批准金额、利率、期限、拒绝原因和审核意见。
- 上传 PDF、DOCX、TXT、MD 政策文件。
- 自动解析、切片、向量化并同步至 Qdrant。
- 查看政策文件的向量同步状态。

### 多 Agent 审批

- 材料完整性检查与 OCR 结果校验。
- 反欺诈风险评估。
- DTI、收入稳定性和建议额度计算。
- LLM 综合风险分析。
- 独立 LLM 复核，发现冲突时最多返工一次。
- LLM 生成结构化审批建议。
- `PolicyGuard` 使用确定性规则进行最终安全校验。
- Agent、工具或外部模型发生异常时转入人工审核，不会降级为自动通过。

## 2. 系统架构

```text
Vue 3 客户端 / 管理端
          |
          | HTTP / JSON / Multipart
          v
Spring Boot REST API
          |
          +-- 贷款申请、材料、审核、报告服务
          |
          +-- 多 Agent 审批编排
          |     |
          |     +-- DocumentIntakeAgent：材料检查与 OCR 结果整理
          |     +-- FraudRiskAgent：确定性反欺诈评估
          |     +-- RepaymentCapacityAgent：偿债能力计算
          |     +-- RiskWorker：LLM 风险分析与政策检索
          |     +-- ReviewWorker：独立 LLM 复核
          |     +-- DecisionWorker：LLM 审批建议
          |     +-- PolicyGuard：确定性安全护栏
          |
          +-- MySQL：申请、风险结果、决策、人工审核、审计日志
          |
          +-- Qdrant：政策分片向量和检索 Payload
          |
          +-- DeepSeek：三个 LLM Worker
          +-- DashScope：text-embedding-v4
          +-- 百度千帆：DeepSeek-OCR
```

## 3. 多 Agent 如何协作

`AgentOrchestrationServiceImpl` 是审批编排入口。每个 Agent 不直接共享完整对话历史，而是通过结构化案件上下文和结构化工件协作。

主要共享对象如下：

| 对象 | 作用 |
| --- | --- |
| `ApprovalCaseContext` | 保存当前申请 ID 和各 Agent 的结构化发现 |
| `AgentFinding` | 记录 Agent 名称、结论、置信度、证据和建议动作 |
| `RiskReport` | `RiskWorker` 输出的综合风险报告 |
| `ReviewReport` | `ReviewWorker` 输出的复核报告和返工要求 |
| `DecisionProposal` | `DecisionWorker` 输出的审批建议 |

执行顺序：

```text
申请与材料
   |
   v
DocumentIntakeAgent
   |
   +---- 材料缺失 ----------> 等待补件
   |
   v
FraudRiskAgent + RepaymentCapacityAgent
   |
   v
RiskWorker（LLM + Qdrant 政策检索）
   |
   v
ReviewWorker（独立 LLM 复核）
   |
   +---- 不接受 ----> RiskWorker 定向返工一次 ----> 再次复核
   |
   v
DecisionWorker（LLM 审批建议）
   |
   v
PolicyGuard（确定性校验）
   |
   +---- 通过 / 拒绝
   +---- 规则边界、证据不足或异常 ----> 人工审核
```

LLM 负责理解政策、分析风险、复核证据和生成建议；确定性规则负责不可被模型绕过的底线约束。这样可以保留 LLM 的推理能力，同时避免金融审批完全依赖生成式结果。

## 4. MySQL 与 Qdrant 的职责

### MySQL

MySQL 是业务事实库，保存：

- 系统角色和演示账号。
- 风险黑名单的精确哈希值。
- 政策文档元数据和同步状态。
- 贷款申请和申请人画像。
- 上传材料与 OCR 状态。
- 反欺诈、偿债能力和审批结果。
- 人工审核工单。
- Agent 运行、工具调用、政策命中和状态审计日志。

精确黑名单不能使用向量相似度判断。身份证、手机号、银行卡等精确标识应经过规范化和哈希后在 MySQL 中进行等值查询。

### Qdrant

Qdrant 只保存用于语义检索的政策分片：

- Embedding 向量。
- 政策编号和版本。
- 分片编号、标题、章节和正文。
- 产品编码。
- 生效日期、失效日期和政策状态。

检索时先进行向量召回，再使用产品、状态和日期进行精确过滤。Qdrant 不代替 MySQL 保存交易事实。

## 5. 技术栈

### 后端

| 技术 | 版本/用途 |
| --- | --- |
| Java | 17 |
| Spring Boot | 3.3.1 |
| Spring Web | REST API 和文件上传 |
| Spring Validation | 请求参数校验 |
| MyBatis | 3.0.3，MySQL 数据访问 |
| MySQL | 8.x，业务数据 |
| LangChain4j | 0.35.0，OpenAI 兼容 LLM 接入 |
| Apache PDFBox | 2.0.31，PDF 文本提取 |
| Apache POI | 5.3.0，DOCX 文本提取 |
| Qdrant | 政策向量数据库 |
| H2 | 自动化测试数据库 |
| Maven | 构建和测试 |

### 前端

| 技术 | 版本/用途 |
| --- | --- |
| Vue | 3.5 |
| Vite | 6 |
| Tailwind CSS | 3.4 |
| lucide-vue-next | 页面图标 |
| Fetch API | 调用后端接口 |

## 6. 项目目录

```text
FinancialCrisis/
├── README.md
├── pom.xml
├── docker-compose.yml                 # Qdrant 容器
├── 智能信贷审批Agent-数据库建表.sql      # MySQL 8 初始化脚本（23 张表）
├── src/
│   ├── main/
│   │   ├── java/com/erbu/financialcrisis/
│   │   │   ├── agent/
│   │   │   │   ├── artifact/          # Agent 结构化输出
│   │   │   │   ├── collaboration/     # 共享案件上下文
│   │   │   │   ├── guard/             # 确定性审批护栏
│   │   │   │   ├── runtime/           # 结构化 LLM 客户端
│   │   │   │   ├── tool/              # 政策检索工具
│   │   │   │   └── worker/            # 三个 LLM Worker
│   │   │   ├── controller/             # REST API
│   │   │   ├── domain/                 # 实体和枚举
│   │   │   ├── dto/                    # 请求和响应对象
│   │   │   ├── knowledge/              # Embedding 与 Qdrant
│   │   │   ├── mapper/                 # MyBatis Mapper 接口
│   │   │   ├── service/                # 业务服务
│   │   │   └── store/                  # 数据持久化门面
│   │   └── resources/
│   │       ├── application.yml
│   │       └── mapper/                 # MyBatis XML
│   └── test/                            # H2 集成测试和单元测试
└── frontend/
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── components/
        ├── services/
        ├── App.vue
        └── style.css
```

## 7. 环境要求

建议安装：

- JDK 17
- Maven 3.8+
- Node.js 18+
- npm 9+
- MySQL 8+
- Docker Desktop

检查版本：

```bash
java -version
mvn -version
node -v
npm -v
docker --version
```

## 8. 初始化 MySQL

项目默认连接：

```text
jdbc:mysql://localhost:3306/financial_crisis
```

### 8.1 安装了 MySQL 命令行客户端

```bash
mysql -u root -p < 智能信贷审批Agent-数据库建表.sql
```

或者进入 MySQL 后执行：

```sql
SOURCE /Users/你的用户名/IdeaProjects/FinancialCrisis/智能信贷审批Agent-数据库建表.sql;
```

### 8.2 MySQL 运行在 Docker 中

先查看容器名：

```bash
docker ps
```

然后执行：

```bash
docker exec -i 你的MySQL容器名 mysql -u root -p你的密码 < 智能信贷审批Agent-数据库建表.sql
```

### 8.3 验证数据库

```sql
USE financial_crisis;
SHOW TABLES;
SELECT COUNT(*) FROM information_schema.tables
WHERE table_schema = 'financial_crisis';
```

初始化脚本当前创建 23 张表，并初始化 `ADMIN`、`USER`、`REVIEWER` 三种角色及演示账号。

## 9. 启动 Qdrant

项目根目录已经包含 `docker-compose.yml`：

```bash
docker compose up -d
```

检查容器：

```bash
docker compose ps
```

检查服务：

```bash
curl http://localhost:6333/collections
```

Qdrant 端口：

| 端口 | 用途 |
| --- | --- |
| `6333` | HTTP API，项目默认使用该端口 |
| `6334` | gRPC API |

停止容器但保留数据：

```bash
docker compose stop
```

重新启动：

```bash
docker compose start
```

## 10. 配置环境变量

Spring Boot 会自动加载项目根目录下的 `.env`：

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
```

在项目根目录创建 `.env`，每行使用 `KEY=value`，不要写 `export`，等号两边不要留空格：

```properties
# MySQL
DB_URL=jdbc:mysql://localhost:3306/financial_crisis?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME=root
DB_PASSWORD=你的MySQL密码

# DeepSeek LLM
LLM_ENABLED=true
DEEPSEEK_API_KEY=你的DeepSeekKey
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-v4-flash
LLM_TIMEOUT_SECONDS=45

# 百度千帆 DeepSeek-OCR
BAIDU_API_KEY=你的百度千帆APIKey
BAIDU_OCR_BASE_URL=https://qianfan.baidubce.com/v2
BAIDU_OCR_MODEL=deepseek-ocr
BAIDU_OCR_TIMEOUT_SECONDS=60
BAIDU_OCR_FALLBACK_TO_MOCK=true

# DashScope Embedding + Qdrant
KNOWLEDGE_ENABLED=true
EMBEDDING_API_KEY=你的DashScopeKey
EMBEDDING_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
EMBEDDING_MODEL=text-embedding-v4
QDRANT_URL=http://localhost:6333
QDRANT_POLICY_COLLECTION=credit_policy_chunks_v4
```

配置说明：

| 环境变量 | 必需 | 说明 |
| --- | --- | --- |
| `DB_URL` | 是 | MySQL JDBC 地址 |
| `DB_USERNAME` | 是 | MySQL 用户名 |
| `DB_PASSWORD` | 是 | MySQL 密码 |
| `LLM_ENABLED` | 建议 | 是否启用三个 LLM Worker |
| `DEEPSEEK_API_KEY` | 启用 LLM 时 | DeepSeek API Key |
| `DEEPSEEK_BASE_URL` | 否 | 默认 `https://api.deepseek.com` |
| `DEEPSEEK_MODEL` | 否 | DeepSeek 模型名称 |
| `BAIDU_API_KEY` | 真实 OCR 时 | 百度千帆模型 API Key |
| `BAIDU_OCR_FALLBACK_TO_MOCK` | 否 | `true` 时 OCR 失败使用开发模拟结果 |
| `KNOWLEDGE_ENABLED` | 政策 RAG 时 | 是否启用 Embedding 和 Qdrant |
| `EMBEDDING_API_KEY` | 启用知识库时 | DashScope API Key |
| `EMBEDDING_MODEL` | 否 | 当前使用 `text-embedding-v4` |
| `QDRANT_URL` | 启用知识库时 | Qdrant HTTP 地址 |
| `QDRANT_POLICY_COLLECTION` | 否 | 政策 Collection 名称 |

`.env` 已经被 `.gitignore` 忽略。不要将真实 Key 写进 README、源码、提交记录或聊天截图。

如果 Key 曾经提交到 Git，删除本地文件并不能消除泄露，应立即在服务商后台废弃并重新生成。

## 11. 启动后端

在项目根目录运行：

```bash
mvn spring-boot:run
```

成功日志包含：

```text
Tomcat started on port 8080
Started FinancialCrisisApplication
```

健康检查：

```bash
curl http://localhost:8080/actuator/health
```

预期结果：

```json
{"status":"UP"}
```

也可以先构建再运行：

```bash
mvn clean package
java -jar target/financial-crisis-0.0.1-SNAPSHOT.jar
```

## 12. 启动前端

打开第二个终端：

```bash
cd frontend
npm ci
npm run dev
```

浏览器访问：

```text
http://localhost:5173
```

开发服务器会把 `/api` 请求代理到 `http://localhost:8080`。

生产构建：

```bash
cd frontend
npm run build
```

构建文件输出到 `frontend/dist`。

## 13. 演示账号

| 角色 | 账号 | 密码 | 页面 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `admin123` | 管理总览、政策上传、人工审核、最终结果 |
| 客户 | `user` | `user123` | 贷款申请、材料上传、进度查询 |

需要注意：当前页面登录账号定义在 `frontend/src/services/auth.js`，属于演示级前端认证。SQL 中虽然初始化了账号和角色表，但当前登录页尚未调用后端鉴权接口。

生产部署必须增加：

- Spring Security。
- 后端登录接口。
- BCrypt 或 Argon2 密码哈希。
- JWT 或服务端 Session。
- 管理接口角色鉴权。
- 数据权限隔离和敏感字段加密。

## 14. 使用流程

### 14.1 上传政策文件

1. 启动 MySQL、Qdrant、后端和前端。
2. 使用 `admin/admin123` 登录。
3. 在“政策知识库”区域选择政策文件。
4. 填写政策编号、版本、标题、产品和生效日期。
5. 点击“上传并同步知识库”。
6. 等待状态变为“已同步”。

支持格式：

- PDF
- DOCX
- TXT
- MD

上传限制为 10 MB。后端会：

1. 提取文件文本。
2. 计算 SHA-256 内容摘要。
3. 将文本按约 1000 字切片，相邻分片重叠约 150 字。
4. 调用 `text-embedding-v4` 生成向量。
5. 自动创建 Qdrant Collection。
6. 将向量和政策 Payload 写入 Qdrant。
7. 在 MySQL `policy_document` 中记录元数据和同步状态。

重复上传相同“政策编号 + 版本”时，会先删除旧版本向量再写入新分片，避免旧内容残留。

普通文字型 PDF 可以直接提取。纯扫描图片型 PDF 当前无法通过 PDFBox 提取正文，需要先做 OCR 或转换为可搜索 PDF。

### 14.2 创建贷款申请

1. 使用 `user/user123` 登录。
2. 填写申请人、身份证号、手机号、金额、期限和就业信息。
3. 提交贷款申请。
4. 上传系统要求的申请材料。
5. 材料满足要求后触发审批编排。

材料类型包括：

| 枚举值 | 说明 |
| --- | --- |
| `ID_CARD_FRONT` | 身份证正面 |
| `ID_CARD_BACK` | 身份证背面 |
| `BANK_STATEMENT` | 银行流水 |
| `INCOME_CERTIFICATE` | 收入证明 |
| `CREDIT_REPORT` | 征信报告 |
| `HOUSEHOLD_BOOK` | 户口簿 |

### 14.3 人工审核

以下情况会进入人工审核：

- 自动审批认为需要人工判断。
- LLM 返回异常或结构化结果不合法。
- OCR、Embedding、Qdrant 或其他工具失败。
- 复核后仍存在证据冲突。
- `PolicyGuard` 判定不能自动完成审批。

管理员操作：

1. 登录管理端。
2. 在“待人工复核”区域找到案件。
3. 点击“查看详情”。
4. 查看申请金额、期限、就业信息、脱敏证件信息、风险分数、收入、债务和建议额度。
5. 选择“审批通过”或“审批拒绝”。
6. 填写审核意见并提交。

人工审核完成后，案件从待审核队列移除，并出现在最终审批结果中。

## 15. 主要 API

统一响应格式：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 贷款申请

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/loan/applications` | 查询申请列表 |
| `POST` | `/api/loan/applications` | 创建贷款申请 |
| `GET` | `/api/loan/applications/{id}` | 查询申请详情 |
| `GET` | `/api/loan/applications/{id}/status` | 查询申请状态 |
| `GET` | `/api/loan/applications/{id}/report` | 查询审批报告 |

### 申请材料

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/loan/applications/{id}/documents` | Multipart 上传材料 |
| `POST` | `/api/loan/applications/{id}/supplement` | 提交补件信息 |

### 人工审核

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/admin/reviews/pending` | 查询待审核工单 |
| `GET` | `/api/admin/reviews/{id}` | 查询申请及风控详情 |
| `POST` | `/api/admin/reviews/{id}/approve` | 人工审批通过 |
| `POST` | `/api/admin/reviews/{id}/reject` | 人工审批拒绝 |

### 政策知识库

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/admin/policy-knowledge/documents` | 查询政策文档 |
| `POST` | `/api/admin/policy-knowledge/documents` | 上传并向量化政策文件 |
| `POST` | `/api/admin/policy-knowledge/chunks` | 手工写入单个政策分片 |

### 审计

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/admin/audit/{id}/timeline` | 后端审计接口；当前管理页面不展示状态变化过程 |

## 16. 测试

执行全部后端测试：

```bash
mvn test
```

测试环境使用 H2，不依赖本机 MySQL、Qdrant、DeepSeek 或百度千帆。

当前测试覆盖：

- Spring Boot 上下文和主要申请接口。
- OCR 成功与失败处理。
- OCR 失败阻止自动审批。
- `PolicyGuard` 安全规则。
- 政策文件切片与 Qdrant 写入调用。
- 不支持文件类型校验。

同时检查后端打包和前端构建：

```bash
mvn -DskipTests package
cd frontend
npm run build
```

测试中可能看到 `mock OCR failure` 的 ERROR 日志，这是验证“OCR 失败必须阻止自动审批”的预期失败场景。只要 Maven 最终显示测试失败数为 0，即表示测试通过。

## 17. 常见问题

### 17.1 `Port 8080 was already in use`

表示已有程序占用后端端口：

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
kill 进程PID
```

或者临时更换端口：

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

更换端口后还需要修改 `frontend/vite.config.js` 的代理目标。

### 17.2 `zsh: command not found: mysql`

这表示 MySQL 服务可能已经运行，但命令行客户端不在 PATH 中。可以使用 MySQL 安装目录下的完整路径，或者通过 Docker 容器执行 MySQL 命令。

### 17.3 MySQL 连接失败

检查：

```bash
nc -vz localhost 3306
```

然后确认 `.env` 中的数据库名、用户名和密码。项目数据库名是：

```text
financial_crisis
```

### 17.4 Qdrant 连接失败

```bash
docker compose ps
curl http://localhost:6333/collections
```

如果容器未启动：

```bash
docker compose up -d
```

### 17.5 政策上传提示知识库未启用

确认：

```properties
KNOWLEDGE_ENABLED=true
EMBEDDING_API_KEY=有效的Key
QDRANT_URL=http://localhost:6333
```

修改 `.env` 后需要完整重启 Spring Boot。

### 17.6 `invalid_iam_token` 或 OCR 返回 401

这表示百度千帆鉴权失败，不是 MySQL 或 Qdrant 问题。常见原因：

- `BAIDU_API_KEY` 无效或已经过期。
- 使用了百度云 Access Key，而不是千帆模型 API Key。
- IDEA 运行配置中的旧变量覆盖了 `.env`。
- 当前账号没有模型权限或可用额度。

开发阶段可以使用：

```properties
BAIDU_OCR_FALLBACK_TO_MOCK=true
```

此时 OCR 失败会返回明确标记的模拟结果，只能用于开发测试。

### 17.7 终端能启动，IDEA 不能启动

项目会读取根目录 `.env`，但 IDEA 的工作目录必须是项目根目录：

```text
/Users/你的用户名/IdeaProjects/FinancialCrisis
```

同时检查 IDEA Run Configuration 中是否配置了同名旧环境变量。系统环境变量通常会覆盖 `.env`。

### 17.8 `.env` 是否可以提交

不可以。项目 `.gitignore` 已忽略 `.env` 和 `.env.*`。建议保留一份不含真实密钥的 `.env.example` 作为配置说明；删除 `.env.example` 不会影响运行，但不利于其他开发者快速配置项目。

## 18. 当前限制与生产化建议

当前版本仍有以下演示性质的限制：

- 前端登录使用静态账号，没有真正的服务端认证。
- 管理 API 尚未通过 Spring Security 做角色鉴权。
- 部分确定性风控 Agent 使用演示计算逻辑。
- 扫描型 PDF 政策尚未接入 OCR。
- 文件元数据保存在数据库，未接入对象存储。
- 数据库结构通过 SQL 脚本管理，未引入 Flyway 或 Liquibase。
- 外部 API 调用尚未配置完整的限流、重试和熔断策略。

生产化建议：

1. 引入 Spring Security、JWT/Session 和 RBAC。
2. 使用 BCrypt/Argon2 存储密码。
3. 对身份证、手机号等敏感字段进行加密和脱敏审计。
4. 使用 Flyway 或 Liquibase 管理数据库版本。
5. 使用 MinIO、OSS 或 S3 保存原始文件。
6. 对 LLM、OCR、Embedding 和 Qdrant 增加超时、重试、熔断和监控。
7. 对 Prompt、模型、政策版本和决策结果建立可追溯版本号。
8. 增加人工审核双人复核和权限分离。
9. 为政策上传增加病毒扫描、格式校验和异步任务队列。
10. 增加覆盖真实 MySQL 和 Qdrant 的容器化集成测试。

## 19. 安全说明

- 不要提交 `.env`、API Key、数据库密码或生产客户数据。
- 不要在日志中输出完整身份证号、手机号、银行卡号或模型密钥。
- 管理端详情页面已经对身份证号和手机号进行前端脱敏，但生产环境还应在后端按角色控制字段。
- LLM 输出只能作为审批建议，最终必须经过确定性规则和必要的人工审核。
- 演示账号和默认密码仅用于本地开发，生产部署前必须删除。

## 20. 快速启动清单

```text
[ ] JDK、Maven、Node.js、MySQL、Docker 已安装
[ ] 已执行 MySQL 初始化 SQL
[ ] Qdrant 容器已启动
[ ] 项目根目录 .env 已正确配置
[ ] DEEPSEEK_API_KEY 已配置
[ ] EMBEDDING_API_KEY 已配置
[ ] KNOWLEDGE_ENABLED=true
[ ] Spring Boot 已在 8080 启动
[ ] Vue 前端已在 5173 启动
[ ] /actuator/health 返回 UP
[ ] /collections 可以访问 Qdrant
```

完成以上项目后，访问 `http://localhost:5173` 即可开始使用。

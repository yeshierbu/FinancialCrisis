# 智能信贷审批系统（Financial Crisis）

基于 `Spring Boot + Vue 3 + LangChain4j` 的智能信贷审批演示项目。项目围绕“线上贷款申请 -> 材料提交 -> Agent 自动审批 -> 状态追踪 -> 人工复核/审批报告”的主流程搭建，适合用于智能信贷、风控审批、多 Agent 编排、审计留痕等场景。

审批核心采用三个独立 LLM Worker：`RiskWorker` 进行综合风险分析并检索政策，
`ReviewWorker` 独立检查证据和冲突（最多触发一次返工），`DecisionWorker` 生成审批建议；
最后由确定性的 `PolicyGuard` 阻止黑名单、DTI、金额和证据约束被模型绕过。
业务事实保存在 MySQL，政策语义检索使用 Qdrant。

## 技术栈

### 后端

| 技术 | 说明 |
| --- | --- |
| Java 17 | 后端开发语言 |
| Spring Boot 3.3.1 | Web 服务、配置管理、应用启动 |
| Spring Web | REST API 接口 |
| Spring Validation | 请求参数校验 |
| LangChain4j 0.35.0 | 大模型/Agent 能力扩展基础配置 |
| MyBatis Spring Boot Starter 3.0.3 | 业务数据的 Mapper 持久化层 |
| MySQL 8 | 贷款申请、审批结果和审计日志的持久化数据库 |
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
│   │   │   ├── agent               # 三个 LLM Worker、结构化工件、工具与安全护栏
│   │   │   │   └── collaboration    # 共享案件上下文与结构化 Agent 发现
│   │   │   ├── common              # 统一响应、业务异常
│   │   │   ├── config              # 全局异常处理、LangChain4j 配置
│   │   │   ├── controller          # 用户端和管理端 REST API
│   │   │   ├── domain              # 实体与枚举
│   │   │   ├── dto                 # 请求/响应 DTO
│   │   │   ├── mapper              # MyBatis Mapper 接口
│   │   │   ├── service             # 业务接口与实现
│   │   │   └── store               # 数据库持久化门面
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

演示登录账号：

| 角色 | 账号 | 密码 | 登录后界面 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `admin123` | 审批管理总览、全量申请、人工复核 |
| 客户 | `user` | `user123` | 贷款申请、申请记录、审批进度 |

开发环境下，`frontend/vite.config.js` 会把 `/api` 请求代理到 `http://localhost:8080`。

可通过环境变量覆盖大模型配置：

| 环境变量 | 说明 |
| --- | --- |
| `BAIDU_API_KEY` | 百度千帆 API Key，用于 DeepSeek-OCR 鉴权 |
| `BAIDU_OCR_BASE_URL` | 千帆模型服务地址，默认 `https://qianfan.baidubce.com/v2` |
| `BAIDU_OCR_MODEL` | OCR 模型，默认且建议保持 `deepseek-ocr` |
| `BAIDU_OCR_TIMEOUT_SECONDS` | 单张图片 OCR 超时时间，默认 60 秒 |
| `DEEPSEEK_API_KEY` | DeepSeek API Key；严格模式下必须配置 |
| `LLM_ENABLED` | 是否启用 LLM 审查，默认 `true`；设为 `false` 时自动审批会转人工复核 |
| `DEEPSEEK_BASE_URL` | DeepSeek OpenAI 兼容接口地址，默认 `https://api.deepseek.com` |
| `DEEPSEEK_MODEL` | 模型名称，默认 `deepseek-v4-flash` |
| `LLM_TIMEOUT_SECONDS` | 单次 LLM 调用超时时间，默认 45 秒 |

`LangChain4jConfig` 负责创建 DeepSeek 兼容的 `ChatLanguageModel`。三个 Worker 分别使用独立提示词和结构化工件协作；只有模型返回合法 JSON，且最终建议通过 `PolicyGuard`，流程才会进入自动审批结果。

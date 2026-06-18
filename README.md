# Financial Crisis

这是一个基于 `Spring Boot + LangChain4j` 的智能信贷审批 Agent 项目骨架。

## 当前骨架包含

- `controller`：用户端与管理端接口入口
- `service`：申请服务、人工复核服务、Agent 编排服务
- `agent`：信息采集、反欺诈、偿债能力、合规决策四类 Agent
- `domain`：基础实体与状态枚举
- `common/config`：统一响应、异常处理、LangChain4j 基础配置

## 设计说明

1. 先用内存存储让主链路跑通，后续再替换为 MySQL + Repository。
2. Agent 现在是占位实现，方便后续逐步填入 OCR、征信、规则引擎和 RAG。
3. 注释重点放在“为什么这么分层”和“后续该往哪里扩”，方便项目继续演进。

## 下一步建议

1. 接入 MySQL 和 MyBatis/JPA。
2. 把 SQL 文档中的表结构真正落到项目中。
3. 为 Agent 补充工具调用、日志和状态机。
4. 增加文件上传、材料解析和审批报告接口。

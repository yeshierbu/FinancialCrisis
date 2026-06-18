# 智能信贷审批 Agent API 接口文档

## 1. 文档说明

本文档用于定义智能信贷审批系统对外提供的核心接口，覆盖用户端贷款申请流程、材料上传、审批进度查询，以及管理端人工复核、审计查看等能力。

基础约定：

- Base URL：`/api`
- 数据格式：`application/json`
- 文件上传：`multipart/form-data`
- 字符集：`UTF-8`
- 时间格式：`yyyy-MM-dd HH:mm:ss`

统一响应结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

错误码建议：

- `0`：成功
- `4001`：参数校验失败
- `4002`：申请单不存在
- `4003`：当前状态不允许该操作
- `4004`：文件格式不支持
- `5001`：外部征信接口调用失败
- `5002`：OCR 解析失败
- `5003`：系统内部异常

## 2. 用户端接口

### 2.1 创建贷款申请

- 方法：`POST`
- 路径：`/api/loan/applications`

请求示例：

```json
{
  "productCode": "CONSUMER_LOAN_STD",
  "applicantName": "张三",
  "idCardNo": "310101199001011234",
  "mobile": "13800138000",
  "loanAmount": 80000,
  "loanTerm": 24,
  "employmentType": "FULL_TIME",
  "companyName": "上海某科技有限公司",
  "workYears": 3
}
```

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "applicationId": 100001,
    "status": "SUBMITTED",
    "createdAt": "2026-06-17 17:00:00"
  }
}
```

### 2.2 上传申请材料

- 方法：`POST`
- 路径：`/api/loan/applications/{applicationId}/documents`
- Content-Type：`multipart/form-data`

表单字段：

- `documentType`：证件类型，取值示例 `ID_CARD_FRONT`、`ID_CARD_BACK`、`BANK_STATEMENT`、`CREDIT_REPORT`
- `file`：上传文件

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "documentId": 200001,
    "documentType": "BANK_STATEMENT",
    "ocrStatus": "PENDING"
  }
}
```

### 2.3 提交补充资料

- 方法：`POST`
- 路径：`/api/loan/applications/{applicationId}/supplement`

请求示例：

```json
{
  "remark": "补充最近6个月工资流水",
  "documents": [
    {
      "documentType": "BANK_STATEMENT",
      "fileUrl": "minio://loan-docs/statement-100001.pdf"
    }
  ]
}
```

### 2.4 查询申请详情

- 方法：`GET`
- 路径：`/api/loan/applications/{applicationId}`

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "applicationId": 100001,
    "productCode": "CONSUMER_LOAN_STD",
    "applicantName": "张三",
    "loanAmount": 80000,
    "loanTerm": 24,
    "status": "RISK_ANALYZING",
    "currentStep": "反欺诈核验中"
  }
}
```

### 2.5 查询审批进度

- 方法：`GET`
- 路径：`/api/loan/applications/{applicationId}/status`

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "applicationId": 100001,
    "status": "MANUAL_REVIEW",
    "statusDesc": "人工复核中",
    "lastUpdatedAt": "2026-06-17 17:20:00",
    "timeline": [
      {
        "status": "SUBMITTED",
        "time": "2026-06-17 17:00:00"
      },
      {
        "status": "OCR_PARSING",
        "time": "2026-06-17 17:02:00"
      },
      {
        "status": "RISK_ANALYZING",
        "time": "2026-06-17 17:10:00"
      }
    ]
  }
}
```

### 2.6 获取审批报告

- 方法：`GET`
- 路径：`/api/loan/applications/{applicationId}/report`

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "reportId": 300001,
    "reportUrl": "minio://loan-report/approval-100001.pdf",
    "generatedAt": "2026-06-17 17:35:00"
  }
}
```

## 3. 管理端接口

### 3.1 查询待人工复核列表

- 方法：`GET`
- 路径：`/api/admin/reviews/pending`

请求参数：

- `pageNo`
- `pageSize`
- `riskLevel`
- `productCode`

### 3.2 查询人工复核详情

- 方法：`GET`
- 路径：`/api/admin/reviews/{ticketId}`

### 3.3 人工复核通过

- 方法：`POST`
- 路径：`/api/admin/reviews/{ticketId}/approve`

请求示例：

```json
{
  "approvedAmount": 60000,
  "loanTerm": 24,
  "interestRate": 10.8,
  "reviewComment": "材料完整，风险可控，同意通过"
}
```

### 3.4 人工复核拒绝

- 方法：`POST`
- 路径：`/api/admin/reviews/{ticketId}/reject`

请求示例：

```json
{
  "rejectReasonCode": "MANUAL_RISK_REJECT",
  "reviewComment": "收入波动较大，近三个月流水异常"
}
```

### 3.5 查询审计时间线

- 方法：`GET`
- 路径：`/api/admin/audit/{applicationId}/timeline`

响应字段建议：

- 申请基础信息
- 状态变更时间线
- 各 Agent 输入输出摘要
- 工具调用记录
- 风险标签与规则命中
- 条款引用记录
- 最终审批结论

## 4. 内部服务接口建议

虽然不一定直接对外暴露，但在工程设计中建议拆出以下内部能力：

- `POST /internal/ocr/parse`
- `POST /internal/credit/query`
- `POST /internal/risk/blacklist-check`
- `POST /internal/rag/policy-search`
- `POST /internal/decision/evaluate`
- `POST /internal/report/generate`

## 5. 状态流转约束

建议接口结合状态机做幂等和校验：

- 只有 `SUBMITTED`、`MATERIAL_PENDING` 状态允许继续上传材料
- 只有 `MANUAL_REVIEW` 状态允许执行人工通过/拒绝
- 只有 `APPROVED`、`REJECTED` 状态允许生成最终归档报告

## 6. 安全与审计建议

- 所有接口记录 `traceId`
- 管理端接口必须带操作人身份
- 审计接口需要区分脱敏视图和原始视图权限
- 上传材料时校验文件类型、大小和病毒扫描结果

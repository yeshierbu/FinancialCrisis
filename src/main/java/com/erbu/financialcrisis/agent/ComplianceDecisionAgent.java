package com.erbu.financialcrisis.agent;

import com.erbu.financialcrisis.domain.entity.LoanApplication;
import org.springframework.stereotype.Component;

/**
 * 合规决策 Agent。
 * 这一层最终应该接规则引擎、RAG 检索和决策解释生成逻辑。
 */
@Component
public class ComplianceDecisionAgent {

    public void decide(LoanApplication application) {
        // TODO: 1. 入参合法性、业务单据状态前置校验
        // TODO: 2. 数据库新增/更新/查询操作（调用Mapper）
        // TODO: 3. 业务单据状态流转变更
        // TODO: 4. 同步数据到外部工单/消息系统
        // TODO: 5. 记录操作审计日志、状态变更日志
        // TODO: 6. 封装返回结果/抛出业务异常
    }
}

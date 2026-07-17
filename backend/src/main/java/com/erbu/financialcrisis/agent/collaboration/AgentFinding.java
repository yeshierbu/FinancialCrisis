package com.erbu.financialcrisis.agent.collaboration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Agent 对案件上下文贡献的一条结构化发现。
 *
 * <p>Agent 之间通过结构化发现共享事实、证据和下一步建议，避免只传递无法审计的自然语言。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentFinding {

    private String agentName;
    private String role;
    private String conclusion;
    private BigDecimal confidence;
    private List<String> evidence;
    private String nextAction;
}

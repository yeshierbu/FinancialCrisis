package com.erbu.financialcrisis.agent.tool;

import com.erbu.financialcrisis.knowledge.PolicyEvidence;
import com.erbu.financialcrisis.knowledge.PolicyKnowledgeStore;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** 提供给审批 Agent 的只读政策检索工具。 */
@Component
public class PolicySearchTool {
    private final PolicyKnowledgeStore knowledgeStore;

    public PolicySearchTool(PolicyKnowledgeStore knowledgeStore) {
        this.knowledgeStore = knowledgeStore;
    }

    public List<PolicyEvidence> search(String question, String productCode) {
        return knowledgeStore.search(question, productCode, LocalDate.now(), 5);
    }
}

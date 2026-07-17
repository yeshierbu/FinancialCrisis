package com.erbu.financialcrisis.agent.result;

import com.erbu.financialcrisis.domain.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 材料采集工具的结构化输出。
 *
 * <p>文档里反复强调 Agent 之间不要传递散乱的自然语言，所以这里单独定义一个结果对象。
 * 第一版只承载材料完整性、缺失材料和解析置信度；后续接入真实 OCR 后，可以继续扩展身份证解析、
 * 收入解析、征信摘要等字段，而不用修改后续风控 Agent 的调用方式。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentIntakeResult {

    /**
     * 当前申请是否已经具备自动审批所需的最低材料集合。
     */
    private Boolean documentComplete;

    /**
     * 仍然缺失的材料类型。前端可以据此生成补件提示，审计侧也可以回放为什么进入补件状态。
     */
    private List<DocumentType> missingDocuments;

    /**
     * 材料解析的整体置信度，用于描述当前 OCR 结果的可用程度。
     */
    private BigDecimal parseConfidence;

    /**
     * 是否需要用户补充材料。它和 documentComplete 含义接近，但保留这个字段便于后续表达
     * “材料齐全但置信度过低，也需要补件”的场景。
     */
    private Boolean needSupplement;

    /**
     * 面向审计和演示的简短摘要，不参与最终硬规则判断。
     */
    private String summary;
}

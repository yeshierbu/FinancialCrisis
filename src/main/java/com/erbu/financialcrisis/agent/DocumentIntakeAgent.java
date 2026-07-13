package com.erbu.financialcrisis.agent;

import com.erbu.financialcrisis.agent.result.DocumentIntakeResult;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.UploadedDocument;
import com.erbu.financialcrisis.domain.enums.DocumentType;
import com.erbu.financialcrisis.domain.enums.OcrStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 信息采集 Agent。
 * 用于承接 OCR、材料解析、字段标准化和补件判断。
 */
@Component
public class DocumentIntakeAgent {

    /**
     * 自动审批第一步所需的最低材料集合。
     *
     * <p>征信报告、收入证明等材料在真实业务里也很重要，但第一版先保持最小可演示集合：
     * 身份证正反面用于身份核验，银行流水用于收入估算。后续可以把这组规则改成产品维度配置。</p>
     */
    private static final Set<DocumentType> REQUIRED_DOCUMENTS = EnumSet.of(
            DocumentType.ID_CARD_FRONT,
            DocumentType.ID_CARD_BACK,
            DocumentType.BANK_STATEMENT
    );

    /** 检查必需材料是否已通过百度千帆 OCR。 */
    public DocumentIntakeResult collectAndParse(LoanApplication application, List<UploadedDocument> documents) {
        List<DocumentType> recognizedTypes = documents.stream()
                .filter(document -> document.getOcrStatus() == OcrStatus.SUCCESS)
                .map(UploadedDocument::getDocumentType)
                .distinct()
                .toList();

        List<DocumentType> missingDocuments = REQUIRED_DOCUMENTS.stream()
                .filter(requiredType -> !recognizedTypes.contains(requiredType))
                .toList();

        boolean usedMockFallback = documents.stream()
                .filter(document -> document.getOcrStatus() == OcrStatus.SUCCESS)
                .map(UploadedDocument::getParseResultJson)
                .filter(result -> result != null && !result.isBlank())
                .anyMatch(result -> result.contains("\"mockFallback\":true"));

        if (!missingDocuments.isEmpty()) {
            return new DocumentIntakeResult(
                    false,
                    missingDocuments,
                    new BigDecimal("0.30"),
                    true,
                    "材料缺失或 OCR 尚未成功，待处理：" + missingDocuments
            );
        }

        return new DocumentIntakeResult(
                true,
                List.of(),
                usedMockFallback ? new BigDecimal("0.60") : new BigDecimal("0.95"),
                false,
                usedMockFallback
                        ? "材料完整，真实 OCR 不可用，已使用模拟 OCR 降级结果（仅供演示）"
                        : "材料完整，已通过百度千帆 DeepSeek-OCR 识别"
        );
    }
}

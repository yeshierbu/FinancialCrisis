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

    public DocumentIntakeResult collectAndParse(LoanApplication application, List<UploadedDocument> documents) {
        List<DocumentType> uploadedTypes = documents.stream()
                .map(UploadedDocument::getDocumentType)
                .distinct()
                .toList();

        List<DocumentType> missingDocuments = REQUIRED_DOCUMENTS.stream()
                .filter(requiredType -> !uploadedTypes.contains(requiredType))
                .toList();

        if (!missingDocuments.isEmpty()) {
            return new DocumentIntakeResult(
                    false,
                    missingDocuments,
                    new BigDecimal("0.30"),
                    true,
                    "材料不完整，缺失：" + missingDocuments
            );
        }

        /*
         * 第一版不接真实 OCR。这里直接把已上传材料标记为解析成功，并写入可读的模拟 JSON。
         * 这样前端和审计接口能看到“材料 -> OCR -> 风控”的链路，后续替换成真实 OCR 工具时，
         * 只需要把这段模拟逻辑移动到 OcrParseTool。
         */
        for (UploadedDocument document : documents) {
            document.setOcrStatus(OcrStatus.SUCCESS);
            document.setParseResultJson(buildMockParseResult(application, document));
        }

        return new DocumentIntakeResult(
                true,
                List.of(),
                new BigDecimal("0.92"),
                false,
                "材料完整，已完成本地模拟 OCR 解析"
        );
    }

    private String buildMockParseResult(LoanApplication application, UploadedDocument document) {
        return """
                {"documentType":"%s","applicantName":"%s","parseStatus":"SUCCESS"}
                """.formatted(document.getDocumentType(), application.getApplicantName()).trim();
    }
}

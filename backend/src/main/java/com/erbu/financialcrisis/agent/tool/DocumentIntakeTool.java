package com.erbu.financialcrisis.agent.tool;

import com.erbu.financialcrisis.agent.result.DocumentIntakeResult;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.UploadedDocument;
import com.erbu.financialcrisis.domain.enums.DocumentType;
import com.erbu.financialcrisis.domain.enums.OcrStatus;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Set;

/**
 * 材料采集工具。只负责可复算的材料存在性、OCR 状态和补件判断。
 */
@Component
public class DocumentIntakeTool {

    /**
     * 自动审批第一步所需的最低材料集合。
     *
     * <p>征信报告、收入证明等材料在真实业务里也很重要，但第一版先保持最小可演示集合：
     * 身份证正反面用于身份核验，银行流水用于收入估算。后续可以把这组规则改成产品维度配置。</p>
     */
    private final Set<DocumentType> requiredDocuments;

    public DocumentIntakeTool(@Value("${approval.documents.required-types:ID_CARD_FRONT,ID_CARD_BACK,BANK_STATEMENT}")
                              String requiredTypes) {
        this.requiredDocuments = Arrays.stream(requiredTypes.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(DocumentType::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** 检查必需材料是否已通过百炼 Qwen OCR。
     * 检查一笔贷款申请的上传材料是否满足自动审批的最低要求*/
    public DocumentIntakeResult collectAndParse(LoanApplication application, List<UploadedDocument> documents) {

        /**
          * 从上传材料列表 documents 里找出“已经 OCR 成功的材料类型，存到recognizedTypes列表
        */
        List<DocumentType> recognizedTypes = documents.stream()
                .filter(document -> document.getOcrStatus() == OcrStatus.SUCCESS)
                .map(UploadedDocument::getDocumentType)
                .distinct()
                .toList();
        /**
         * 和上传过的材料进行比较，看系统必须材料中还差哪些材料，并且添加到missingDocuments列表
         */
        List<DocumentType> missingDocuments = requiredDocuments.stream()
                .filter(requiredType -> !recognizedTypes.contains(requiredType))
                .toList();
/**
 *判断有没有使用模拟 OCR 兜底生成的成功解析结果
 */
        boolean usedMockFallback = documents.stream()
                .filter(document -> document.getOcrStatus() == OcrStatus.SUCCESS)
                .map(UploadedDocument::getParseResultJson)
                .filter(result -> result != null && !result.isBlank())
                .anyMatch(result -> result.contains("\"mockFallback\":true"));

        /**
         * 如果存在缺失材料提示补充missingDocuments
         */
        if (!missingDocuments.isEmpty()) {
            return new DocumentIntakeResult(
                    false,
                    missingDocuments,
                    new BigDecimal("0.30"),
                    true,
                    "材料缺失或 OCR 尚未成功，待处理：" + missingDocuments
            );
        }

        /**
         * 不存在材料缺失，后续只判断是否有mockFallback为true的数据
         */
        return new DocumentIntakeResult(
                true,
                List.of(),
                usedMockFallback ? new BigDecimal("0.60") : new BigDecimal("0.95"),
                false,
                usedMockFallback
                        ? "材料完整，真实 OCR 不可用，已使用模拟 OCR 降级结果 "
                        : "材料完整，已通过百炼 Qwen OCR 识别"
        );
    }

    /** 上传接口用它判断何时可以投递审批任务，避免每份材料都创建一条队列消息。 */
    public boolean isReadyForApproval(List<UploadedDocument> documents) {
        Set<DocumentType> recognizedTypes = documents.stream()
                .filter(document -> document.getOcrStatus() == OcrStatus.SUCCESS)
                .map(UploadedDocument::getDocumentType)
                .collect(Collectors.toSet());
        return recognizedTypes.containsAll(requiredDocuments);
    }
}

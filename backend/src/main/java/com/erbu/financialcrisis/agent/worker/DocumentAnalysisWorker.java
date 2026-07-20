package com.erbu.financialcrisis.agent.worker;

import com.erbu.financialcrisis.agent.artifact.DocumentAnalysisReport;
import com.erbu.financialcrisis.agent.result.DocumentIntakeResult;
import com.erbu.financialcrisis.agent.runtime.StructuredLlmClient;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.UploadedDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 真正的 LLM 材料分析 Worker：理解 OCR 文本、核对身份与收入证据。
 * 它不决定材料是否已上传，材料存在性仍由 DocumentIntakeTool 确定性校验。
 */
@Component
public class DocumentAnalysisWorker {
    private final StructuredLlmClient llm;
    private final ObjectMapper objectMapper;

    public DocumentAnalysisWorker(StructuredLlmClient llm, ObjectMapper objectMapper) {
        this.llm = llm;
        this.objectMapper = objectMapper;
    }

    public DocumentAnalysisReport analyze(LoanApplication application,
                                          List<UploadedDocument> documents,
                                          DocumentIntakeResult intakeResult) {
        try {
            List<Map<String, Object>> material = documents.stream().map(document -> {
                Map<String, Object> item = new LinkedHashMap<>();
                //提取OCR结果
                item.put("documentType", document.getDocumentType());
                item.put("ocrStatus", document.getOcrStatus());
                item.put("ocrResult", document.getParseResultJson());
                return item;
            }).toList();
            //上传给LLM
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("declaredApplication", application);
            input.put("intakeToolResult", intakeResult);
            input.put("materials", material);

            return llm.generate(
                    "你是信贷材料分析 Agent。只能根据 OCR 内容和申请人申报信息提取证据，"
                            + "不得编造收入、身份或交易。recommendedAction 只能是 CONTINUE、SUPPLEMENT、MANUAL_REVIEW。",
                    "请分析材料并返回 DocumentAnalysisReport JSON：\n"
                            + objectMapper.writeValueAsString(input)
                            + "\n字段：documentComplete,identityConsistent,incomeEvidence,evidence,anomalies,"
                            + "missingEvidence,recommendedAction,confidence,summary。",
                    DocumentAnalysisReport.class
            );
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("DocumentAnalysisWorker 构建上下文失败", ex);
        }
    }
}

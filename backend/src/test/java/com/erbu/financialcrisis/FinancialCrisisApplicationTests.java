package com.erbu.financialcrisis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erbu.financialcrisis.service.QianfanOcrService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 最基础的上下文加载测试。
 * 后续可以继续补 Controller、Service 和 Agent 的单元测试。
 */
@SpringBootTest(properties = {
        "llm.api-key=test-api-key",
        "llm.enabled=true"
})
@AutoConfigureMockMvc
@Transactional
class FinancialCrisisApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatLanguageModel chatLanguageModel;

    @MockBean
    private QianfanOcrService qianfanOcrService;

    @BeforeEach
    void mockMultiAgentResponses() {
        // 使用一个联合 JSON 模拟四个 Worker 的结构化输出，测试不访问真实 LLM。
        // Spring Boot 的 ObjectMapper 会忽略当前目标 DTO 不需要的字段。
        String json = """
                {
                  "riskLevel": "LOW",
                  "riskScore": 20,
                  "recommendedAction": "PASS",
                  "claims": ["工具事实一致，未发现显著风险"],
                  "missingEvidence": [],
                  "policyReferences": [],
                  "confidence": 0.95,
                  "evidence": ["fraud-tool-result", "repayment-tool-result"],
                  "summary": "Mocked multi-agent result",
                  "accepted": true,
                  "contradictions": [],
                  "unsupportedClaims": [],
                  "revisionInstructions": [],
                  "decision": "APPROVED",
                  "approvedAmount": 50000,
                  "approvedTerm": 24,
                  "reasonCodes": ["LOW_RISK"],
                  "explanation": "风险较低且偿债能力满足要求"
                }
                """;
        when(chatLanguageModel.generate(anyList()))
                .thenReturn(Response.from(AiMessage.from(json)));
        when(qianfanOcrService.recognize(any(), anyString()))
                .thenReturn("{\"provider\":\"BAIDU_QIANFAN\",\"model\":\"deepseek-ocr\",\"text\":\"mock OCR text\"}");
    }

    @Test
    void contextLoads() {
    }

    @Test
    void mainApprovalFlowCanRunEndToEnd() throws Exception {
        String createRequest = """
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
                """;

        String createResponse = mockMvc.perform(post("/api/loan/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("DOCUMENT_PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = objectMapper.readTree(createResponse);
        long applicationId = responseJson.path("data").path("applicationId").asLong();

        uploadDocument(applicationId, "ID_CARD_FRONT", "id-front.jpg", "memory://docs/id-front.jpg");
        uploadDocument(applicationId, "ID_CARD_BACK", "id-back.jpg", "memory://docs/id-back.jpg");
        uploadDocument(applicationId, "BANK_STATEMENT", "bank-statement.jpg", "memory://docs/bank-statement.jpg");

        mockMvc.perform(get("/api/loan/applications/{applicationId}/status", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/loan/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].applicationId").value(applicationId));

        mockMvc.perform(get("/api/loan/applications/{applicationId}/report", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.reportType").value("INTERNAL_AUDIT"));

        mockMvc.perform(get("/api/admin/audit/{applicationId}/timeline", applicationId)
                        .param("includeRawPayload", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.timeline").isArray());
    }

    @Test
    void ocrFailureMustStopAutomaticApproval() throws Exception {
        when(qianfanOcrService.recognize(any(), anyString()))
                .thenThrow(new IllegalStateException("mock OCR failure"));

        String createRequest = """
                {
                  "productCode": "CONSUMER_LOAN_STD",
                  "applicantName": "测试用户",
                  "idCardNo": "310101199001011234",
                  "mobile": "13800138000",
                  "loanAmount": 20000,
                  "loanTerm": 12,
                  "employmentType": "FULL_TIME",
                  "companyName": "测试公司",
                  "workYears": 3
                }
                """;
        String createResponse = mockMvc.perform(post("/api/loan/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long applicationId = objectMapper.readTree(createResponse).path("data").path("applicationId").asLong();

        MockMultipartFile file = new MockMultipartFile(
                "file", "id-front.jpg", "image/jpeg", "test-image".getBytes()
        );
        mockMvc.perform(multipart("/api/loan/applications/{applicationId}/documents", applicationId)
                        .file(file)
                        .param("documentType", "ID_CARD_FRONT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ocrStatus").value("FAILED"));

        mockMvc.perform(get("/api/loan/applications/{applicationId}/status", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DOCUMENT_PENDING"));
    }

    private void uploadDocument(long applicationId, String documentType, String fileName, String fileUrl) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", fileName, "image/jpeg", ("test-image-" + fileUrl).getBytes()
        );
        mockMvc.perform(multipart("/api/loan/applications/{applicationId}/documents", applicationId)
                        .file(file)
                        .param("documentType", documentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.ocrStatus").value("SUCCESS"));
    }
}

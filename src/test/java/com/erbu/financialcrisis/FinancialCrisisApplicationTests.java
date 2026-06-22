package com.erbu.financialcrisis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 最基础的上下文加载测试。
 * 后续可以继续补 Controller、Service 和 Agent 的单元测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
class FinancialCrisisApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        uploadDocument(applicationId, "BANK_STATEMENT", "bank-statement.pdf", "memory://docs/bank-statement.pdf");

        mockMvc.perform(get("/api/loan/applications/{applicationId}/status", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

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

    private void uploadDocument(long applicationId, String documentType, String fileName, String fileUrl) throws Exception {
        String request = """
                {
                  "documentType": "%s",
                  "fileName": "%s",
                  "fileUrl": "%s",
                  "fileSize": 1024
                }
                """.formatted(documentType, fileName, fileUrl);

        mockMvc.perform(post("/api/loan/applications/{applicationId}/documents", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}

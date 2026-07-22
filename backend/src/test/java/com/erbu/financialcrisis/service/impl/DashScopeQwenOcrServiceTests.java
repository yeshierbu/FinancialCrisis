package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.domain.enums.DocumentType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashScopeQwenOcrServiceTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void enabledFallbackProducesExplicitMockResult() throws Exception {
        DashScopeQwenOcrService service = createService(true);

        String result = service.recognize(
                DocumentType.ID_CARD_FRONT,
                "data:image/jpeg;base64,dGVzdA=="
        );
        JsonNode json = objectMapper.readTree(result);

        assertEquals("MOCK_FALLBACK", json.path("provider").asText());
        assertEquals("mock-ocr", json.path("model").asText());
        assertTrue(json.path("mockFallback").asBoolean());
        assertEquals("OCR_CLIENT_ERROR", json.path("failureCode").asText());
    }

    @Test
    void disabledFallbackStillPropagatesOcrFailure() {
        DashScopeQwenOcrService service = createService(false);

        assertThrows(IllegalStateException.class, () -> service.recognize(
                DocumentType.ID_CARD_FRONT,
                "data:image/jpeg;base64,dGVzdA=="
        ));
    }

    private DashScopeQwenOcrService createService(boolean fallbackToMock) {
        return new DashScopeQwenOcrService(
                RestClient.builder(),
                objectMapper,
                "http://127.0.0.1:1",
                "",
                "qwen3.5-ocr",
                1,
                fallbackToMock
        );
    }
}

package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.domain.enums.DocumentType;
import com.erbu.financialcrisis.service.QianfanOcrService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 百度千帆 DeepSeek-OCR 客户端。
 * 图片仅在请求期间以 Base64 data URL 发送，不在本地保存原图。
 */
@Service
public class BaiduQianfanOcrService implements QianfanOcrService {

    private static final Logger log = LoggerFactory.getLogger(BaiduQianfanOcrService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final boolean fallbackToMock;

    public BaiduQianfanOcrService(RestClient.Builder restClientBuilder,
                                  ObjectMapper objectMapper,
                                  @Value("${ocr.baidu.base-url:https://qianfan.baidubce.com/v2}") String baseUrl,
                                  @Value("${ocr.baidu.api-key:}") String apiKey,
                                  @Value("${ocr.baidu.model:deepseek-ocr}") String model,
                                  @Value("${ocr.baidu.timeout-seconds:60}") long timeoutSeconds,
                                  @Value("${ocr.baidu.fallback-to-mock:false}") boolean fallbackToMock) {
        Duration timeout = Duration.ofSeconds(timeoutSeconds);
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        this.restClient = restClientBuilder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.fallbackToMock = fallbackToMock;
    }

    @Override
    public String recognize(DocumentType documentType, String imageUrl) {
        try {
            validateConfiguration(imageUrl);

            Map<String, Object> payload = Map.of(
                    "model", model,
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of("type", "text", "text", promptFor(documentType)),
                                    Map.of("type", "image_url", "image_url", Map.of("url", imageUrl))
                            )
                    )),
                    "stream", false,
                    "max_tokens", 2500
            );

            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            JsonNode response = objectMapper.readTree(responseBody);
            String recognizedText = response.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();
            if (recognizedText.isBlank()) {
                throw new IllegalStateException("百度千帆 DeepSeek-OCR 返回内容为空");
            }

            return objectMapper.writeValueAsString(Map.of(
                    "provider", "BAIDU_QIANFAN",
                    "model", model,
                    "documentType", documentType.name(),
                    "text", recognizedText,
                    "recognizedAt", LocalDateTime.now().toString()
            ));
        } catch (RestClientResponseException ex) {
            String providerError = summarizeProviderError(ex.getResponseBodyAsString());
            if (fallbackToMock) {
                log.warn(
                        "百度千帆 DeepSeek-OCR 请求失败，开发环境已启用模拟降级，documentType={}, model={}, httpStatus={}, providerError={}",
                        documentType,
                        model,
                        ex.getStatusCode().value(),
                        providerError
                );
                return mockFallbackResult(
                        documentType,
                        "HTTP_" + ex.getStatusCode().value(),
                        providerError
                );
            }
            log.error(
                    "百度千帆 DeepSeek-OCR 请求失败，documentType={}, model={}, httpStatus={}, providerError={}",
                    documentType,
                    model,
                    ex.getStatusCode().value(),
                    providerError,
                    ex
            );
            throw new IllegalStateException(
                    "百度千帆 DeepSeek-OCR 调用失败，HTTP "
                            + ex.getStatusCode().value() + "，" + providerError,
                    ex
            );
        } catch (Exception ex) {
            if (fallbackToMock) {
                log.warn(
                        "百度千帆 DeepSeek-OCR 调用失败，开发环境已启用模拟降级，documentType={}, model={}, reason={}",
                        documentType,
                        model,
                        safeLogValue(ex.getMessage())
                );
                return mockFallbackResult(
                        documentType,
                        "OCR_CLIENT_ERROR",
                        safeLogValue(ex.getMessage())
                );
            }
            log.error(
                    "百度千帆 DeepSeek-OCR 调用或响应解析失败，documentType={}, model={}",
                    documentType,
                    model,
                    ex
            );
            throw new IllegalStateException("百度千帆 DeepSeek-OCR 调用失败", ex);
        }
    }

    private String mockFallbackResult(DocumentType documentType,
                                      String failureCode,
                                      String failureMessage) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "provider", "MOCK_FALLBACK",
                    "model", "mock-ocr",
                    "documentType", documentType.name(),
                    "text", "模拟 OCR 结果：材料已接收，真实 OCR 当前不可用。",
                    "mockFallback", true,
                    "failureCode", failureCode,
                    "failureMessage", failureMessage,
                    "recognizedAt", LocalDateTime.now().toString()
            ));
        } catch (Exception ex) {
            throw new IllegalStateException("创建 OCR 模拟降级结果失败", ex);
        }
    }

    private String summarizeProviderError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "providerError=EMPTY_RESPONSE_BODY";
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode error = root.path("error");
            String code = firstNonBlank(error.path("code").asText(), root.path("code").asText());
            String type = firstNonBlank(error.path("type").asText(), root.path("type").asText());
            String message = firstNonBlank(error.path("message").asText(), root.path("message").asText());
            return "code=" + safeLogValue(code)
                    + ", type=" + safeLogValue(type)
                    + ", message=" + safeLogValue(message);
        } catch (Exception ignored) {
            return "providerError=UNPARSEABLE_RESPONSE, bodyLength=" + responseBody.length();
        }
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String safeLogValue(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String sanitized = value
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("bce-v3/[^\\s,]+", "[REDACTED]");
        return sanitized.length() <= 500 ? sanitized : sanitized.substring(0, 500) + "...";
    }

    private void validateConfiguration(String imageUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置 BAIDU_API_KEY，不能执行真实 OCR");
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("OCR 图片内容不能为空");
        }
    }

    private String promptFor(DocumentType documentType) {
        return switch (documentType) {
            case ID_CARD_FRONT, ID_CARD_BACK ->
                    "<image>\nFree OCR. 忠实提取身份证图片中的全部可见文字，不要猜测缺失内容。";
            case BANK_STATEMENT ->
                    "<image>\n<|grounding|>Convert the document to markdown. 忠实保留银行流水中的日期、摘要、收入、支出和余额。";
            default -> "<image>\nFree OCR. 忠实提取图片中的全部可见文字，不要补充不存在的信息。";
        };
    }
}

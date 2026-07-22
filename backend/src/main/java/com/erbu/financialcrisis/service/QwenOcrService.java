package com.erbu.financialcrisis.service;

import com.erbu.financialcrisis.domain.enums.DocumentType;

/**
 * Qwen OCR 服务。
 */
public interface QwenOcrService {

    /**
     * 使用图片 URL 或 data URL 识别材料，并返回可直接写入 JSON 字段的结果。
     */
    String recognize(DocumentType documentType, String imageUrl);
}

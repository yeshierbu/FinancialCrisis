package com.erbu.financialcrisis.service;

import com.erbu.financialcrisis.domain.enums.DocumentType;

/**
 * 百度千帆 OCR 服务。
 */
public interface QianfanOcrService {

    /**
     * 使用图片 URL 或 data URL 识别材料，并返回可直接写入 JSON 字段的结果。
     */
    String recognize(DocumentType documentType, String imageUrl);
}

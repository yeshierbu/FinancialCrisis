package com.erbu.financialcrisis.dto.request;

import com.erbu.financialcrisis.domain.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 补充材料明细请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplementDocumentRequest {

    @NotNull(message = "材料类型不能为空")
    private DocumentType documentType;

    @NotBlank(message = "文件地址不能为空")
    private String fileUrl;
}

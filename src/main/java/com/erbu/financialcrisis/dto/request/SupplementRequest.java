package com.erbu.financialcrisis.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 提交补充资料请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplementRequest {

    private String remark;

    @Valid
    @NotEmpty(message = "补充材料不能为空")
    private List<SupplementDocumentRequest> documents;
}

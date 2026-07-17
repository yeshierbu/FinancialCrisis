package com.erbu.financialcrisis.dto.request;

import com.erbu.financialcrisis.domain.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 待人工复核列表查询请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualReviewPendingQueryRequest {

    private Integer pageNo;
    private Integer pageSize;
    private RiskLevel riskLevel;
    private String productCode;
}

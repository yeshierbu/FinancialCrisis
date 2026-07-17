package com.erbu.financialcrisis.dto.response;

import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import com.erbu.financialcrisis.domain.enums.DecisionResult;
import com.erbu.financialcrisis.domain.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 人工复核处理响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualReviewResponse {

    private Long applicationId;
    private ReviewStatus reviewStatus;
    private DecisionResult decisionResult;
    private ApplicationStatus applicationStatus;
}

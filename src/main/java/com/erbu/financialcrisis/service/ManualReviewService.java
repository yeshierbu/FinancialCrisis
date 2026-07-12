package com.erbu.financialcrisis.service;

import com.erbu.financialcrisis.dto.request.ManualReviewRequest;
import com.erbu.financialcrisis.dto.request.ManualReviewPendingQueryRequest;
import com.erbu.financialcrisis.dto.response.ManualReviewDetailResponse;
import com.erbu.financialcrisis.dto.response.ManualReviewPendingResponse;
import com.erbu.financialcrisis.dto.response.ManualReviewResponse;

import java.util.List;

/**
 * 人工复核服务接口。
 */
public interface ManualReviewService {

    /** 查询待复核工单。 */
    List<ManualReviewPendingResponse> queryPendingReviews(ManualReviewPendingQueryRequest request);

    /** 查询复核详情及当前审批建议。 */
    ManualReviewDetailResponse getReviewDetail(Long applicationId);

    /** 人工通过申请。 */
    ManualReviewResponse approve(Long applicationId, ManualReviewRequest request);

    /** 人工拒绝申请。 */
    ManualReviewResponse reject(Long applicationId, ManualReviewRequest request);
}

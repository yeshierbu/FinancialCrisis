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

    List<ManualReviewPendingResponse> queryPendingReviews(ManualReviewPendingQueryRequest request);

    ManualReviewDetailResponse getReviewDetail(Long applicationId);

    ManualReviewResponse approve(Long applicationId, ManualReviewRequest request);

    ManualReviewResponse reject(Long applicationId, ManualReviewRequest request);
}

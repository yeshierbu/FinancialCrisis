package com.erbu.financialcrisis.service;

import com.erbu.financialcrisis.dto.request.ManualReviewRequest;

/**
 * 人工复核服务接口。
 */
public interface ManualReviewService {

    void approve(Long applicationId, ManualReviewRequest request);

    void reject(Long applicationId, ManualReviewRequest request);
}

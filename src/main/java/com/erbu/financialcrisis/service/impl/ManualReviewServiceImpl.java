package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.dto.request.ManualReviewRequest;
import com.erbu.financialcrisis.service.ManualReviewService;
import org.springframework.stereotype.Service;

/**
 * 人工复核服务实现。
 * 第一版只保留方法骨架，后续可以接数据库、工单系统和审计日志。
 */
@Service
public class ManualReviewServiceImpl implements ManualReviewService {

    @Override
    public void approve(Long applicationId, ManualReviewRequest request) {
        // TODO: 1. 校验申请是否处于 MANUAL_REVIEW
        // TODO: 2. 落库人工审批结果
        // TODO: 3. 写状态迁移日志和审计日志
    }

    @Override
    public void reject(Long applicationId, ManualReviewRequest request) {
        // TODO: 1. 校验申请是否处于 MANUAL_REVIEW
        // TODO: 2. 保存拒贷原因码与复核意见
        // TODO: 3. 推进状态为 REJECTED 并生成报告
    }
}

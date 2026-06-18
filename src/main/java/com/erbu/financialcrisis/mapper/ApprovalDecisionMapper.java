package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.ApprovalDecision;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批决策结果表 Mapper。
 */
@Mapper
public interface ApprovalDecisionMapper {

    int insert(ApprovalDecision approvalDecision);

    int updateByApplicationId(ApprovalDecision approvalDecision);

    ApprovalDecision selectByApplicationId(Long applicationId);

    int deleteByApplicationId(Long applicationId);
}

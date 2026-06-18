package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.ApprovalReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批报告表 Mapper。
 */
@Mapper
public interface ApprovalReportMapper {

    int insert(ApprovalReport approvalReport);

    int updateByApplicationId(ApprovalReport approvalReport);

    ApprovalReport selectByApplicationId(Long applicationId);

    int deleteByApplicationId(Long applicationId);
}

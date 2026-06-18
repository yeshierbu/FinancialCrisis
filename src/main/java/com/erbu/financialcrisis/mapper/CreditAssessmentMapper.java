package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.CreditAssessment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 征信评估结果表 Mapper。
 */
@Mapper
public interface CreditAssessmentMapper {

    int insert(CreditAssessment creditAssessment);

    int updateByApplicationId(CreditAssessment creditAssessment);

    CreditAssessment selectByApplicationId(Long applicationId);

    int deleteByApplicationId(Long applicationId);
}

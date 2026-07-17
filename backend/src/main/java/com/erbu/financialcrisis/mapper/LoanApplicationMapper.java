package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.LoanApplication;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 贷款申请主表 Mapper。
 */
@Mapper
public interface LoanApplicationMapper {

    int insert(LoanApplication loanApplication);

    int updateByApplicationId(LoanApplication loanApplication);

    LoanApplication selectByApplicationId(Long applicationId);

    List<LoanApplication> selectAll();

    int deleteByApplicationId(Long applicationId);
}

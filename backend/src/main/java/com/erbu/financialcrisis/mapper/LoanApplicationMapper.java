package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.LoanApplication;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import org.apache.ibatis.annotations.Param;

/**
 * 贷款申请主表 Mapper。
 */
@Mapper
public interface LoanApplicationMapper {

    int insert(LoanApplication loanApplication);

    int updateByApplicationId(LoanApplication loanApplication);
    int updateStatusIfCurrent(@Param("applicationId") Long applicationId,
                              @Param("expectedStatus") ApplicationStatus expectedStatus,
                              @Param("toStatus") ApplicationStatus toStatus,
                              @Param("currentStep") String currentStep,
                              @Param("updatedAt") java.time.LocalDateTime updatedAt);

    LoanApplication selectByApplicationId(Long applicationId);
    LoanApplication selectOwnedByApplicationId(@Param("applicationId") Long applicationId,
                                               @Param("ownerUsername") String ownerUsername);

    List<LoanApplication> selectAll();
    List<LoanApplication> selectByOwnerUsername(String ownerUsername);

    int deleteByApplicationId(Long applicationId);
}

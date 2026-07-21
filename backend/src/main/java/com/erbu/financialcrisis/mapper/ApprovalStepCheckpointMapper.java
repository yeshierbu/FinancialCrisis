package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.ApprovalStepCheckpoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ApprovalStepCheckpointMapper {
    ApprovalStepCheckpoint select(@Param("applicationId") Long applicationId, @Param("stepName") String stepName);
    int insert(ApprovalStepCheckpoint checkpoint);
    int deleteByApplicationId(Long applicationId);
}

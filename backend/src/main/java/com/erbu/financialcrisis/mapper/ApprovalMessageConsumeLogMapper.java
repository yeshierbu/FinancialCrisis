package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.ApprovalMessageConsumeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ApprovalMessageConsumeLogMapper {
    ApprovalMessageConsumeLog selectByEventId(String eventId);
    int insertProcessing(ApprovalMessageConsumeLog log);
    int retryClaim(@Param("eventId") String eventId, @Param("consumerName") String consumerName,
                   @Param("retryCount") int retryCount, @Param("claimToken") String claimToken,
                   @Param("leaseUntil") java.time.LocalDateTime leaseUntil);
    int markCompleted(@Param("eventId") String eventId, @Param("claimToken") String claimToken);
    int markFailed(@Param("eventId") String eventId, @Param("lastError") String lastError,
                   @Param("retryCount") int retryCount, @Param("claimToken") String claimToken);
}

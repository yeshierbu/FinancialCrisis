package com.erbu.financialcrisis.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface ApprovalExecutionLockMapper {
    int insert(@Param("applicationId") Long applicationId, @Param("eventId") String eventId,
               @Param("lockedBy") String lockedBy, @Param("expiresAt") LocalDateTime expiresAt);
    int delete(@Param("applicationId") Long applicationId, @Param("eventId") String eventId);
    int deleteExpired(@Param("applicationId") Long applicationId, @Param("now") LocalDateTime now);
}

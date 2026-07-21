package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.ApprovalOutbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApprovalOutboxMapper {
    int insert(ApprovalOutbox outbox);
    List<ApprovalOutbox> selectPending(@Param("limit") int limit);
    int claim(@Param("id") Long id);
    int markPublished(@Param("id") Long id);
    int markRetry(@Param("id") Long id, @Param("lastError") String lastError,
                  @Param("nextRetryAt") java.time.LocalDateTime nextRetryAt);
    int markConsumed(String eventId);
    int resetStalePublishing(@Param("cutoff") java.time.LocalDateTime cutoff);
}

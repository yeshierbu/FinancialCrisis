package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.ManualReviewTicket;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import com.erbu.financialcrisis.domain.enums.ReviewStatus;
import org.apache.ibatis.annotations.Param;

/**
 * 人工复核工单表 Mapper。
 */
@Mapper
public interface ManualReviewTicketMapper {

    int insert(ManualReviewTicket manualReviewTicket);

    int updateByApplicationId(ManualReviewTicket manualReviewTicket);
    int decidePending(@Param("applicationId") Long applicationId,
                      @Param("reviewStatus") ReviewStatus reviewStatus,
                      @Param("reviewComment") String reviewComment,
                      @Param("reviewedAt") java.time.LocalDateTime reviewedAt);

    ManualReviewTicket selectByApplicationId(Long applicationId);

    List<ManualReviewTicket> selectPendingList();

    int deleteByApplicationId(Long applicationId);
}

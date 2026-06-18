package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.ManualReviewTicket;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 人工复核工单表 Mapper。
 */
@Mapper
public interface ManualReviewTicketMapper {

    int insert(ManualReviewTicket manualReviewTicket);

    int updateByApplicationId(ManualReviewTicket manualReviewTicket);

    ManualReviewTicket selectByApplicationId(Long applicationId);

    List<ManualReviewTicket> selectPendingList();

    int deleteByApplicationId(Long applicationId);
}

package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.RepaymentCapacityResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 偿债能力结果表 Mapper。
 */
@Mapper
public interface RepaymentCapacityResultMapper {

    int insert(RepaymentCapacityResult repaymentCapacityResult);

    int updateByApplicationId(RepaymentCapacityResult repaymentCapacityResult);

    RepaymentCapacityResult selectByApplicationId(Long applicationId);

    int deleteByApplicationId(Long applicationId);
}

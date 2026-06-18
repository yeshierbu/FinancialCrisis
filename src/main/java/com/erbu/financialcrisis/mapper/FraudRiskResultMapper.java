package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.FraudRiskResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 反欺诈风控结果表 Mapper。
 */
@Mapper
public interface FraudRiskResultMapper {

    int insert(FraudRiskResult fraudRiskResult);

    int updateByApplicationId(FraudRiskResult fraudRiskResult);

    FraudRiskResult selectByApplicationId(Long applicationId);

    int deleteByApplicationId(Long applicationId);
}

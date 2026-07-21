package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.RiskBlacklistHit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RiskBlacklistMapper {
    RiskBlacklistHit selectActive(@Param("subjectType") String subjectType,
                                  @Param("subjectHash") String subjectHash);
}

package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.PolicyHitRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 政策命中记录表 Mapper。
 */
@Mapper
public interface PolicyHitRecordMapper {

    int insert(PolicyHitRecord policyHitRecord);

    int updateById(PolicyHitRecord policyHitRecord);

    PolicyHitRecord selectById(Long id);

    List<PolicyHitRecord> selectByApplicationId(Long applicationId);

    int deleteById(Long id);
}

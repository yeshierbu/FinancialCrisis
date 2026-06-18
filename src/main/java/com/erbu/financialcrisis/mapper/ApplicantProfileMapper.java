package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.ApplicantProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 申请人画像表 Mapper。
 */
@Mapper
public interface ApplicantProfileMapper {

    int insert(ApplicantProfile applicantProfile);

    int updateByApplicationId(ApplicantProfile applicantProfile);

    ApplicantProfile selectByApplicationId(Long applicationId);

    int deleteByApplicationId(Long applicationId);
}

package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.SystemAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemAccountMapper {
    SystemAccount selectByUsername(String username);
}

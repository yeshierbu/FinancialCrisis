package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.StateTransitionLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 状态流转日志表 Mapper。
 */
@Mapper
public interface StateTransitionLogMapper {

    int insert(StateTransitionLog stateTransitionLog);

    int updateByLogId(StateTransitionLog stateTransitionLog);

    StateTransitionLog selectByLogId(Long logId);

    List<StateTransitionLog> selectByApplicationId(Long applicationId);

    int deleteByLogId(Long logId);
}

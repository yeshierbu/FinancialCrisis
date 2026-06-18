package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.AgentTaskLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Agent 任务日志表 Mapper。
 */
@Mapper
public interface AgentTaskLogMapper {

    int insert(AgentTaskLog agentTaskLog);

    int updateByLogId(AgentTaskLog agentTaskLog);

    AgentTaskLog selectByLogId(Long logId);

    List<AgentTaskLog> selectByApplicationId(Long applicationId);

    int deleteByLogId(Long logId);
}

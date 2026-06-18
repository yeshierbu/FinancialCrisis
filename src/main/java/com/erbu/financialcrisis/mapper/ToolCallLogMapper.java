package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.ToolCallLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 工具调用日志表 Mapper。
 */
@Mapper
public interface ToolCallLogMapper {

    int insert(ToolCallLog toolCallLog);

    int updateByLogId(ToolCallLog toolCallLog);

    ToolCallLog selectByLogId(Long logId);

    List<ToolCallLog> selectByApplicationId(Long applicationId);

    int deleteByLogId(Long logId);
}

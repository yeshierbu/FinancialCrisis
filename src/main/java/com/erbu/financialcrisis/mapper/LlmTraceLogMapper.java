package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.LlmTraceLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * LLM 调用轨迹表 Mapper。
 */
@Mapper
public interface LlmTraceLogMapper {

    int insert(LlmTraceLog llmTraceLog);

    int updateByLogId(LlmTraceLog llmTraceLog);

    LlmTraceLog selectByLogId(Long logId);

    List<LlmTraceLog> selectByApplicationId(Long applicationId);

    int deleteByLogId(Long logId);
}

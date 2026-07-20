package com.erbu.financialcrisis.service;

import com.erbu.financialcrisis.domain.entity.ApprovalStepCheckpoint;
import com.erbu.financialcrisis.mapper.ApprovalStepCheckpointMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Service
public class ApprovalCheckpointService {
    private final ApprovalStepCheckpointMapper mapper;
    private final ObjectMapper objectMapper;

    public ApprovalCheckpointService(ApprovalStepCheckpointMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper; this.objectMapper = objectMapper;
    }

    public <T> T restoreOrExecute(Long applicationId, String stepName, Class<T> type, Supplier<T> work) {
        ApprovalStepCheckpoint existing = mapper.select(applicationId, stepName);
        if (existing != null) return read(existing.getStateJson(), type);
        T result = work.get();
        save(applicationId, stepName, result);
        return result;
    }

    public <T> T require(Long applicationId, String stepName, Class<T> type) {
        ApprovalStepCheckpoint existing = mapper.select(applicationId, stepName);
        if (existing == null) throw new IllegalStateException("缺少审批检查点：" + stepName);
        return read(existing.getStateJson(), type);
    }

    @Transactional
    public <T> void save(Long applicationId, String stepName, T result) {
        ApprovalStepCheckpoint value = new ApprovalStepCheckpoint();
        value.setApplicationId(applicationId); value.setStepName(stepName);
        value.setStateJson(write(result)); value.setCompletedAt(LocalDateTime.now()); value.setCreatedAt(LocalDateTime.now());
        try { mapper.insert(value); } catch (DuplicateKeyException ignored) { /* concurrent winner is authoritative */ }
    }

    @Transactional public void clear(Long applicationId) { mapper.deleteByApplicationId(applicationId); }

    private String write(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception ex) { throw new IllegalStateException("序列化审批检查点失败", ex); }
    }
    private <T> T read(String json, Class<T> type) {
        try {
            var node = objectMapper.readTree(json);
            return node.isTextual() ? objectMapper.readValue(node.asText(), type) : objectMapper.treeToValue(node, type);
        }
        catch (Exception ex) { throw new IllegalStateException("恢复审批检查点失败：" + ex.getMessage(), ex); }
    }
}

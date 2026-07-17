package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.common.BusinessException;
import com.erbu.financialcrisis.knowledge.PolicyKnowledgeStore;
import com.erbu.financialcrisis.mapper.PolicyDocumentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PolicyIngestionServiceImplTests {

    @Test
    void shouldSplitTextAndWriteEveryChunkToKnowledgeStore() {
        PolicyDocumentMapper mapper = mock(PolicyDocumentMapper.class);
        PolicyKnowledgeStore store = mock(PolicyKnowledgeStore.class);
        PolicyIngestionServiceImpl service = new PolicyIngestionServiceImpl(
                mapper, store, "credit_policy_chunks_v4");
        String content = "消费贷审批政策。\n\n" + "申请人偿债能力与风险证据。".repeat(100);
        MockMultipartFile file = new MockMultipartFile("file", "policy.txt", "text/plain",
                content.getBytes(StandardCharsets.UTF_8));

        var result = service.importDocument(file, "POLICY-TEST", "测试政策", "1.0",
                "CONSUMER_LOAN_STD", LocalDate.of(2026, 1, 1), null, "admin");

        assertThat(result.chunkCount()).isGreaterThan(1);
        assertThat(result.vectorSyncStatus()).isEqualTo("SYNCED");
        verify(mapper).insert(any());
        verify(mapper, atLeastOnce()).updateByDocumentIdAndVersion(any());
        verify(store).deleteDocumentVersion("POLICY-TEST", "1.0");
        verify(store, atLeastOnce()).upsert(any());
    }

    @Test
    void shouldRejectUnsupportedFileType() {
        PolicyIngestionServiceImpl service = new PolicyIngestionServiceImpl(
                mock(PolicyDocumentMapper.class), mock(PolicyKnowledgeStore.class), "collection");
        MockMultipartFile file = new MockMultipartFile("file", "policy.exe",
                "application/octet-stream", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.importDocument(file, "P", "政策", "1.0", "PRODUCT",
                LocalDate.now(), null, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持");
    }
}

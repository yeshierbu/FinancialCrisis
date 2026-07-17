package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.common.BusinessException;
import com.erbu.financialcrisis.domain.entity.PolicyDocument;
import com.erbu.financialcrisis.knowledge.PolicyChunk;
import com.erbu.financialcrisis.knowledge.PolicyKnowledgeStore;
import com.erbu.financialcrisis.mapper.PolicyDocumentMapper;
import com.erbu.financialcrisis.service.PolicyIngestionService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/** 政策文件解析、切片、主数据落库及 Qdrant 同步服务。 */
@Service
public class PolicyIngestionServiceImpl implements PolicyIngestionService {
    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 150;

    private final PolicyDocumentMapper mapper;
    private final PolicyKnowledgeStore knowledgeStore;
    private final String collection;

    public PolicyIngestionServiceImpl(PolicyDocumentMapper mapper,
                                      PolicyKnowledgeStore knowledgeStore,
                                      @Value("${knowledge.qdrant.collection:credit_policy_chunks_v4}") String collection) {
        this.mapper = mapper;
        this.knowledgeStore = knowledgeStore;
        this.collection = collection;
    }

    @Override
    public PolicyImportResult importDocument(MultipartFile file, String documentId, String title,
                                             String version, String productCode, LocalDate effectiveFrom,
                                             LocalDate effectiveTo, String createdBy) {
        validate(file, effectiveFrom, effectiveTo);
        String fileName = safeFileName(file.getOriginalFilename());
        byte[] bytes;
        String text;
        try {
            bytes = file.getBytes();
            text = extractText(fileName, bytes);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(4001, "政策文件解析失败：" + ex.getMessage());
        }
        List<String> chunks = split(text);
        if (chunks.isEmpty()) throw new BusinessException(4001, "政策文件没有可提取的文字内容");

        LocalDateTime now = LocalDateTime.now();
        PolicyDocument document = new PolicyDocument(null, documentId, title, version, productCode,
                "ACTIVE", effectiveFrom, effectiveTo, fileName, sha256(bytes), collection,
                "PENDING", createdBy == null || createdBy.isBlank() ? "admin" : createdBy, now, now);
        save(document);

        try {
            // 重传同一版本时先清理旧分片，避免文件变短后残留已经失效的向量。
            knowledgeStore.deleteDocumentVersion(documentId, version);
            for (int i = 0; i < chunks.size(); i++) {
                String chunkId = "%s-%s-%03d".formatted(documentId, version, i + 1);
                knowledgeStore.upsert(new PolicyChunk(documentId, chunkId, title,
                        "chunk-" + (i + 1), version, productCode, "ACTIVE",
                        effectiveFrom.toString(), effectiveTo == null ? null : effectiveTo.toString(), chunks.get(i)));
            }
            document.setVectorSyncStatus("SYNCED");
            document.setUpdatedAt(LocalDateTime.now());
            mapper.updateByDocumentIdAndVersion(document);
            return new PolicyImportResult(documentId, version, title, productCode,
                    fileName, chunks.size(), "SYNCED");
        } catch (RuntimeException ex) {
            document.setVectorSyncStatus("FAILED");
            document.setUpdatedAt(LocalDateTime.now());
            mapper.updateByDocumentIdAndVersion(document);
            throw new IllegalStateException("政策向量化或 Qdrant 写入失败：" + ex.getMessage(), ex);
        }
    }

    @Override
    public List<PolicyDocument> listDocuments() {
        return mapper.selectRecent();
    }

    private void save(PolicyDocument document) {
        PolicyDocument existing = mapper.selectByDocumentIdAndVersion(
                document.getDocumentId(), document.getVersion());
        if (existing == null) mapper.insert(document);
        else {
            document.setId(existing.getId());
            mapper.updateByDocumentIdAndVersion(document);
        }
    }

    private String extractText(String fileName, byte[] bytes) throws Exception {
        String extension = extension(fileName);
        if ("txt".equals(extension) || "md".equals(extension)) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if ("pdf".equals(extension)) {
            try (PDDocument document = PDDocument.load(bytes)) {
                return new PDFTextStripper().getText(document);
            }
        }
        if ("docx".equals(extension)) {
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
                StringBuilder value = new StringBuilder();
                document.getParagraphs().forEach(p -> value.append(p.getText()).append('\n'));
                document.getTables().forEach(table -> table.getRows().forEach(row ->
                        row.getTableCells().forEach(cell -> value.append(cell.getText()).append('\n'))));
                return value.toString();
            }
        }
        throw new BusinessException(4001, "仅支持 PDF、DOCX、TXT、MD 政策文件");
    }

    private List<String> split(String raw) {
        String text = raw.replace("\r\n", "\n").replace('\r', '\n')
                .replaceAll("[\\t ]+", " ").replaceAll("\n{3,}", "\n\n").trim();
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            if (end < text.length()) {
                int paragraph = text.lastIndexOf("\n\n", end);
                if (paragraph > start + CHUNK_SIZE / 2) end = paragraph;
            }
            String chunk = text.substring(start, end).trim();
            if (!chunk.isBlank()) result.add(chunk);
            if (end >= text.length()) break;
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        return result;
    }

    private void validate(MultipartFile file, LocalDate from, LocalDate to) {
        if (file == null || file.isEmpty()) throw new BusinessException(4001, "请选择政策文件");
        if (from == null) throw new BusinessException(4001, "请选择政策生效日期");
        if (to != null && to.isBefore(from)) throw new BusinessException(4001, "失效日期不能早于生效日期");
        extension(safeFileName(file.getOriginalFilename()));
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String safeFileName(String value) {
        if (value == null || value.isBlank()) return "policy.txt";
        return value.replace("\\", "/").substring(value.replace("\\", "/").lastIndexOf('/') + 1);
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("无法计算政策文件摘要", ex);
        }
    }
}

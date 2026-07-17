package com.erbu.financialcrisis.service;

import com.erbu.financialcrisis.domain.entity.PolicyDocument;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface PolicyIngestionService {
    PolicyImportResult importDocument(MultipartFile file, String documentId, String title,
                                      String version, String productCode, LocalDate effectiveFrom,
                                      LocalDate effectiveTo, String createdBy);

    List<PolicyDocument> listDocuments();

    record PolicyImportResult(String documentId, String version, String title,
                              String productCode, String fileName, int chunkCount,
                              String vectorSyncStatus) {}
}

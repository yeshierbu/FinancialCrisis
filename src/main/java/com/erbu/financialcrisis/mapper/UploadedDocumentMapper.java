package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.UploadedDocument;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 上传材料表 Mapper。
 */
@Mapper
public interface UploadedDocumentMapper {

    int insert(UploadedDocument uploadedDocument);

    int updateByDocumentId(UploadedDocument uploadedDocument);

    UploadedDocument selectByDocumentId(Long documentId);

    List<UploadedDocument> selectByApplicationId(Long applicationId);

    int deleteByDocumentId(Long documentId);
}

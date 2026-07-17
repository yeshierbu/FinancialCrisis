package com.erbu.financialcrisis.mapper;

import com.erbu.financialcrisis.domain.entity.PolicyDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PolicyDocumentMapper {
    int insert(PolicyDocument document);
    int updateByDocumentIdAndVersion(PolicyDocument document);
    PolicyDocument selectByDocumentIdAndVersion(@Param("documentId") String documentId,
                                                @Param("version") String version);
    List<PolicyDocument> selectRecent();
}

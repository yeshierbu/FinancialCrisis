package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.dto.request.SupplementRequest;
import com.erbu.financialcrisis.dto.request.UploadDocumentRequest;
import com.erbu.financialcrisis.dto.response.SupplementResponse;
import com.erbu.financialcrisis.dto.response.UploadedDocumentResponse;
import com.erbu.financialcrisis.service.DocumentService;
import org.springframework.stereotype.Service;

/**
 * 申请材料业务服务实现。
 * 第一版只保留方法骨架，后续可以接数据库、工单系统和审计日志。
 */
@Service
public class DocumentServiceImpl implements DocumentService {

    @Override
    public UploadedDocumentResponse uploadDocument(Long applicationId, UploadDocumentRequest request) {
        // TODO: 1. 入参合法性、业务单据状态前置校验
        // TODO: 2. 数据库新增/更新/查询操作（调用Mapper）
        // TODO: 3. 业务单据状态流转变更
        // TODO: 4. 同步数据到外部工单/消息系统
        // TODO: 5. 记录操作审计日志、状态变更日志
        // TODO: 6. 封装返回结果/抛出业务异常
        return null;
    }

    @Override
    public SupplementResponse submitSupplement(Long applicationId, SupplementRequest request) {
        // TODO: 1. 入参合法性、业务单据状态前置校验
        // TODO: 2. 数据库新增/更新/查询操作（调用Mapper）
        // TODO: 3. 业务单据状态流转变更
        // TODO: 4. 同步数据到外部工单/消息系统
        // TODO: 5. 记录操作审计日志、状态变更日志
        // TODO: 6. 封装返回结果/抛出业务异常
        return null;
    }
}

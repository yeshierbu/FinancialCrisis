package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.dto.request.CreateLoanApplicationRequest;
import com.erbu.financialcrisis.dto.response.ApplicationStatusResponse;
import com.erbu.financialcrisis.dto.response.LoanApplicationResponse;
import com.erbu.financialcrisis.service.LoanApplicationService;
import org.springframework.stereotype.Service;

/**
 * 贷款申请业务服务实现。
 * 第一版只保留方法骨架，后续可以接数据库、工单系统和审计日志。
 */
@Service
public class LoanApplicationServiceImpl implements LoanApplicationService {

    @Override
    public LoanApplicationResponse createApplication(CreateLoanApplicationRequest request) {
        // TODO: 1. 入参合法性、业务单据状态前置校验
        // TODO: 2. 数据库新增/更新/查询操作（调用Mapper）
        // TODO: 3. 业务单据状态流转变更
        // TODO: 4. 同步数据到外部工单/消息系统
        // TODO: 5. 记录操作审计日志、状态变更日志
        // TODO: 6. 封装返回结果/抛出业务异常
        return null;
    }

    @Override
    public LoanApplicationResponse getApplication(Long applicationId) {
        // TODO: 1. 入参合法性、业务单据状态前置校验
        // TODO: 2. 数据库新增/更新/查询操作（调用Mapper）
        // TODO: 3. 业务单据状态流转变更
        // TODO: 4. 同步数据到外部工单/消息系统
        // TODO: 5. 记录操作审计日志、状态变更日志
        // TODO: 6. 封装返回结果/抛出业务异常
        return null;
    }

    @Override
    public ApplicationStatusResponse getApplicationStatus(Long applicationId) {
        // TODO: 1. 入参合法性、业务单据状态前置校验
        // TODO: 2. 数据库新增/更新/查询操作（调用Mapper）
        // TODO: 3. 业务单据状态流转变更
        // TODO: 4. 同步数据到外部工单/消息系统
        // TODO: 5. 记录操作审计日志、状态变更日志
        // TODO: 6. 封装返回结果/抛出业务异常
        return null;
    }
}

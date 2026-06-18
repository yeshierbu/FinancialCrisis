package com.erbu.financialcrisis.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 创建贷款申请请求。
 */
public class CreateLoanApplicationRequest {

    @NotBlank(message = "产品编码不能为空")
    private String productCode;

    @NotBlank(message = "申请人姓名不能为空")
    private String applicantName;

    @NotBlank(message = "身份证号不能为空")
    private String idCardNo;

    @NotBlank(message = "手机号不能为空")
    private String mobile;

    @NotNull(message = "申请金额不能为空")
    @DecimalMin(value = "1000", message = "申请金额不能低于1000")
    private BigDecimal loanAmount;

    @NotNull(message = "申请期数不能为空")
    @Min(value = 1, message = "申请期数必须大于0")
    private Integer loanTerm;

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getIdCardNo() {
        return idCardNo;
    }

    public void setIdCardNo(String idCardNo) {
        this.idCardNo = idCardNo;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }

    public Integer getLoanTerm() {
        return loanTerm;
    }

    public void setLoanTerm(Integer loanTerm) {
        this.loanTerm = loanTerm;
    }
}

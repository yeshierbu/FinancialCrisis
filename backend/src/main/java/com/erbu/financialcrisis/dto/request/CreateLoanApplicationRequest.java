package com.erbu.financialcrisis.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 创建贷款申请请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLoanApplicationRequest {

    @NotBlank(message = "产品编码不能为空")
    private String productCode;

    @NotBlank(message = "申请人姓名不能为空")
    @Size(max = 8, message = "申请人姓名不能超过8个汉字")
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

    private String employmentType;
    private String companyName;
    private Integer workYears;
    private String channelCode;
}

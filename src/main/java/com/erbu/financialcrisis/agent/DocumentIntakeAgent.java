package com.erbu.financialcrisis.agent;

import com.erbu.financialcrisis.domain.entity.LoanApplication;
import org.springframework.stereotype.Component;

/**
 * 信息采集 Agent。
 * 用于承接 OCR、材料解析、字段标准化和补件判断。
 */
@Component
public class DocumentIntakeAgent {

    public void collectAndParse(LoanApplication application) {
        // TODO: 调用 OCR 工具、解析流水与征信报告、识别缺失材料
        application.setCurrentStep("材料解析完成");
    }
}

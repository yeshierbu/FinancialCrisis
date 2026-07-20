package com.erbu.financialcrisis.service;

import com.erbu.financialcrisis.messaging.ApprovalStartMessage;

public interface ApprovalConsumeService {
    String tryStart(ApprovalStartMessage message, String consumerName);
    void complete(String eventId, String claimToken);
    void fail(ApprovalStartMessage message, String claimToken, String error);
}

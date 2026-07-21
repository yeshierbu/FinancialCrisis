package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.dto.request.ManualReviewRequest;
import com.erbu.financialcrisis.service.ManualReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:manual_review_concurrency;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "llm.api-key=test-api-key"
})
class ManualReviewConcurrencyTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired ManualReviewService service;

    @Test void approveAndRejectRaceProducesOneDecision() throws Exception {
        jdbc.update("INSERT INTO loan_application(application_no,product_code,applicant_name,id_card_no,mobile,loan_amount,loan_term,status,current_step,owner_username,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                "CONCURRENT-1", "P", "用户", "ID", "13800000000", new BigDecimal("10000"), 12,
                "MANUAL_REVIEW", "review", "user");
        Long applicationId = jdbc.queryForObject("SELECT id FROM loan_application WHERE application_no='CONCURRENT-1'", Long.class);
        jdbc.update("INSERT INTO manual_review_ticket(application_id,ticket_no,review_status,assigned_to,created_at,updated_at) VALUES(?,?,'PENDING','reviewer',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                applicationId, "MR-CONCURRENT-1");

        ManualReviewRequest request = new ManualReviewRequest();
        request.setReviewComment("concurrent");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> approve = () -> run(start, () -> service.approve(applicationId, request));
        Callable<Boolean> reject = () -> run(start, () -> service.reject(applicationId, request));
        List<Future<Boolean>> futures = List.of(executor.submit(approve), executor.submit(reject));
        start.countDown();
        int successes = 0;
        for (Future<Boolean> future : futures) if (future.get(10, TimeUnit.SECONDS)) successes++;
        executor.shutdownNow();

        assertEquals(1, successes);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM approval_decision WHERE application_id=?", Integer.class, applicationId));
    }

    private boolean run(CountDownLatch start, Runnable action) {
        try { start.await(); action.run(); return true; }
        catch (Exception expectedConflict) { return false; }
    }
}

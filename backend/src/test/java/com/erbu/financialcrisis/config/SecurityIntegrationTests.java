package com.erbu.financialcrisis.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "security.enabled=true",
        "llm.api-key=test-api-key",
        "spring.datasource.url=jdbc:h2:mem:security_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class SecurityIntegrationTests {
    @Autowired MockMvc mvc;

    @Test void apiRequiresAuthenticationAndAcceptsDatabaseAccount() throws Exception {
        mvc.perform(get("/api/loan/applications")).andExpect(status().isUnauthorized());
        String basic = Base64.getEncoder().encodeToString("user:user123".getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/api/loan/applications").header("Authorization", "Basic " + basic))
                .andExpect(status().isOk());
    }

    @Test void normalUserCannotAccessAdminApi() throws Exception {
        String basic = Base64.getEncoder().encodeToString("user:user123".getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/api/admin/reviews/pending").header("Authorization", "Basic " + basic))
                .andExpect(status().isForbidden());
    }
}

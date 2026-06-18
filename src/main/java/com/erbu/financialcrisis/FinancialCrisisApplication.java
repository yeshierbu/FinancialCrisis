package com.erbu.financialcrisis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目启动类。
 * 这一层只负责启动 Spring Boot 容器，不承担任何业务逻辑。
 */
@SpringBootApplication
public class FinancialCrisisApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancialCrisisApplication.class, args);
    }
}

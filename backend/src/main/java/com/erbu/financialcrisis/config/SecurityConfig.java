package com.erbu.financialcrisis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.erbu.financialcrisis.mapper.SystemAccountMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            @Value("${security.enabled:true}") boolean enabled) throws Exception {
        http.csrf(csrf -> csrf.disable());
        if (!enabled) return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
        return http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/admin/policy-knowledge/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "REVIEWER")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults()).build();
    }

    @Bean
    UserDetailsService users(SystemAccountMapper accounts) {
        return username -> {
            var account = accounts.selectByUsername(username);
            if (account == null) throw new UsernameNotFoundException("账号不存在");
            return User.withUsername(account.getUsername()).password(account.getPasswordHash())
                    .roles(account.getRoleCode()).disabled(!"ACTIVE".equals(account.getAccountStatus())).build();
        };
    }

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}

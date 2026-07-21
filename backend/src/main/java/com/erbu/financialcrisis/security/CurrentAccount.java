package com.erbu.financialcrisis.security;

import com.erbu.financialcrisis.common.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class CurrentAccount {
    private final boolean securityEnabled;
    public CurrentAccount(@Value("${security.enabled:true}") boolean securityEnabled) { this.securityEnabled = securityEnabled; }
    public String username() {
        if (!securityEnabled) return "user";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
            throw new BusinessException(4010, "未登录");
        return auth.getName();
    }
    public boolean privileged() {
        if (!securityEnabled) return false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_REVIEWER"));
    }
}

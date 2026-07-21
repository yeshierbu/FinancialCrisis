package com.erbu.financialcrisis.domain.entity;

import lombok.Data;

@Data
public class SystemAccount {
    private Long id;
    private String username;
    private String passwordHash;
    private String roleCode;
    private String displayName;
    private String accountStatus;
}

package com.vynex.model;

/**
 * Plain Java object representing a user.
 * No JPA annotations — mapped manually via raw JDBC in UserRepository.
 */
public class User {

    private Long id;
    private String fullName;
    private String email;
    private String password;
    private String accountType;
    private String companyName;
    private String businessType;
    private Integer companySize;
    private String industry;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public Integer getCompanySize() { return companySize; }
    public void setCompanySize(Integer companySize) { this.companySize = companySize; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
}

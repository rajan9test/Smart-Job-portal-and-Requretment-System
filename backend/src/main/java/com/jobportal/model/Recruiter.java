package com.jobportal.model;

public class Recruiter extends User {

    private Long companyId;
    private String designation;

    public Recruiter(String name, String email, String phone, String passwordHash, Long companyId) {
        super(name, email, phone, passwordHash, Role.RECRUITER);
        this.companyId = companyId;
    }

    @Override
    public String showDashboard() {
        return "Recruiter dashboard for %s (company #%d)".formatted(getName(), companyId);
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }
}

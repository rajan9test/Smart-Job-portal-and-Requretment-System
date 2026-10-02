package com.jobportal.model;

public class Admin extends User {

    public Admin(String name, String email, String phone, String passwordHash) {
        super(name, email, phone, passwordHash, Role.ADMIN);
    }

    @Override
    public String showDashboard() {
        return "Admin dashboard for " + getName();
    }
}

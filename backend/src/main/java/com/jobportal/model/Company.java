package com.jobportal.model;

import java.util.Objects;

public class Company implements Identifiable {

    private Long id;
    private String name;
    private String location;
    private String website;

    public Company(String name, String location, String website) {
        this.name = Objects.requireNonNull(name, "name");
        this.location = location;
        this.website = website;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getWebsite() {
        return website;
    }

    @Override
    public String toString() {
        return "Company{id=%d, name='%s'}".formatted(id, name);
    }
}

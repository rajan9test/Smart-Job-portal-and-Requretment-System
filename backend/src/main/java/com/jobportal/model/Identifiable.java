package com.jobportal.model;

/**
 * Anything that can be stored in a repository has a Long id assigned on first save.
 */
public interface Identifiable {
    Long getId();

    void setId(Long id);
}

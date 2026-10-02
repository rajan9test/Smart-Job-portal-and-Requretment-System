package com.jobportal.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic persistence contract. Services depend on these interfaces only, so the in-memory
 * implementations can be swapped for JDBC ones in Phase 2 without touching business logic.
 */
public interface Repository<T, ID> {

    /** Inserts (assigning an id) or updates the entity, and returns it. */
    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    boolean existsById(ID id);

    void deleteById(ID id);

    long count();
}

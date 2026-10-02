package com.jobportal.repository.inmemory;

import com.jobportal.model.Identifiable;
import com.jobportal.repository.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Thread-safe map-backed repository. ConcurrentHashMap + AtomicLong because the notification
 * worker threads read and write alongside the main thread.
 */
public abstract class InMemoryRepository<T extends Identifiable> implements Repository<T, Long> {

    protected final Map<Long, T> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public T save(T entity) {
        if (entity.getId() == null) {
            entity.setId(sequence.incrementAndGet());
        }
        store.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<T> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public boolean existsById(Long id) {
        return store.containsKey(id);
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    @Override
    public long count() {
        return store.size();
    }

    protected List<T> findWhere(Predicate<T> predicate) {
        return store.values().stream().filter(predicate).toList();
    }
}

package com.jobportal.repository.inmemory;

import com.jobportal.model.Role;
import com.jobportal.model.User;
import com.jobportal.repository.UserRepository;

import java.util.List;
import java.util.Optional;

public class InMemoryUserRepository extends InMemoryRepository<User> implements UserRepository {

    @Override
    public Optional<User> findByEmail(String email) {
        String normalized = email.trim().toLowerCase();
        return store.values().stream().filter(u -> u.getEmail().equals(normalized)).findFirst();
    }

    @Override
    public List<User> findByRole(Role role) {
        return findWhere(u -> u.getRole() == role);
    }
}

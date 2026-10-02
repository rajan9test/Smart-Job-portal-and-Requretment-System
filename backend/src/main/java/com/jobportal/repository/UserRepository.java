package com.jobportal.repository;

import com.jobportal.model.Role;
import com.jobportal.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends Repository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);
}

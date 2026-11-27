package com.hireflow.auth.repository;

import java.util.Optional;
import java.util.UUID;

import com.hireflow.auth.entity.User;
import com.hireflow.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Callers pass the email already lowercased. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);
}

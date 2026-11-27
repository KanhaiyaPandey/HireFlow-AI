package com.hireflow.auth.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import com.hireflow.auth.entity.User;
import com.hireflow.security.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Callers pass the email already lowercased. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    Optional<User> findByIdAndCompanyId(UUID id, UUID companyId);

    @Query("""
            select u from User u
            where u.companyId = :companyId
              and u.role in :roles
              and (lower(u.name) like :pattern escape '!' or lower(u.email) like :pattern escape '!')
            """)
    Page<User> searchCompanyUsers(UUID companyId, Collection<Role> roles, String pattern, Pageable pageable);
}

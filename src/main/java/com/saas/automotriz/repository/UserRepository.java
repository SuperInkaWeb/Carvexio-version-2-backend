package com.saas.automotriz.repository;

import com.saas.automotriz.model.Role;
import com.saas.automotriz.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findFirstByEmail(String email);
    Optional<User> findFirstByEmailIgnoreCase(String email);
    List<User> findByRole(Role role);
    Long countByRole(Role role);
}

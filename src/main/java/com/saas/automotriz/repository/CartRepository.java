package com.saas.automotriz.repository;

import com.saas.automotriz.model.Cart;
import com.saas.automotriz.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByClient(User client);
}

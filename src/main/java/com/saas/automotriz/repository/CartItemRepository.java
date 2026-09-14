package com.saas.automotriz.repository;

import com.saas.automotriz.model.Cart;
import com.saas.automotriz.model.CartItem;
import com.saas.automotriz.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
}
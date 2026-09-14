package com.saas.automotriz.controller;

import com.saas.automotriz.request.AddToCartRequest;
import com.saas.automotriz.request.ApplyDiscountRequest;
import com.saas.automotriz.dto.CartDTO;
import com.saas.automotriz.dto.CartItemDTO;
import com.saas.automotriz.model.*;
import com.saas.automotriz.repository.CartItemRepository;
import com.saas.automotriz.repository.CartRepository;
import com.saas.automotriz.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    // ver carrito
    @GetMapping
    public ResponseEntity<CartDTO> getCart(@AuthenticationPrincipal User user) {
        Cart cart = getOrCreateCart(user);
        return ResponseEntity.ok(toDTO(cart));
    }

    // agregar producto
    @PostMapping("/items")
    public ResponseEntity<CartDTO> addItem(@AuthenticationPrincipal User user,
                                           @RequestBody AddToCartRequest request) {
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser mayor que cero.");
        }

        int stockAvailable = product.getStock() == null ? 0 : product.getStock();
        Optional<CartItem> existing = cartItemRepository.findByCartAndProduct(cart, product);
        int quantityAlreadyInCart = existing.map(CartItem::getQuantity).orElse(0);
        int requestedTotal = quantityAlreadyInCart + request.getQuantity();

        // Validar el total acumulado para impedir que se agregue más de lo disponible.
        if (requestedTotal > stockAvailable) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo hay " + stockAvailable + " unidades disponibles de " + product.getName()
                            + ". Ya tienes " + quantityAlreadyInCart + " en tu carrito.");
        }

        // si ya existe el producto en el carrito, sumar cantidad
        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + request.getQuantity());
            cartItemRepository.save(existing.get());
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
        }

        return ResponseEntity.ok(toDTO(cartRepository.findById(cart.getId()).get()));
    }

    // eliminar producto del carrito
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDTO> removeItem(@AuthenticationPrincipal User user,
                                              @PathVariable Long itemId) {
        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item no encontrado"));

        if (!item.getCart().getId().equals(cart.getId())) {
            return ResponseEntity.status(403).build();
        }

        cartItemRepository.delete(item);
        return ResponseEntity.ok(toDTO(cartRepository.findById(cart.getId()).get()));
    }

    // aplicar descuento
    @PutMapping("/discount")
    public ResponseEntity<CartDTO> applyDiscount(@AuthenticationPrincipal User user,
                                                 @RequestBody ApplyDiscountRequest request) {
        if (request.getDiscount() < 0 || request.getDiscount() > 100) {
            return ResponseEntity.badRequest().build();
        }

        Cart cart = getOrCreateCart(user);
        cart.setDiscount(request.getDiscount());
        return ResponseEntity.ok(toDTO(cartRepository.save(cart)));
    }

    // vaciar carrito
    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal User user) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        cart.setDiscount(0.0);
        cartRepository.save(cart);
        return ResponseEntity.noContent().build();
    }

    // obtener o crear carrito automáticamente
    private Cart getOrCreateCart(User user) {
        return cartRepository.findByClient(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setClient(user);
                    return cartRepository.save(newCart);
                });
    }

    private CartDTO toDTO(Cart cart) {
        List<CartItemDTO> itemDTOs = cart.getItems().stream().map(item -> {
            Product product = item.getProduct();
            CartItemDTO dto = new CartItemDTO();
            dto.setId(item.getId());
            dto.setProductId(product.getId());
            dto.setProductName(product.getName());
            dto.setPrice(product.getPrice());
            dto.setQuantity(item.getQuantity());
            dto.setStock(product.getStock() == null ? 0 : product.getStock());
            dto.setSubtotal(product.getPrice() * item.getQuantity());
            dto.setDeliveryAvailable(Boolean.TRUE.equals(product.getDeliveryAvailable()));
            dto.setBusinessName(product.getBusiness() != null ? product.getBusiness().getName() : "Tienda automotriz");
            return dto;
        }).toList();

        double subtotal = itemDTOs.stream().mapToDouble(CartItemDTO::getSubtotal).sum();
        double discountAmount = subtotal * (cart.getDiscount() / 100);
        double total = subtotal - discountAmount;

        CartDTO dto = new CartDTO();
        dto.setId(cart.getId());
        dto.setItems(itemDTOs);
        dto.setDiscount(cart.getDiscount());
        dto.setSubtotal(subtotal);
        dto.setDiscountAmount(discountAmount);
        dto.setTotal(total);
        return dto;
    }
}

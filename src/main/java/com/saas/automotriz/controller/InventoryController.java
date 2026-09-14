package com.saas.automotriz.controller;

import com.saas.automotriz.dto.ProductDTO;
import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.Product;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.BusinessRepository;
import com.saas.automotriz.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final ProductRepository productRepository;
    private final BusinessRepository businessRepository;

    // ver todo el inventario
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getInventory(@AuthenticationPrincipal User user,
                                                         @RequestParam(required = false) Long localId) {
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        List<Product> list;
        if (localId != null) {
            list = productRepository.findByBusinessAndBranchIdAndActiveTrue(business, localId);
        } else {
            list = productRepository.findByBusinessAndActiveTrue(business);
        }
        List<ProductDTO> products = list.stream().map(this::toDTO).toList();
        return ResponseEntity.ok(products);
    }

    // alertas de stock bajo (por defecto avisa cuando stock <= 5)
    @GetMapping("/alerts")
    public ResponseEntity<List<ProductDTO>> getLowStock(@AuthenticationPrincipal User user,
                                                        @RequestParam(defaultValue = "5") Integer minStock,
                                                        @RequestParam(required = false) Long localId) {
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        List<Product> list;
        if (localId != null) {
            list = productRepository.findLowStockByBusinessAndBranchId(business, localId, minStock);
        } else {
            list = productRepository.findLowStockByBusiness(business, minStock);
        }
        List<ProductDTO> products = list.stream().map(this::toDTO).toList();
        return ResponseEntity.ok(products);
    }

    // actualizar stock de un producto
    @PutMapping("/{productId}/stock")
    public ResponseEntity<ProductDTO> updateStock(@AuthenticationPrincipal User user,
                                                  @PathVariable Long productId,
                                                  @RequestParam Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (!product.getBusiness().getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        product.setStock(quantity);
        return ResponseEntity.ok(toDTO(productRepository.save(product)));
    }

    private ProductDTO toDTO(Product p) {
        ProductDTO dto = new ProductDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setPrice(p.getPrice());
        dto.setStock(p.getStock());
        dto.setCategory(p.getCategory());
        dto.setBrand(p.getBrand());
        dto.setPhotoUrl(p.getPhotoUrl());
        dto.setBusinessName(p.getBusiness().getName());
        return dto;
    }
}

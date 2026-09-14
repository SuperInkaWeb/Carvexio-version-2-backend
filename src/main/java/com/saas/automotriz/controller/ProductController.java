package com.saas.automotriz.controller;

import com.saas.automotriz.dto.ProductDTO;
import com.saas.automotriz.request.ProductRequest;
import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.Product;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.BranchRepository;
import com.saas.automotriz.repository.BusinessRepository;
import com.saas.automotriz.repository.ProductRepository;
import com.saas.automotriz.service.CloudinaryService;
import com.saas.automotriz.service.PlanLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final BusinessRepository businessRepository;
    private final CloudinaryService cloudinaryService;
    private final BranchRepository branchRepository;
    private final PlanLimitService planLimitService;

    // negocio ve sus productos
    @GetMapping("/my")
    public ResponseEntity<List<ProductDTO>> getMyProducts(@AuthenticationPrincipal User user,
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

    // negocio crea producto
    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@AuthenticationPrincipal User user,
                                                    @RequestBody ProductRequest request) {
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        planLimitService.checkProductLimit(business, productRepository.countByBusinessAndActiveTrue(business), 1);
        Product product = new Product();
        product.setBusiness(business);
        if (request.getLocalId() != null) {
            branchRepository.findById(request.getLocalId()).ifPresent(product::setBranch);
        }
        applyRequest(product, request);
        return ResponseEntity.ok(toDTO(productRepository.save(product)));
    }

    // negocio crea productos en lote
    @PostMapping("/bulk")
    public ResponseEntity<List<ProductDTO>> createProductsBulk(@AuthenticationPrincipal User user,
                                                              @RequestBody List<ProductRequest> requests) {
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        planLimitService.checkProductLimit(business, productRepository.countByBusinessAndActiveTrue(business), requests.size());
        List<Product> products = requests.stream().map(request -> {
            Product product = new Product();
            product.setBusiness(business);
            if (request.getLocalId() != null) {
                branchRepository.findById(request.getLocalId()).ifPresent(product::setBranch);
            }
            applyRequest(product, request);
            return product;
        }).toList();
        List<Product> saved = productRepository.saveAll(products);
        return ResponseEntity.ok(saved.stream().map(this::toDTO).toList());
    }

    // negocio edita producto
    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@AuthenticationPrincipal User user,
                                                    @PathVariable Long id,
                                                    @RequestBody ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (!product.getBusiness().getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        if (request.getLocalId() != null) {
            branchRepository.findById(request.getLocalId()).ifPresent(product::setBranch);
        }
        applyRequest(product, request);
        return ResponseEntity.ok(toDTO(productRepository.save(product)));
    }

    // negocio elimina producto (soft delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@AuthenticationPrincipal User user,
                                              @PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (!product.getBusiness().getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        product.setActive(false);
        productRepository.save(product);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {

        List<Product> results;

        if (name != null) {
            results = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(name);
        } else if (category != null) {
            results = productRepository.findByCategoryIgnoreCaseAndActiveTrue(category);
        } else if (brand != null) {
            results = productRepository.findByBrandIgnoreCaseAndActiveTrue(brand);
        } else if (minPrice != null && maxPrice != null) {
            results = productRepository.findByPriceBetweenAndActiveTrue(minPrice, maxPrice);
        } else {
            results = productRepository.findAll()
                    .stream().filter(p -> p.getActive()).toList();
        }

        return ResponseEntity.ok(results.stream().map(this::toDTO).toList());
    }

    @PutMapping("/{id}/photo")
    public ResponseEntity<ProductDTO> uploadPhoto(@AuthenticationPrincipal User user,
                                                  @PathVariable Long id,
                                                  @RequestParam MultipartFile file) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (!product.getBusiness().getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        String url = cloudinaryService.uploadImage(file);
        product.setPhotoUrl(url);
        return ResponseEntity.ok(toDTO(productRepository.save(product)));
    }

    // PARA EL MARKETPLACE: Obtener productos de un negocio
    @GetMapping("/public/business/{businessId}")
    public ResponseEntity<List<ProductDTO>> getPublicProducts(@PathVariable Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));

        List<ProductDTO> products = productRepository.findByBusinessAndActiveTrue(business)
                .stream().map(this::toDTO).toList();

        return ResponseEntity.ok(products);
    }


    private void applyRequest(Product product, ProductRequest r) {
        if (r.getName() != null) product.setName(r.getName());
        if (r.getDescription() != null) product.setDescription(r.getDescription());
        if (r.getPrice() != null) product.setPrice(r.getPrice());
        if (r.getStock() != null) product.setStock(r.getStock());
        if (r.getCategory() != null) product.setCategory(r.getCategory());
        if (r.getBrand() != null) product.setBrand(r.getBrand());
        if (r.getSupplier() != null) product.setSupplier(r.getSupplier());
        if (r.getIgv() != null) product.setIgv(r.getIgv());
        if (r.getDeliveryAvailable() != null) product.setDeliveryAvailable(r.getDeliveryAvailable());
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
        dto.setIgv(p.getIgv());
        dto.setDeliveryAvailable(Boolean.TRUE.equals(p.getDeliveryAvailable()));
        if (p.getBranch() != null) dto.setLocalId(p.getBranch().getId());
        return dto;
    }
}

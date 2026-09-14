package com.saas.automotriz.controller;

import com.saas.automotriz.dto.OrderDTO;
import com.saas.automotriz.dto.OrderItemDTO;
import com.saas.automotriz.model.*;
import com.saas.automotriz.repository.*;
import com.saas.automotriz.request.CheckoutRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BusinessRepository businessRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // 1. Checkout: Convertir el carrito actual en uno o varios pedidos (agrupados por negocio)
    @PostMapping("/checkout")
    @Transactional
    public ResponseEntity<List<OrderDTO>> checkout(@AuthenticationPrincipal User user,
                                                  @RequestBody CheckoutRequest request) {
        Cart cart = cartRepository.findByClient(user)
                .orElseThrow(() -> new RuntimeException("No tienes un carrito activo"));

        if (cart.getItems().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Filtramos items del carrito si se especificaron IDs (Compra seleccionada)
        List<CartItem> itemsToProcess;
        if (request.getItemIds() != null && !request.getItemIds().isEmpty()) {
            itemsToProcess = cart.getItems().stream()
                    .filter(item -> request.getItemIds().contains(item.getId()))
                    .toList();
        } else {
            itemsToProcess = cart.getItems();
        }

        if (itemsToProcess.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }

        DeliveryMethod deliveryMethod = request.getDeliveryMethod() == null
                ? DeliveryMethod.PICKUP
                : request.getDeliveryMethod();
        boolean deliveryAvailable = itemsToProcess.stream()
                .allMatch(item -> Boolean.TRUE.equals(item.getProduct().getDeliveryAvailable()));
        if (deliveryMethod == DeliveryMethod.DELIVERY && !deliveryAvailable) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El envÃ­o a domicilio no estÃ¡ disponible para todos los productos seleccionados.");
        }
        if (deliveryMethod == DeliveryMethod.DELIVERY
                && (request.getAddress() == null || request.getAddress().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ingresa una direcciÃ³n para el envÃ­o a domicilio.");
        }

        // Agrupamos los items a procesar por el negocio que los vende
        Map<Business, List<CartItem>> itemsByBusiness = itemsToProcess.stream()
                .collect(Collectors.groupingBy(item -> item.getProduct().getBusiness()));

        List<Order> savedOrders = new ArrayList<>();
        int rewardDiscount = user.getActiveRewardDiscount() == null ? 0 : user.getActiveRewardDiscount();
        double cartDiscount = cart.getDiscount() == null ? 0.0 : cart.getDiscount();
        double selectedSubtotal = itemsToProcess.stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity()).sum();
        boolean rewardCanApply = (rewardDiscount == 5 && selectedSubtotal >= 200)
                || (rewardDiscount == 10 && selectedSubtotal >= 500);

        for (Map.Entry<Business, List<CartItem>> entry : itemsByBusiness.entrySet()) {
            Business biz = entry.getKey();
            List<CartItem> cartItemsForThisOrder = entry.getValue();

            Order order = new Order();
            order.setClient(user);
            order.setBusiness(biz);
            order.setStatus(OrderStatus.PENDING);
            order.setDeliveryMethod(deliveryMethod);
            order.setAddress(request.getAddress());
            order.setPhone(request.getPhone());
            order.setNotes(request.getNotes());

            double total = 0;
            List<OrderItem> orderItems = new ArrayList<>();

            for (CartItem ci : cartItemsForThisOrder) {
                OrderItem oi = new OrderItem();
                oi.setOrder(order);
                oi.setProduct(ci.getProduct());
                oi.setQuantity(ci.getQuantity());
                oi.setPriceAtPurchase(ci.getProduct().getPrice());
                oi.setSubtotal(ci.getProduct().getPrice() * ci.getQuantity());
                total += oi.getSubtotal();
                orderItems.add(oi);
                
                // Actualizamos stock del producto
                Product p = ci.getProduct();
                if (p.getStock() < ci.getQuantity()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Stock insuficiente para: " + p.getName()
                                    + ". Disponible: " + p.getStock()
                                    + ", solicitado: " + ci.getQuantity() + ".");
                }
                p.setStock(p.getStock() - ci.getQuantity());
                productRepository.save(p);
            }

            order.setTotalAmount(total);
            double cartDiscountAmount = Math.round(total * cartDiscount) / 100.0;
            double rewardDiscountAmount = rewardCanApply
                    ? Math.min(Math.round((total - cartDiscountAmount) * rewardDiscount) / 100.0, rewardDiscount == 5 ? 20.0 : 50.0)
                    : 0.0;
            double discountAmount = cartDiscountAmount + rewardDiscountAmount;
            order.setDiscountAmount(discountAmount);
            order.setPaidAmount(total - discountAmount);
            order.setItems(orderItems);
            savedOrders.add(orderRepository.save(order));
        }

        // Vaciamos solo los items procesados del carrito
        cartItemRepository.deleteAll(itemsToProcess);

        // Si el carrito queda vacÃ­o, reseteamos el descuento
        if (cart.getItems().size() <= itemsToProcess.size()) {
            cart.setDiscount(0.0);
        }
        cartRepository.save(cart);

        if (rewardDiscount > 0 && rewardCanApply) {
            user.setActiveRewardDiscount(0);
            userRepository.save(user);
        }

        return ResponseEntity.ok(savedOrders.stream().map(this::toDTO).toList());
    }

    // 2. Cliente ve su historial de compras
    @GetMapping("/my")
    public ResponseEntity<List<OrderDTO>> getMyOrders(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderRepository.findByClientOrderByCreatedAtDesc(user)
                .stream().map(this::toDTO).toList());
    }

    // 3. Negocio ve los pedidos que le han hecho
    @GetMapping("/business")
    public ResponseEntity<List<OrderDTO>> getBusinessOrders(@AuthenticationPrincipal User user,
                                                             @RequestParam(required = false) Long localId) {
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("No eres dueÃ±o de un negocio registrado"));
        
        List<Order> orders = localId != null
                ? orderRepository.findByBusinessAndBranchIdOrderByCreatedAtDesc(business, localId)
                : orderRepository.findByBusinessOrderByCreatedAtDesc(business);

        return ResponseEntity.ok(orders.stream().map(this::toDTO).toList());
    }

    // 4. Negocio actualiza el estado del pedido
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateStatus(@AuthenticationPrincipal User user,
                                                @PathVariable Long id,
                                                @RequestParam OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        // Validar que el usuario sea el dueÃ±o del negocio que recibiÃ³ el pedido
        if (!order.getBusiness().getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        DeliveryMethod method = order.getDeliveryMethod() == null ? DeliveryMethod.PICKUP : order.getDeliveryMethod();
        if (method == DeliveryMethod.PICKUP && status == OrderStatus.SHIPPED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Este pedido es para recojo en tienda y no puede marcarse como envÃ­o.");
        }
        if (method == DeliveryMethod.DELIVERY && status == OrderStatus.READY_FOR_PICKUP) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Este pedido es a domicilio y no puede marcarse como listo para recojo.");
        }

        order.setStatus(status);
        return ResponseEntity.ok(toDTO(orderRepository.save(order)));
    }

private OrderDTO toDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setClientId(order.getClient().getId());
        dto.setClientName(order.getClient().getName());
        dto.setBusinessId(order.getBusiness().getId());
        dto.setBusinessName(order.getBusiness().getName());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setDiscountAmount(order.getDiscountAmount() == null ? 0.0 : order.getDiscountAmount());
        dto.setPaidAmount(order.getPaidAmount() == null ? order.getTotalAmount() : order.getPaidAmount());
        dto.setStatus(order.getStatus().name());
        dto.setDeliveryMethod(order.getDeliveryMethod() == null
                ? DeliveryMethod.PICKUP.name()
                : order.getDeliveryMethod().name());
        dto.setAddress(order.getAddress());
        dto.setPhone(order.getPhone());
        dto.setNotes(order.getNotes());
        dto.setCreatedAt(order.getCreatedAt());
        
        dto.setItems(order.getItems().stream().map(item -> {
            OrderItemDTO itemDTO = new OrderItemDTO();
            itemDTO.setId(item.getId());
            itemDTO.setProductId(item.getProduct().getId());
            itemDTO.setProductName(item.getProduct().getName());
            itemDTO.setQuantity(item.getQuantity());
            itemDTO.setPriceAtPurchase(item.getPriceAtPurchase());
            itemDTO.setSubtotal(item.getSubtotal());
            return itemDTO;
        }).toList());
        
        return dto;
    }

    private void grantRewardPoints(Order order) {
        if (Boolean.TRUE.equals(order.getRewardPointsGranted())) return;
        int points = order.getItems().stream()
                .mapToInt(item -> pointsForProduct(item.getPriceAtPurchase()) * item.getQuantity()).sum();
        User client = order.getClient();
        client.setRewardPoints((client.getRewardPoints() == null ? 0 : client.getRewardPoints()) + points);
        order.setRewardPointsGranted(true);
        userRepository.save(client);
    }

    private int pointsForProduct(double price) {
        if (price < 50) return 5;
        if (price < 100) return 10;
        if (price < 500) return 25;
        if (price < 1000) return 60;
        return 120;
    }
}


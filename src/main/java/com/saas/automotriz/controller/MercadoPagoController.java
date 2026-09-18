package com.saas.automotriz.controller;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.OrderStatus;
import com.saas.automotriz.model.Order;
import com.saas.automotriz.model.OrderItem;
import com.saas.automotriz.model.Plan;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.BusinessRepository;
import com.saas.automotriz.repository.OrderRepository;
import com.saas.automotriz.repository.PlanRepository;
import com.saas.automotriz.repository.UserRepository;
import com.saas.automotriz.request.OrderRequest;
import com.saas.automotriz.request.PlanPaymentRequest;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.mercadopago.exceptions.MPApiException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@ConditionalOnProperty(
        name = "mercadopago.enabled",
        havingValue = "true",
        matchIfMissing = false
)

public class MercadoPagoController {

    private static final String PLAN_REFERENCE_PREFIX = "PLAN:";

    private final OrderRepository orderRepository;
    private final PlanRepository planRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;

    public MercadoPagoController(OrderRepository orderRepository, PlanRepository planRepository,
                                  BusinessRepository businessRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.planRepository = planRepository;
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
    }

    @Value("${mercadopago.access.token}")
    private String accessToken;

    @Value("${mercadopago.collector.id}")
    private Long collectorId;

    @Value("${mercadopago.webhook.secret}")
    private String webhookSecret;

    @Value("${app.backend-url:}")
    private String backendUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        // Inicializa el SDK con tu token apenas inicia el servidor
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    @PostMapping("/create-preference")
    public String createPreference(@AuthenticationPrincipal User user, @RequestBody OrderRequest order) {
        try {
            Order persistedOrder = order.getOrderId() == null ? null : orderRepository.findById(order.getOrderId()).orElse(null);
            if (persistedOrder == null) return "Error: pedido no encontrado";
            if (persistedOrder.getClient() == null || !persistedOrder.getClient().getId().equals(user.getId())) return "Error: no autorizado";
            if (persistedOrder.getStatus() != OrderStatus.PENDING) return "Error: el pedido no esta pendiente de pago";
            // 1. Forzamos el uso del token directamente en la peticiÃ³n (mÃ¡s seguro)
            com.mercadopago.core.MPRequestOptions options = com.mercadopago.core.MPRequestOptions.builder()
                    .accessToken(accessToken)
                    .build();

            PreferenceClient client = new PreferenceClient();

            // 2. Agregamos el item con la moneda explÃ­cita (PEN para Soles)
            Double orderAmount = persistedOrder.getPaidAmount() == null ? persistedOrder.getTotalAmount() : persistedOrder.getPaidAmount();
            if (orderAmount == null || orderAmount <= 0) return "Error: monto de pedido invalido";
            List<PreferenceItemRequest> items = new ArrayList<>();
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title("Pedido #" + persistedOrder.getId() + " - " + persistedOrder.getBusiness().getName())
                    .quantity(1)
                    .unitPrice(BigDecimal.valueOf(orderAmount))
                    .currencyId("PEN") // <-- IMPORTANTE: Cambia a "ARS", "MXN", etc. segÃºn tu paÃ­s
                    .build();
            items.add(item);

            PreferenceRequest.PreferenceRequestBuilder requestBuilder = PreferenceRequest.builder()
                    .items(items)
                    .backUrls(
                            PreferenceBackUrlsRequest.builder()
                                    .success(frontendUrl + "/cliente/compras")
                                    .failure(frontendUrl + "/cliente/compras")
                                    .pending(frontendUrl + "/cliente/compras")
                                    .build()
                    );

            // 3. Vinculamos la preferencia al pedido, para que el webhook sepa quÃ© Order marcar como pagado
            if (order.getOrderId() != null) {
                requestBuilder.externalReference(String.valueOf(order.getOrderId()));
            }
            // 4. URL pÃºblica donde Mercado Pago avisarÃ¡ cuando el pago se complete
            if (backendUrl != null && !backendUrl.isBlank()) {
                requestBuilder.notificationUrl(backendUrl + "/api/payments/webhook");
            }
            // Comenta esta lÃ­nea de abajo un momento si sigue fallando
            // .autoReturn("approved")

            // 5. Crear la preferencia
            Preference preference = client.create(requestBuilder.build(), options);

            return preference.getId();

        } catch (MPApiException e) {
            // ESTO NOS DARÃ EL ERROR REAL DE MERCADO PAGO
            log.error("Error de MercadoPago al crear preferencia. Status: {}", e.getApiResponse() != null ? e.getApiResponse().getStatusCode() : "N/A");
            log.debug("Respuesta de error recibida desde MercadoPago.");

            return "Error al procesar el pago";
        } catch (Exception e) {
            log.error("Error procesando operacion de MercadoPago", e);
            return "Error interno al procesar el pago";
        }
    }

    // Crea la preferencia de pago para que un negocio se suscriba a un Plan
    @PostMapping("/create-plan-preference")
    public String createPlanPreference(@AuthenticationPrincipal User user, @RequestBody PlanPaymentRequest request) {
        try {
            Business business = businessRepository.findByOwnerId(user.getId())
                    .orElseThrow(() -> new RuntimeException("No tienes un negocio registrado"));
            Plan plan = planRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Plan no encontrado"));

            if (!Boolean.TRUE.equals(plan.getActive())) {
                return "Error: el plan no esta disponible";
            }

            if (plan.getPrice() == null || plan.getPrice() <= 0) {
                return "Error: el plan no requiere pago";
            }

            com.mercadopago.core.MPRequestOptions options = com.mercadopago.core.MPRequestOptions.builder()
                    .accessToken(accessToken)
                    .build();

            PreferenceClient client = new PreferenceClient();

            // El precio se toma del Plan guardado en el backend, nunca de lo que mande el cliente
            List<PreferenceItemRequest> items = new ArrayList<>();
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title("SuscripciÃ³n Plan: " + plan.getName())
                    .quantity(1)
                    .unitPrice(BigDecimal.valueOf(plan.getPrice()))
                    .currencyId("PEN")
                    .build();
            items.add(item);

            PreferenceRequest.PreferenceRequestBuilder requestBuilder = PreferenceRequest.builder()
                    .items(items)
                    .backUrls(
                            PreferenceBackUrlsRequest.builder()
                                    .success(frontendUrl + "/empresa/plan")
                                    .failure(frontendUrl + "/empresa/plan")
                                    .pending(frontendUrl + "/empresa/plan")
                                    .build()
                    )
                    .externalReference(PLAN_REFERENCE_PREFIX + business.getId() + ":" + plan.getId());

            if (backendUrl != null && !backendUrl.isBlank()) {
                requestBuilder.notificationUrl(backendUrl + "/api/payments/webhook");
            }

            Preference preference = client.create(requestBuilder.build(), options);
            return preference.getId();

        } catch (MPApiException e) {
            log.error("Error de MercadoPago al crear preferencia. Status: {}", e.getApiResponse() != null ? e.getApiResponse().getStatusCode() : "N/A");
            log.debug("Respuesta de error recibida desde MercadoPago.");
            return "Error al procesar el pago";
        } catch (Exception e) {
            log.error("Error procesando operacion de MercadoPago", e);
            return "Error interno al procesar el pago";
        }
    }

    // Mercado Pago llama esta URL automÃ¡ticamente cuando un pago cambia de estado
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String id,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestHeader(name = "x-signature", required = false) String xSignature,
            @RequestHeader(name = "x-request-id", required = false) String xRequestId) {

        String eventType = topic != null ? topic : type;
        String paymentId = id != null ? id : dataId;

        if (!"payment".equals(eventType) || paymentId == null) {
            return ResponseEntity.ok().build();
        }

        if (!paymentId.matches("\\d+")) {
            log.warn("Webhook de MercadoPago rechazado por paymentId invalido.");
            return ResponseEntity.ok().build();
        }

        if (!isValidWebhookSignature(xSignature, xRequestId, dataId)) {
            log.warn("Webhook de MercadoPago rechazado por firma invalida.");
            return ResponseEntity.status(401).build();
        }

        try {
            com.mercadopago.core.MPRequestOptions options = com.mercadopago.core.MPRequestOptions.builder()
                    .accessToken(accessToken)
                    .build();

            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(Long.parseLong(paymentId), options);

            if (payment.getCollectorId() == null || !collectorId.equals(payment.getCollectorId())) {
                log.warn("Pago de MercadoPago rechazado por collectorId no valido.");
                return ResponseEntity.ok().build();
            }

            String externalReference = payment.getExternalReference();
            String status = payment.getStatus(); // approved, pending, rejected, etc.


            if (externalReference != null && "approved".equals(status)) {
                if (externalReference.startsWith(PLAN_REFERENCE_PREFIX)) {
                    // Formato: PLAN:{businessId}:{planId}
                    String[] parts = externalReference.substring(PLAN_REFERENCE_PREFIX.length()).split(":");
                    if (parts.length == 2 && parts[0].matches("\\d+") && parts[1].matches("\\d+")) {
                        Long businessId = Long.parseLong(parts[0]);
                        Long planId = Long.parseLong(parts[1]);
                        businessRepository.findById(businessId).ifPresent(business ->
                                planRepository.findById(planId).ifPresent(plan -> {
                                    if (!Boolean.TRUE.equals(plan.getActive())) {
                                        return;
                                    }
                                    if (plan.getPrice() == null || plan.getPrice() <= 0) {
                                        return;
                                    }
                                    BigDecimal expectedAmount = BigDecimal.valueOf(plan.getPrice());
                                    if (!"PEN".equals(payment.getCurrencyId()) || payment.getTransactionAmount() == null || payment.getTransactionAmount().compareTo(expectedAmount) != 0) {
                                        return;
                                    }
                                    business.setPlan(plan);
                                    businessRepository.save(business);
                                })
                        );
                    }
                } else {
                    if (!externalReference.matches("\\d+")) {
                        log.warn("Webhook de MercadoPago rechazado por externalReference invalido.");
                        return ResponseEntity.ok().build();
                    }
                    Long orderId = Long.parseLong(externalReference);
                    orderRepository.findById(orderId).ifPresent(o -> {
                        Double orderAmount = o.getPaidAmount() == null ? o.getTotalAmount() : o.getPaidAmount();
                        if (orderAmount == null || orderAmount <= 0) {
                            return;
                        }
                        BigDecimal expectedAmount = BigDecimal.valueOf(orderAmount);
                        if (!"PEN".equals(payment.getCurrencyId()) || payment.getTransactionAmount() == null || payment.getTransactionAmount().compareTo(expectedAmount) != 0) {
                            return;
                        }
                        if (o.getStatus() == OrderStatus.PENDING) {
                            o.setStatus(OrderStatus.PAID);
                            grantRewardPoints(o);
                            orderRepository.save(o);
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("Error procesando operacion de MercadoPago", e);
        }

        return ResponseEntity.ok().build();
    }

    private boolean isValidWebhookSignature(String xSignature, String xRequestId, String dataId) {
        if (xSignature == null || xRequestId == null || dataId == null || webhookSecret == null || webhookSecret.isBlank()) {
            return false;
        }
        try {
            String ts = null;
            String v1 = null;
            for (String part : xSignature.split(",")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2) {
                    if ("ts".equals(kv[0])) ts = kv[1];
                    if ("v1".equals(kv[0])) v1 = kv[1];
                }
            }
            if (ts == null || v1 == null) return false;
            String manifest = "id:" + dataId + ";request-id:" + xRequestId + ";ts:" + ts + ";";
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) hex.append(String.format("%02x", b));
            return MessageDigest.isEqual(hex.toString().getBytes(StandardCharsets.UTF_8), v1.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error validando firma del webhook de MercadoPago", e);
            return false;
        }
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































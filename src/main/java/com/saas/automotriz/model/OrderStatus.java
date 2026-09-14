package com.saas.automotriz.model;

public enum OrderStatus {
    PENDING,            // El pedido se ha creado pero no se ha confirmado el pago
    PAID,               // Pago satisfactorio
    PREPARING,          // El negocio está preparando el paquete
    SHIPPED,            // El pedido está en camino (Delivery)
    READY_FOR_PICKUP,   // El pedido está listo para ser recogido en el local
    DELIVERED,          // Entregado satisfactoriamente
    CANCELLED           // Pedido cancelado por el cliente o el negocio
}

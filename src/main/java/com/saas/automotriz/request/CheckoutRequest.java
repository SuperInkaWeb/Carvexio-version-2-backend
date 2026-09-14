package com.saas.automotriz.request;

import lombok.Data;
import java.util.List;
import com.saas.automotriz.model.DeliveryMethod;

@Data
public class CheckoutRequest {
    private String address;     // Dirección de envío o recojo
    private String phone;       // Teléfono de contacto
    private String notes;       // Notas extra
    private DeliveryMethod deliveryMethod;
    private List<Long> itemIds; // IDs específicos de los productos a comprar (opcional)
}

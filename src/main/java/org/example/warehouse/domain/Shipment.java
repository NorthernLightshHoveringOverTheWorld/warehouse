package org.example.warehouse.domain;

import java.util.Objects;
import java.util.UUID;


public record Shipment(UUID productId, UUID customerId, int quantity) {
    public Shipment {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(customerId, "customerId");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть положительным.");
        }
    }
}

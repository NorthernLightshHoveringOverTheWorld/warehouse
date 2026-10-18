package org.example.warehouse.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Неизменяемое состояние одного склада: остатки и журнал отпуска. */
public record Warehouse(String name, Map<UUID, Integer> stock, List<Shipment> shipments) {
    public Warehouse {
        name = Validation.requiredText(name, "Название склада");
        stock = Map.copyOf(stock);
        shipments = List.copyOf(shipments);
        if (stock.values().stream().anyMatch(quantity -> quantity < 0)) {
            throw new IllegalArgumentException("Остаток не может быть отрицательным.");
        }
    }

    public Warehouse(String name) {
        this(name, Map.of(), List.of());
    }

    public int quantityOf(UUID productId) {
        return stock.getOrDefault(Objects.requireNonNull(productId, "productId"), 0);
    }

    public Warehouse addUnit(UUID productId) {
        int current = quantityOf(productId);
        if (current == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Достигнут предел количества товара.");
        }
        Map<UUID, Integer> updated = new HashMap<>(stock);
        updated.put(productId, current + 1);
        return new Warehouse(name, updated, shipments);
    }

    public Warehouse ship(UUID productId, UUID customerId, int quantity) {
        Shipment shipment = new Shipment(productId, customerId, quantity);
        int available = quantityOf(productId);
        if (quantity > available) {
            throw new IllegalArgumentException("Недостаточно товара. Доступно: " + available);
        }
        Map<UUID, Integer> updated = new HashMap<>(stock);
        updated.put(productId, available - quantity);
        List<Shipment> history = new ArrayList<>(shipments);
        history.add(shipment);
        return new Warehouse(name, updated, history);
    }
}

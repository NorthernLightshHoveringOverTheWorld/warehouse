package org.example.warehouse.application;

import org.example.warehouse.application.port.CustomerRepository;
import org.example.warehouse.application.port.ProductRepository;
import org.example.warehouse.application.port.WarehouseRepository;
import org.example.warehouse.domain.Shipment;
import org.example.warehouse.domain.Warehouse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;


public final class InventoryService {
    private final WarehouseRepository warehouse;
    private final ProductRepository products;
    private final CustomerRepository customers;

    public InventoryService(WarehouseRepository warehouse, ProductRepository products,
                            CustomerRepository customers) {
        this.warehouse = Objects.requireNonNull(warehouse);
        this.products = Objects.requireNonNull(products);
        this.customers = Objects.requireNonNull(customers);
    }

    public void receive(UUID productId, int quantity) {
        requireProduct(productId);
        warehouse.save(warehouse.load().receive(productId, quantity));
    }

    public void ship(UUID productId, UUID customerId, int quantity) {
        requireProduct(productId);
        requireCustomer(customerId);
        warehouse.save(warehouse.load().ship(productId, customerId, quantity));
    }

    public int quantityOf(UUID productId) {
        requireProduct(productId);
        return warehouse.load().quantityOf(productId);
    }

    public List<StockPosition> allPositions() {
        Warehouse snapshot = warehouse.load();
        return products.findAll().stream()
                .map(product -> new StockPosition(product, snapshot.quantityOf(product.id())))
                .toList();
    }

    public List<StockPosition> availablePositions() {
        return allPositions().stream().filter(position -> position.quantity() > 0).toList();
    }

    public Map<String, List<StockPosition>> groupByType() {
        Map<String, List<StockPosition>> groups = allPositions().stream().collect(
                Collectors.groupingBy(position -> position.product().type(), TreeMap::new,
                        Collectors.toUnmodifiableList()));
        return Collections.unmodifiableMap(groups);
    }

    public List<Shipment> shipmentsFor(UUID customerId) {
        requireCustomer(customerId);
        return warehouse.load().shipments().stream()
                .filter(shipment -> shipment.customerId().equals(customerId)).toList();
    }

    private void requireProduct(UUID productId) {
        products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден."));
    }

    private void requireCustomer(UUID customerId) {
        customers.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Покупатель не найден."));
    }
}

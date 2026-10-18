package org.example.warehouse.infrastructure.memory;

import org.example.warehouse.application.port.ProductRepository;
import org.example.warehouse.domain.Product;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryProductRepository implements ProductRepository {
    private final Map<UUID, Product> products = new LinkedHashMap<>();

    @Override
    public void add(Product product) {
        Objects.requireNonNull(product);
        if (products.putIfAbsent(product.id(), product) != null) {
            throw new IllegalArgumentException("Товар с таким id уже существует.");
        }
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return Optional.ofNullable(products.get(Objects.requireNonNull(id)));
    }

    @Override
    public List<Product> findAll() {
        return List.copyOf(products.values());
    }
}

package org.example.warehouse.infrastructure.memory;

import org.example.warehouse.application.port.SupplierRepository;
import org.example.warehouse.domain.Supplier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemorySupplierRepository implements SupplierRepository {
    private final Map<UUID, Supplier> suppliers = new LinkedHashMap<>();

    @Override
    public void add(Supplier supplier) {
        Objects.requireNonNull(supplier);
        if (suppliers.putIfAbsent(supplier.id(), supplier) != null) {
            throw new IllegalArgumentException("Поставщик с таким id уже существует.");
        }
    }

    @Override
    public Optional<Supplier> findById(UUID id) {
        return Optional.ofNullable(suppliers.get(Objects.requireNonNull(id)));
    }

    @Override
    public List<Supplier> findAll() {
        return List.copyOf(suppliers.values());
    }
}

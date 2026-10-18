package org.example.warehouse.infrastructure.memory;

import org.example.warehouse.application.port.WarehouseRepository;
import org.example.warehouse.domain.Warehouse;

import java.util.Objects;

public final class InMemoryWarehouseRepository implements WarehouseRepository {
    private Warehouse warehouse;

    public InMemoryWarehouseRepository(Warehouse warehouse) {
        this.warehouse = Objects.requireNonNull(warehouse);
    }

    @Override
    public Warehouse load() {
        return warehouse;
    }

    @Override
    public void save(Warehouse warehouse) {
        this.warehouse = Objects.requireNonNull(warehouse);
    }
}

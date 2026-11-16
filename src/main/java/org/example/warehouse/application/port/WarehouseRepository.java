package org.example.warehouse.application.port;

import org.example.warehouse.domain.Warehouse;
import java.util.function.UnaryOperator;


public interface WarehouseRepository {
    Warehouse load();
    void save(Warehouse warehouse);

    default void update(UnaryOperator<Warehouse> operation) {
        save(operation.apply(load()));
    }
}
